package brooklyn.entity.java;

import static com.google.common.base.Preconditions.checkNotNull;
import groovy.json.StringEscapeUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.scriptbuilder.statements.java.InstallJDK;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.event.basic.BasicConfigKey;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.location.basic.jclouds.JcloudsLocation.JcloudsSshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.MutableSet;
import brooklyn.util.flags.TypeCoercions;
import brooklyn.util.text.StringEscapes.BashStringEscapes;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.internal.Primitives;

/**
 * The SSH implementation of the {@link brooklyn.entity.java.JavaSoftwareProcessDriver}.
 */
public abstract class JavaSoftwareProcessSshDriver extends AbstractSoftwareProcessSshDriver implements JavaSoftwareProcessDriver {

    public static final Logger log = LoggerFactory.getLogger(JavaSoftwareProcessSshDriver.class);

    public static final List<List<String>> MUTUALLY_EXCLUSIVE_OPTS = ImmutableList.<List<String>> of(ImmutableList.of("-client",
            "-server"));

    public static final List<String> KEY_VAL_OPT_PREFIXES = ImmutableList.of("-Xmx", "-Xms", "-Xss");

    public JavaSoftwareProcessSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);

        entity.setAttribute(Attributes.LOG_FILE_LOCATION, getLogFileLocation());
    }

    protected abstract String getLogFileLocation();

    public boolean isJmxEnabled() {
        return entity instanceof UsesJmx;
    }

    /**
     * Sets all JVM options (-X.. -D..) in an environment var JAVA_OPTS.
     * <p>
     * That variable is constructed from getJavaOpts(), then wrapped _unescaped_ in double quotes. An
     * error is thrown if there is an unescaped double quote in the string. All other unescaped
     * characters are permitted, but unless $var expansion or `command` execution is desired (although
     * this is not confirmed as supported) the generally caller should escape any such characters, for
     * example using {@link BashStringEscapes#escapeLiteralForDoubleQuotedBash(String)}.
     */
    @Override
    public Map<String, String> getShellEnvironment() {
        for (String it : getJavaOpts()) {
            BashStringEscapes.assertValidForDoubleQuotingInBash(it);
        }
        // do not double quote here; the env var is double quoted subsequently;
        // spaces should be preceded by double-quote
        // (if dbl quotes are needed we could pass on the command-line instead of in an env var)
        String sJavaOpts = Joiner.on(" ").join(getJavaOpts());
        // println "using java opts: $sJavaOpts"
        return MutableMap.<String, String> builder().putAll(super.getShellEnvironment()).put("JAVA_OPTS", sJavaOpts).build();
    }

    /**
     * arguments to pass to the JVM; this is the config options (e.g. -Xmx1024; only the contents of
     * {@link #getCustomJavaConfigOptions()} by default) and java system properties (-Dk=v; add custom
     * properties in {@link #getCustomJavaSystemProperties()})
     * <p>
     * See {@link #getShellEnvironment()} for discussion of quoting/escaping strategy.
     **/
    public List<String> getJavaOpts() {
        Iterable<String> sysprops = Iterables.transform(getJavaSystemProperties().entrySet(),
                new Function<Map.Entry<String, ?>, String>() {
                    public String apply(Map.Entry<String, ?> entry) {
                        String k = entry.getKey();
                        Object v = entry.getValue();
                        try {
                            if (v != null && Primitives.isWrapperType(v.getClass())) {
                                v = "" + v;
                            } else {
                                v = BasicConfigKey.resolveValue(v, Object.class, entity.getExecutionContext());
                                if (v == null) {
                                } else if (v instanceof CharSequence) {
                                } else if (TypeCoercions.isPrimitiveOrBoxer(v.getClass())) {
                                    v = "" + v;
                                } else {
                                    // could do toString, but that's likely not what is desired;
                                    // probably a type mismatch,
                                    // post-processing should be specified (common types are accepted
                                    // above)
                                    throw new IllegalArgumentException("cannot convert value " + v + " of type " + v.getClass()
                                            + " to string to pass as JVM property; use a post-processor");
                                }
                            }
                            return "-D" + k + (v != null ? "=" + v : "");
                        } catch (Exception e) {
                            log.warn("Error resolving java option key {}, propagating: {}", k, e);
                            throw Throwables.propagate(e);
                        }
                    }
                });

        Set<String> result = MutableSet.<String> builder().addAll(getCustomJavaConfigOptions()).addAll(sysprops).build();

        for (String customOpt : entity.getConfig(UsesJava.JAVA_OPTS)) {
            for (List<String> mutuallyExclusiveOpt : MUTUALLY_EXCLUSIVE_OPTS) {
                if (mutuallyExclusiveOpt.contains(customOpt)) {
                    result.removeAll(mutuallyExclusiveOpt);
                }
            }
            for (String keyValOptPrefix : KEY_VAL_OPT_PREFIXES) {
                if (customOpt.startsWith(keyValOptPrefix)) {
                    for (Iterator<String> iter = result.iterator(); iter.hasNext();) {
                        String existingOpt = (String) iter.next();
                        if (existingOpt.startsWith(keyValOptPrefix)) {
                            iter.remove();
                        }
                    }
                }
            }
            if (customOpt.indexOf("=") != -1) {
                String customOptPrefix = customOpt.substring(0, customOpt.indexOf("="));

                for (Iterator<String> iter = result.iterator(); iter.hasNext();) {
                    String existingOpt = (String) iter.next();
                    if (existingOpt.startsWith(customOptPrefix)) {
                        iter.remove();
                    }
                }
            }
            result.add(customOpt);
        }

        return ImmutableList.copyOf(result);
    }

    /**
     * Returns the complete set of Java system properties (-D defines) to set for the application.
     * <p>
     * This is exposed to the JVM as the contents of the {@code JAVA_OPTS} environment variable. Default
     * set contains config key, custom system properties, and JMX defines.
     * <p>
     * Null value means to set -Dkey otherwise it is -Dkey=value.
     * <p>
     * See {@link #getShellEnvironment()} for discussion of quoting/escaping strategy.
     */
    protected Map<String,?> getJavaSystemProperties() {
        return MutableMap.<String,Object>builder()
                .putAll(getCustomJavaSystemProperties())
                .putAll(isJmxEnabled() ? getJmxJavaSystemProperties() : Collections.<String,Object>emptyMap())
                .putAll(entity.getConfig(UsesJava.JAVA_SYSPROPS))
                .build();
    }

    /**
     * Return extra Java system properties (-D defines) used by the application.
     * 
     * Override as needed; default is an empty map.
     */
    protected Map getCustomJavaSystemProperties() {
        return Maps.newLinkedHashMap();
    }

    /**
     * Return extra Java config options, ie arguments starting with - which are passed to the JVM prior
     * to the class name.
     * <p>
     * Note defines are handled separately, in {@link #getCustomJavaSystemProperties()}.
     * <p>
     * Override as needed; default is an empty list.
     */
    protected List<String> getCustomJavaConfigOptions() {
        return Lists.newArrayList();
    }

    @Override
    public Integer getJmxPort() {
        return !isJmxEnabled() ? -1 : entity.getAttribute(UsesJmx.JMX_PORT);
    }

    @Deprecated
    // since 0.4, use getRmiServerPort
    @Override
    public Integer getRmiPort() {
        return getRmiServerPort();
    }

    public Integer getRmiServerPort() {
        return !isJmxEnabled() ? -1 : entity.getAttribute(UsesJmx.RMI_SERVER_PORT);
    }

    @Override
    public String getJmxContext() {
        return !isJmxEnabled() ? null : entity.getAttribute(UsesJmx.JMX_CONTEXT);
    }

    /**
     * Return the configuration properties required to enable JMX for a Java application.
     * 
     * These should be set as properties in the {@code JAVA_OPTS} environment variable when calling the
     * run script for the application.
     * 
     * TODO security!
     */
    protected Map<String, ?> getJmxJavaSystemProperties() {
        Integer jmxRemotePort = checkNotNull(getJmxPort(), "jmxPort for entity " + entity);
        String hostName = checkNotNull(getMachine().getAddress().getHostName(), "hostname for entity " + entity);
        return MutableMap.<String, Object> builder().put("com.sun.management.jmxremote", null).put(
                "com.sun.management.jmxremote.port", jmxRemotePort).put("com.sun.management.jmxremote.ssl", false).put(
                "com.sun.management.jmxremote.authenticate", false).put("java.rmi.server.hostname", hostName).build();
    }

    public void installJava() {
        // this should work, but not in 1.4.0 because oracle have blocked download (fixed in head 1.4.1
        // and 1.5.0)
        try {
            getLocation().acquireMutex("install:" + getLocation().getName(), "installing Java at " + getLocation());
            log.debug("checking for java at " + entity + " @ " + getLocation());
            int result = getLocation().execCommands("check java", Arrays.asList("which java"));
            if (result == 0) {
                log.debug("java detected at " + entity + " @ " + getLocation());
            } else {
                log.debug("java not detected at " + entity + " @ " + getLocation() + ", installing");
                if (getLocation() instanceof JcloudsSshMachineLocation) {
                    ExecResponse result2 = ((JcloudsSshMachineLocation) getLocation()).submitRunScript(InstallJDK.fromURL()).get();
                    if (result2.getExitStatus() != 0)
                        log.warn("invalid result code " + result2.getExitStatus() + " installing java at " + entity + " @ "
                                + getLocation() + ":\n" + result2.getOutput() + "\n" + result2.getError());
                } else {
                    log.warn("No knowledge of how to install Java at " + getLocation() + " for " + entity
                            + ", and Java not detected. " + "Processes may fail to start.");
                }
            }
        } catch (Exception e) {
            Throwables.propagate(e);
        } finally {
            getLocation().releaseMutex("install:" + getLocation().getName());
        }

        // //this works on ubuntu (surprising that jdk not in default repos!)
        // "sudo add-apt-repository ppa:dlecan/openjdk",
        // "sudo apt-get update",
        // "sudo apt-get install -y --allow-unauthenticated openjdk-7-jdk"
    }

    @Override
    public void start() {
        installJava();
        super.start();
    }

}
