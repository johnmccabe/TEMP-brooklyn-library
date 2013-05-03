package brooklyn.entity.java;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.event.feed.ConfigToAttributes;
import brooklyn.event.feed.jmx.JmxFeed;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.flags.SetFromFlag;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class VanillaJavaApp extends SoftwareProcessImpl implements UsesJava, UsesJmx, UsesJavaMXBeans {

    // FIXME classpath values: need these to be downloaded and installed?
    
    // TODO Make jmxPollPeriod @SetFromFlag easier to use: currently a confusion over long and TimeDuration, and 
    // no ability to set default value (can't just set field because config vals read/set in super-constructor :-(
     
    private static final Logger log = LoggerFactory.getLogger(VanillaJavaApp.class);
    
    @SetFromFlag("args")
    public static final ConfigKey<List> ARGS = new BasicConfigKey<List>(List.class, "vanillaJavaApp.args", "Arguments for launching the java app", Lists.newArrayList());
    
    @SetFromFlag(value="main", nullable=false)
    public static final ConfigKey<String> MAIN_CLASS = new BasicConfigKey<String>(String.class, "vanillaJavaApp.mainClass", "class to launch");

    @SetFromFlag("classpath")
    public static final ConfigKey<List> CLASSPATH = new BasicConfigKey<List>(List.class, "vanillaJavaApp.classpath", "classpath to use, as list of URL entries", Lists.newArrayList());

    @SetFromFlag
    protected long jmxPollPeriod;
    
    @SetFromFlag("jvmXArgs")
    public static final ConfigKey<List> JVM_XARGS = new BasicConfigKey<List>(List.class, "vanillaJavaApp.jvmXArgs", "JVM -X args for the java app (e.g. memory)", 
        MutableList.of("-Xms128m", "-Xmx512m", "-XX:MaxPermSize=512m"));

    @SetFromFlag("jvmDefines")
    public static final ConfigKey<Map> JVM_DEFINES = new BasicConfigKey<Map>(Map.class, "vanillaJavaApp.jvmDefines", "JVM system property definitions for the app",
        Maps.newLinkedHashMap());

    protected JmxFeed jmxFeed;

    public VanillaJavaApp() {
        super(MutableMap.of(), null);
    }
    public VanillaJavaApp(Entity parent) {
        super(MutableMap.of(), parent);
    }
    public VanillaJavaApp(Map flags) {
        super(flags, null);
    }
    public VanillaJavaApp(Map props, Entity parent) {
        super(props, parent);
    }
    
    public String getMainClass() { return getConfig(MAIN_CLASS); }
    public List<String> getClasspath() { return getConfig(CLASSPATH); }
    public Map getJvmDefines() { return getConfig(JVM_DEFINES); }
    public List getJvmXArgs() { return getConfig(JVM_XARGS); }

    public void addToClasspath(String url) {
        List<String> cp = getConfig(CLASSPATH);
        List<String> newCP = new ArrayList<String>();
        if (cp!=null) newCP.addAll(cp);
        newCP.add(url);
        setConfig(CLASSPATH, newCP);
    }
    
    public void addToClasspath(Collection<String> urls) {
        List<String> cp = getConfig(CLASSPATH);
        List<String> newCP = new ArrayList<String>();
        if (cp!=null) newCP.addAll(cp);
        newCP.addAll(urls);
        setConfig(CLASSPATH, newCP);
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        
        ConfigToAttributes.apply(this);
        
        if ( ((VanillaJavaAppDriver)getDriver()).isJmxEnabled() ) {
            jmxPollPeriod = (jmxPollPeriod > 0) ? jmxPollPeriod : 500;
            jmxFeed = JavaAppUtils.connectMXBeanSensors(this, jmxPollPeriod);
        }

        connectServiceUpIsRunning();
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();
        if (jmxFeed != null) jmxFeed.stop();
    }
    
    @Override
    protected void preStop() {
        // FIXME Confirm don't need to call jmxAdapter.deactivateAdapter();
        super.preStop();
    }

    @Override
    public Class<? extends VanillaJavaAppDriver> getDriverInterface() {
        return VanillaJavaAppDriver.class;
    }
    
    public String getRunDir() {
        // FIXME Make this an attribute; don't assume it hsa to be ssh? What uses this?
        VanillaJavaAppSshDriver driver = (VanillaJavaAppSshDriver) getDriver();
        return (driver != null) ? driver.getRunDir() : null;
    }
}
