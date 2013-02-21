package brooklyn.entity.webapp.tomcat;

import brooklyn.catalog.Catalog;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJmx;
import brooklyn.entity.proxying.BasicEntitySpec;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.webapp.JavaWebAppSoftwareProcess;
import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.location.basic.PortRanges;
import brooklyn.util.flags.SetFromFlag;

/**
 * An {@link brooklyn.entity.Entity} that represents a single Tomcat instance.
 */
@Catalog(name="Tomcat Server", description="Apache Tomcat is an open source software implementation of the Java Servlet and JavaServer Pages technologies", iconUrl="classpath:///tomcat-logo.png")
@ImplementedBy(TomcatServerImpl.class)
public interface TomcatServer extends JavaWebAppSoftwareProcess, UsesJmx {
    
    public static class Spec<T extends TomcatServer, S extends Spec<T,S>> extends BasicEntitySpec<T,S> {

        private static class ConcreteSpec extends Spec<TomcatServer, ConcreteSpec> {
            ConcreteSpec() {
                super(TomcatServer.class);
            }
        }
        
        public static Spec<TomcatServer, ?> newInstance() {
            return new ConcreteSpec();
        }
        
        protected Spec(Class<T> type) {
            super(type);
        }
    }

    @SetFromFlag("version")
    public static final BasicConfigKey<String> SUGGESTED_VERSION =
            new BasicConfigKey<String>(SoftwareProcess.SUGGESTED_VERSION, "7.0.37");

    @SetFromFlag("downloadUrl")
    public static final BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "http://download.nextag.com/apache/tomcat/tomcat-7/v${version}/bin/apache-tomcat-${version}.tar.gz");

    /**
     * Tomcat insists on having a port you can connect to for the sole purpose of shutting it down.
     * Don't see an easy way to disable it; causes collisions in its out-of-the-box location of 8005,
     * so override default here to a high-numbered port.
     */
    @SetFromFlag("shutdownPort")
    public static final PortAttributeSensorAndConfigKey SHUTDOWN_PORT =
            new PortAttributeSensorAndConfigKey("tomcat.shutdownport", "Suggested shutdown port", PortRanges.fromString("31880+"));

    public static final BasicAttributeSensor<String> CONNECTOR_STATUS =
            new BasicAttributeSensor<String>(String.class, "webapp.tomcat.connectorStatus", "Catalina connector state name");

    public static final BasicAttributeSensor<String> JMX_SERVICE_URL = Attributes.JMX_SERVICE_URL;
    
}
