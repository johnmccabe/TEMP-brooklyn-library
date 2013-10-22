/*
 * Copyright 2012-2013 by Cloudsoft Corp.
 */
package brooklyn.entity.messaging.storm;

import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.entity.zookeeper.ZooKeeperEnsemble;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.util.flags.SetFromFlag;

/**
 * An {@link brooklyn.entity.Entity} that represents a Storm node (Nimbus or Worker).
 */
@ImplementedBy(StormImpl.class)
public interface Storm extends SoftwareProcess/*, UsesJmx*/ {

    @SetFromFlag("version")
    ConfigKey<String> SUGGESTED_VERSION = ConfigKeys.newConfigKeyWithDefault(SoftwareProcess.SUGGESTED_VERSION, "0.8.2");

    @SetFromFlag("nimbusHostname")
    ConfigKey<String> NIMBUS_HOSTNAME = ConfigKeys.newStringConfigKey("storm.nimbus.hostname");

    @SetFromFlag("downloadUrl")
    BasicAttributeSensorAndConfigKey<String> DOWNLOAD_URL = new BasicAttributeSensorAndConfigKey<String>(
            SoftwareProcess.DOWNLOAD_URL, "https://dl.dropboxusercontent.com/s/fl4kr7w0oc8ihdw/storm-${version}.zip");

    @SetFromFlag("role")
    BasicAttributeSensorAndConfigKey<Role> ROLE = new BasicAttributeSensorAndConfigKey<Role>(Role.class, "storm.role");

    @SetFromFlag("localDir")
    BasicAttributeSensorAndConfigKey<String> LOCAL_DIR = new BasicAttributeSensorAndConfigKey<String>(String.class,
            "storm.local.dir", "Setting for Storm local dir");
    
    @SetFromFlag("uiPort")
    PortAttributeSensorAndConfigKey UI_PORT = new PortAttributeSensorAndConfigKey("storm.ui.port", "Storm UI port", "8080+");

    @SetFromFlag("thriftPort")
    PortAttributeSensorAndConfigKey THRIFT_PORT = new PortAttributeSensorAndConfigKey("storm.thrift.port", "Storm Thrift port", "6627");

    @SetFromFlag("zookeeperEnsemble")
    ConfigKey<ZooKeeperEnsemble> ZOOKEEPER_ENSEMBLE = ConfigKeys.newConfigKey(ZooKeeperEnsemble.class,
            "storm.zookeeper.ensemble", "Zookeeper ensemble entity");

    @SetFromFlag("stormConfigTemplateUrl")
    ConfigKey<String> STORM_CONFIG_TEMPLATE_URL = ConfigKeys.newStringConfigKey("storm.config.templateUrl", "Template file (in freemarker format) for the storm.yaml config file",
           "classpath://brooklyn/entity/messaging/storm/storm.yaml");

    @SetFromFlag("zeromqVersion")
    ConfigKey<String> ZEROMQ_VERSION = ConfigKeys.newStringConfigKey("storm.zeromq.version", "zeromq version", "2.1.7");

    String getStormConfigTemplateUrl();
    String getHostname();
    Role getRole();
    
    public enum Role {
        NIMBUS, SUPERVISOR, UI
    }

}
