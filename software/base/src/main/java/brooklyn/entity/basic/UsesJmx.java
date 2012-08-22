package brooklyn.entity.basic;

import brooklyn.event.basic.BasicAttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;


public interface UsesJmx extends UsesJava {
	public static final int DEFAULT_JMX_PORT = 1099;   // RMI port?
	@SetFromFlag("jmxPort")
	public static final PortAttributeSensorAndConfigKey JMX_PORT = Attributes.JMX_PORT;
	@SetFromFlag("rmiServerPort")
	public static final PortAttributeSensorAndConfigKey RMI_SERVER_PORT = Attributes.RMI_SERVER_PORT;
	@Deprecated // since 0.4 use RMI_REGISTRY_PORT
    public static final PortAttributeSensorAndConfigKey RMI_PORT = RMI_SERVER_PORT;
	public static final PortAttributeSensorAndConfigKey RMI_REGISTRY_PORT = RMI_SERVER_PORT;
    
	@SetFromFlag("jmxContext")
	public static final BasicAttributeSensorAndConfigKey<String> JMX_CONTEXT = Attributes.JMX_CONTEXT;

	public static final BasicAttributeSensor<String> JMX_URL = new BasicAttributeSensor<String>(String.class, "jmx.url", "JMX URL");
}