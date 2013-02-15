/*
 * Copyright 2012-2013 by Cloudsoft Corp.
 */
package brooklyn.entity.nosql.couchdb;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;
import brooklyn.util.NetworkUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

/**
 * Start a {@link CouchDBNode} in a {@link Location} accessible over ssh.
 */
public class CouchDBNodeSshDriver extends JavaSoftwareProcessSshDriver implements CouchDBNodeDriver {

    private static final Logger log = LoggerFactory.getLogger(CouchDBNodeSshDriver.class);

    public CouchDBNodeSshDriver(CouchDBNodeImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    protected String getLogFileLocation() { return String.format("%s/couchdb.log", getRunDir()); }

    @Override
    public Integer getHttpPort() { return entity.getAttribute(CouchDBNode.HTTP_PORT); }

    @Override
    public Integer getHttpsPort() { return entity.getAttribute(CouchDBNode.HTTPS_PORT); }

    @Override
    public String getClusterName() { return entity.getAttribute(CouchDBNode.CLUSTER_NAME); }

    @Override
    public String getCouchDBConfigTemplateUrl() { return entity.getAttribute(CouchDBNode.COUCHDB_CONFIG_TEMPLATE_URL); }

    @Override
    public String getCouchDBUriTemplateUrl() { return entity.getAttribute(CouchDBNode.COUCHDB_URI_TEMPLATE_URL); }

    @Override
    public String getCouchDBConfigFileName() { return entity.getAttribute(CouchDBNode.COUCHDB_CONFIG_FILE_NAME); }

    @Override
    public void install() {
        log.info("Installing {}", entity);
        List<String> commands = ImmutableList.<String>builder()
                .add(CommonCommands.installPackage("erlang"))
                .add(CommonCommands.installPackage("couchdb"))
                .build();

        newScript(INSTALLING)
                .failOnNonZeroResultCode()
                .body.append(commands)
                .execute();
    }

    public Set<Integer> getPortsUsed() {
        Set<Integer> result = Sets.newLinkedHashSet(super.getPortsUsed());
        result.addAll(getPortMap().values());
        return result;
    }

    private Map<String, Integer> getPortMap() {
        return ImmutableMap.<String, Integer>builder()
                .put("httpPort", getHttpPort())
                .build();
    }

    @Override
    public void customize() {
        log.info("Customizing {} (Cluster {})", entity, getClusterName());
        NetworkUtils.checkPortsValid(getPortMap());

        newScript(CUSTOMIZING).execute();

        // Copy the configuration files across
        String configFileContents = processTemplate(getCouchDBConfigTemplateUrl());
        String destinationConfigFile = String.format("%s/%s", getRunDir(), getCouchDBConfigFileName());
        getMachine().copyTo(new ByteArrayInputStream(configFileContents.getBytes()), destinationConfigFile);
        String uriFileContents = processTemplate(getCouchDBUriTemplateUrl());
        String destinationUriFile = String.format("%s/couch.uri", getRunDir());
        getMachine().copyTo(new ByteArrayInputStream(uriFileContents.getBytes()), destinationUriFile);
    }

    @Override
    public void launch() {
        log.info("Launching  {}", entity);
        newScript(MutableMap.of("usePidFile", false), LAUNCHING)
                .body.append(String.format("nohup couchdb -p %s -a %s/%s -o couchdb-console.log -e couchdb-error.log -b &", getPidFile(), getRunDir(), getCouchDBConfigFileName()))
                .execute();
    }

    public String getPidFile() { return String.format("%s/couchdb.pid", getRunDir()); }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", getPidFile()), CHECK_RUNNING)
                .body.append(String.format("couchdb -p %s -s", getPidFile()))
                .execute() == 0;
    }

    @Override
    public void stop() {
        newScript(MutableMap.of("usePidFile", getPidFile()), STOPPING)
                .body.append(String.format("couchdb -p %s -k", getPidFile()))
                .execute();
    }
}
