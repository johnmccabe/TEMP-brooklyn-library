package brooklyn.entity.proxy;

import brooklyn.entity.Group;
import brooklyn.entity.basic.Attributes;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.ImmutableList;

/**
 * A load balancer that routes requests to set(s) of servers.
 * 
 * There is an optional "serverPool" that will have requests routed to it (e.g. as round-robin). 
 * This is a group whose members are appropriate servers; membership of that group will be tracked 
 * to automatically update the load balancer's configuration as appropriate.
 * 
 * There is an optional urlMappings group for defining additional mapping rules. Members of this
 * group (of type UrlMapping) will be tracked, to automatically update the load balancer's configuration.
 * The UrlMappings can give custom routing rules so that specific urls are routed (and potentially re-written)
 * to particular sets of servers. 
 * 
 * @author aled
 */
public interface LoadBalancer {

    @SetFromFlag("serverPool")
    public static final BasicConfigKey<Group> SERVER_POOL = new BasicConfigKey<Group>(
            Group.class, "loadbalancer.serverpool", "The default servers to route messages to");

    @SetFromFlag("urlMappings")
    public static final BasicConfigKey<Group> URL_MAPPINGS = new BasicConfigKey<Group>(
            Group.class, "loadbalancer.urlmappings", "Special mapping rules (e.g. for domain/path matching, rewrite, etc)");
    
    /** sensor for port to forward to on target entities */
    @SetFromFlag("portNumberSensor")
    public static final BasicConfigKey<AttributeSensor> PORT_NUMBER_SENSOR = new BasicConfigKey<AttributeSensor>(
            AttributeSensor.class, "member.sensor.portNumber", "Port number sensor on members (defaults to http.port)", Attributes.HTTP_PORT);

    @SetFromFlag("port")
    /** port where this controller should live */
    public static final PortAttributeSensorAndConfigKey PROXY_HTTP_PORT = new PortAttributeSensorAndConfigKey(
            "proxy.http.port", "Main HTTP port where this proxy listens", ImmutableList.of(8000,"8001+"));
    
    @SetFromFlag("protocol")
    public static final BasicAttributeSensorAndConfigKey<String> PROTOCOL = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "proxy.protocol", "Main URL protocol this proxy answers (typically http or https)", null);
    
    //does this have special meaning to nginx/others? or should we just take the hostname ?
    public static final String ANONYMOUS = "anonymous";
    
    @SetFromFlag("domain")
    public static final BasicAttributeSensorAndConfigKey<String> DOMAIN_NAME = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "proxy.domainName", "Domain name that this controller responds to", ANONYMOUS);
        
    @SetFromFlag("url")
    public static final BasicAttributeSensorAndConfigKey<String> SPECIFIED_URL = new BasicAttributeSensorAndConfigKey<String>(
            String.class, "proxy.url", "Main URL this proxy listens at");
    
    @SetFromFlag("ssl")
    public static final BasicConfigKey<ProxySslConfig> SSL_CONFIG = 
        new BasicConfigKey<ProxySslConfig>(ProxySslConfig.class, "proxy.ssl.config", "configuration (e.g. certificates) for SSL; will use SSL if set, not use SSL if not set");

}