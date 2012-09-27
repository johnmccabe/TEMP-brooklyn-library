package brooklyn.entity.proxy;

import static brooklyn.util.JavaGroovyEquivalents.elvis;
import static brooklyn.util.JavaGroovyEquivalents.groovyTruth;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.Entity;
import brooklyn.entity.Group;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Description;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.basic.SoftwareProcessEntity;
import brooklyn.entity.group.AbstractMembershipTrackingPolicy;
import brooklyn.entity.group.Cluster;
import brooklyn.entity.trait.Startable;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.MutableMap;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/**
 * Represents a controller mechanism for a {@link Cluster}.
 */
public abstract class AbstractController extends SoftwareProcessEntity implements LoadBalancer {
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractController.class);

    /** sensor for port to forward to on target entities */
    @SetFromFlag("portNumberSensor")
    public static final BasicAttributeSensorAndConfigKey<AttributeSensor> PORT_NUMBER_SENSOR = new BasicAttributeSensorAndConfigKey<AttributeSensor>(
            AttributeSensor.class, "member.sensor.portNumber", "Port number sensor on members (defaults to http.port)", Attributes.HTTP_PORT);

    @SetFromFlag("port")
    /** port where this controller should live */
    public static final PortAttributeSensorAndConfigKey PROXY_HTTP_PORT = new PortAttributeSensorAndConfigKey(
            "proxy.http.port", "Main HTTP port where this proxy listens", ImmutableList.of(8000,"8001+"));
    
    @SetFromFlag("protocol")
    public static final BasicAttributeSensorAndConfigKey<String> PROTOCOL = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "proxy.protocol", "Main URL protocol this proxy answers (typically http or https)", null);
    
    @SetFromFlag("domain")
    public static final BasicAttributeSensorAndConfigKey<String> DOMAIN_NAME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "proxy.domainName", "Domain name that this controller responds to", null);
        
    @SetFromFlag("ssl")
    public static final BasicConfigKey<ProxySslConfig> SSL_CONFIG = 
        new BasicConfigKey<ProxySslConfig>(ProxySslConfig.class, "proxy.ssl.config", "configuration (e.g. certificates) for SSL; will use SSL if set, not use SSL if not set");

    public static final BasicAttributeSensor<String> ROOT_URL = WebAppService.ROOT_URL;
    
    public static final BasicAttributeSensor<Set<String>> SERVER_POOL_TARGETS = new BasicAttributeSensor(
            Set.class, "proxy.targets", "Main set of downstream targets");
    
    /**
     * @deprecated Use SERVER_POOL_TARGETS
     */
    public static final BasicAttributeSensor<Set<String>> TARGETS = SERVER_POOL_TARGETS;
    
    public static final MethodEffector<Void> RELOAD = new MethodEffector(AbstractController.class, "reload");
    
    protected boolean isActive;
    protected boolean updateNeeded = true;

    protected AbstractMembershipTrackingPolicy serverPoolMemberTrackerPolicy;
    protected Set<String> serverPoolAddresses = Sets.newLinkedHashSet();
    protected Set<Entity> serverPoolTargets = Sets.newLinkedHashSet();
    
    public AbstractController() {
        this(MutableMap.of(), null, null);
    }
    public AbstractController(Map properties) {
        this(properties, null, null);
    }
    public AbstractController(Entity owner) {
        this(MutableMap.of(), owner, null);
    }
    public AbstractController(Map properties, Entity owner) {
        this(properties, owner, null);
    }
    public AbstractController(Entity owner, Cluster cluster) {
        this(MutableMap.of(), owner, cluster);
    }
    public AbstractController(Map properties, Entity owner, Cluster cluster) {
        super(properties, owner);

        serverPoolMemberTrackerPolicy = new AbstractMembershipTrackingPolicy(MutableMap.of("name", "Controller targets tracker")) {
            protected void onEntityChange(Entity member) { onServerPoolMemberChanged(member); }
            protected void onEntityAdded(Entity member) { onServerPoolMemberChanged(member); }
            protected void onEntityRemoved(Entity member) { onServerPoolMemberChanged(member); }
        };
    }

    @Override
    public Entity configure(Map flags) {
        Entity result = super.configure(flags);
        
        // Support old "cluster" flag (deprecated)
        if (flags.containsKey("cluster")) {
            Group cluster = (Group) flags.get("cluster");
            LOG.warn("Deprecated use of AbstractController.cluster: entity {}; value {}", this, cluster);
            if (getConfig(SERVER_POOL) == null) {
                setConfig(SERVER_POOL, cluster);
            }
        }
        
        return result;
    }
    
    /**
     * Opportunity to do late-binding of the cluster that is being controlled. Must be called before start().
     * Can pass in the 'cluster'.
     */
    public void bind(Map flags) {
        if (flags.containsKey("serverPool")) {
            setConfig(SERVER_POOL, (Group) flags.get("serverPool"));
            
        } else if (flags.containsKey("cluster")) {
            LOG.warn("Deprecated use of AbstractController.cluster: entity {}; value {}", this, flags.get("cluster"));
            setConfig(SERVER_POOL, (Group) flags.get("cluster"));
        }
    }

    private Group getServerPool() {
        return getConfig(SERVER_POOL);
    }
    
    public boolean isActive() {
    	return isActive;
    }
    
    public String getProtocol() {
        return getAttribute(PROTOCOL);
    }

    public String getDomain() {
        return getAttribute(DOMAIN_NAME);
    }
    
    public Integer getPort() {
        return getAttribute(PROXY_HTTP_PORT);
    }

    public String getUrl() {
        return getAttribute(ROOT_URL);
    }

    public AttributeSensor getPortNumberSensor() {
        return getAttribute(PORT_NUMBER_SENSOR);
    }

    @Description("Forces reload of the configuration")
    public abstract void reload();

    protected String inferProtocol() {
        return getConfig(SSL_CONFIG)!=null ? "https" : "http";
    }
    
    protected String inferUrl() {
        String protocol = checkNotNull(getProtocol(), "protocol must not be null");
        String domain = checkNotNull(getDomain(), "domain must not be null");
        Integer port = checkNotNull(getPort(), "port must not be null");
        return protocol+"://"+domain+":"+port+"/";
    }
    
    @Override
    protected Collection<Integer> getRequiredOpenPorts() {
        Collection<Integer> result = super.getRequiredOpenPorts();
        if (groovyTruth(getAttribute(PROXY_HTTP_PORT))) result.add(getAttribute(PROXY_HTTP_PORT));
        return result;
    }

    @Override
    protected void preStart() {
        super.preStart();
        
        setAttribute(PROTOCOL, inferProtocol());
        setAttribute(DOMAIN_NAME, elvis(getConfig(DOMAIN_NAME), getAttribute(HOSTNAME)));
        setAttribute(ROOT_URL, inferUrl());
        
        checkNotNull(getPortNumberSensor(), "port number sensor must not be null");
    }
    
    @Override
    protected void postStart() {
        super.postStart();
        LOG.info("Adding policy {} to {} on AbstractController.start", serverPoolMemberTrackerPolicy, this);
        addPolicy(serverPoolMemberTrackerPolicy);
        reset();
        isActive = true;
        update();
    }
    
    protected void preStop() {
        super.preStop();
        serverPoolMemberTrackerPolicy.reset();
    }

    /** 
     * Implementations should update the configuration so that 'addresses' are targeted.
     * The caller will subsequently call reload if reconfigureService returned true.
     * 
     * @return True if the configuration has been modified (i.e. required reload); false otherwise.
     */
    protected abstract boolean reconfigureService();
    
    public void update() {
        if (!isActive()) updateNeeded = true;
        else {
            updateNeeded = false;
            LOG.debug("Updating {} in response to changes", this);
            boolean modified = reconfigureService();
            if (modified) {
                LOG.debug("Reloading {} in response to changes", this);
                invokeFromJava(RELOAD);
            } else {
                LOG.debug("Reconfiguration made no change, so skipping reload", this);
            }
        }
        setAttribute(SERVER_POOL_TARGETS, serverPoolAddresses);
    }

    protected synchronized void reset() {
        serverPoolMemberTrackerPolicy.reset();
        serverPoolAddresses.clear();
        serverPoolTargets.clear();
        if (groovyTruth(getServerPool())) {
            serverPoolMemberTrackerPolicy.setGroup(getServerPool());
            
            // Initialize ourselves immediately with the latest set of members; don't wait for
            // listener notifications because then will be out-of-date for short period (causing 
            // problems for rebind)
            for (Entity member : getServerPool().getMembers()) {
                if (belongsInServerPool(member)) {
                    if (LOG.isTraceEnabled()) LOG.trace("Done {} checkEntity {}", this, member);
                    serverPoolTargets.add(member);
                    String address = getAddressOfEntity(member);
                    if (address != null) {
                        serverPoolAddresses.add(address);
                    }
                }
            }
            
            LOG.info("Resetting {}, members {} with address {}", new Object[] {this, serverPoolTargets, serverPoolAddresses});
        }
        
        setAttribute(SERVER_POOL_TARGETS, serverPoolAddresses);
    }

    protected void onServerPoolMemberChanged(Entity member) {
        if (LOG.isTraceEnabled()) LOG.trace("Start {} checkEntity {}", this, member);
        if (belongsInServerPool(member)) {
            addServerPoolMember(member);
        } else {
            removeServerPoolMember(member);
        }
        if (LOG.isTraceEnabled()) LOG.trace("Done {} checkEntity {}", this, member);
    }
    
    protected boolean belongsInServerPool(Entity member) {
        if (!groovyTruth(member.getAttribute(Startable.SERVICE_UP))) {
            LOG.debug("Members of {}, checking {}, eliminating because not up", getDisplayName(), member.getDisplayName());
            return false;
        }
        if (!getServerPool().getMembers().contains(member)) {
            LOG.debug("Members of {}, checking {}, eliminating because not member", getDisplayName(), member.getDisplayName());
            return false;
        }
        LOG.debug("Members of {}, checking {}, approving", getDisplayName(), member.getDisplayName());
        return true;
    }
    
    protected synchronized void addServerPoolMember(Entity member) {
        if (LOG.isTraceEnabled()) LOG.trace("Considering to add to {}, new member {} in locations {} - "+
                "waiting for service to be up", new Object[] {this, member, member.getLocations()});
        if (serverPoolTargets.contains(member)) return;
        
        String address = getAddressOfEntity(member);
        if (address != null) {
            serverPoolAddresses.add(address);
        }

        LOG.info("Adding to {}, new member {} with address {}", new Object[] {this, member, address});
        
        update();
        serverPoolTargets.add(member);
    }
    
    protected synchronized void removeServerPoolMember(Entity member) {
        if (LOG.isTraceEnabled()) LOG.trace("Considering to remove from {}, member {} in locations {} - "+
                "waiting for service to be up", new Object[] {this, member, member.getLocations()});
        if (!serverPoolTargets.contains(member)) return;
        
        String address = getAddressOfEntity(member);
        if (address != null) {
            serverPoolAddresses.remove(address);
        }
        
        LOG.info("Removing from {}, member {} with address {}", new Object[] {this, member, address});
        
        update();
        serverPoolTargets.remove(member);
    }
    
    protected String getAddressOfEntity(Entity member) {
        String ip = member.getAttribute(Attributes.HOSTNAME);
        Integer port = member.getAttribute(Attributes.HTTP_PORT);
        if (ip!=null && port!=null) {
            return ip+":"+port;
        }
        LOG.error("Unable to construct hostname:port representation for "+member+"; skipping in "+this);
        return null;
    }
}
