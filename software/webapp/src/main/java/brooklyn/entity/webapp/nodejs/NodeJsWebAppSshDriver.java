package brooklyn.entity.webapp.nodejs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.webapp.WebAppService;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.file.ArchiveUtils;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.text.Strings;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class NodeJsWebAppSshDriver extends AbstractSoftwareProcessSshDriver implements NodeJsWebAppDriver {

    private static final Logger LOG = LoggerFactory.getLogger(NodeJsWebAppService.class);

    public NodeJsWebAppSshDriver(NodeJsWebAppServiceImpl entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    public NodeJsWebAppServiceImpl getEntity() {
        return (NodeJsWebAppServiceImpl) super.getEntity();
    }

    @Override
    public Integer getHttpPort() {
        return getEntity().getAttribute(Attributes.HTTP_PORT);
    }

    @Override
    public void postLaunch() {
        String rootUrl = String.format("http://%s:%d/", getHostname(), getHttpPort());
        entity.setAttribute(WebAppService.ROOT_URL, rootUrl);
    }

    protected Map<String, Integer> getPortMap() {
        return ImmutableMap.of("http", getEntity().getAttribute(WebAppService.HTTP_PORT));
    }

    @Override
    public Set<Integer> getPortsUsed() {
        return ImmutableSet.<Integer>builder()
                .addAll(super.getPortsUsed())
                .addAll(getPortMap().values())
                .build();
    }

    @Override
    public void install() {
        List<String> packages = getEntity().getConfig(NodeJsWebAppService.NODE_PACKAGE_LIST);
        LOG.info("Installing Node.JS {} {}", getEntity().getConfig(SoftwareProcess.SUGGESTED_VERSION), Iterables.toString(packages));

        List<String> commands = MutableList.<String>builder()
                .add(BashCommands.INSTALL_CURL)
                .add(BashCommands.ifExecutableElse0("apt-get", BashCommands.chain(
                        BashCommands.installPackage("python-software-properties python g++ make"),
                        BashCommands.sudo("add-apt-repository ppa:chris-lea/node.js"))))
                .add(BashCommands.installPackage(MutableMap.of("yum", "git nodejs npm", "apt", "git-core nodejs"), null))
                .add(BashCommands.sudo("npm install -g n"))
                .add(BashCommands.sudo("n " + getEntity().getConfig(SoftwareProcess.SUGGESTED_VERSION)))
                .build();

        if (packages != null && packages.size() > 0) {
            commands.add(BashCommands.sudo("npm install -g " + Joiner.on(' ').join(packages)));
        }

        newScript(INSTALLING)
                .body.append(commands)
                .execute();
    }

    @Override
    public void customize() {
        List<String> commands = Lists.newLinkedList();

        String gitRepoUrl = getEntity().getConfig(NodeJsWebAppService.APP_GIT_REPOSITORY_URL);
        String archiveUrl = getEntity().getConfig(NodeJsWebAppService.APP_ARCHIVE_URL);
        String appName = getEntity().getConfig(NodeJsWebAppService.APP_NAME);

        if (Strings.isNonBlank(gitRepoUrl) && Strings.isNonBlank(archiveUrl)) {
            throw new IllegalStateException("Only one of Git or archive URL must be set");
        } else if (Strings.isNonBlank(gitRepoUrl)) {
            commands.add(String.format("git clone %s %s", gitRepoUrl, appName));
        } else if (Strings.isNonBlank(archiveUrl)) {
            ArchiveUtils.deploy(archiveUrl, getMachine(), getRunDir());
        } else {
            throw new IllegalStateException("At least one of Git or archive URL must be set");
        }

        newScript(CUSTOMIZING)
                .body.append(commands)
                .execute();
    }

    @Override
    public void launch() {
        List<String> commands = Lists.newLinkedList();

        String appName = getEntity().getConfig(NodeJsWebAppService.APP_NAME);
        String appFile = getEntity().getConfig(NodeJsWebAppService.APP_FILE);
        String appCommand = getEntity().getConfig(NodeJsWebAppService.APP_COMMAND);

        commands.add(String.format("cd %s", Os.mergePathsUnix(getRunDir(), appName)));
        commands.add(BashCommands.sudo("nohup " + appCommand + " " + appFile + " &"));

        newScript(MutableMap.of(USE_PID_FILE, true), LAUNCHING)
                .body.append(commands)
                .execute();
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of(USE_PID_FILE, true), CHECK_RUNNING).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(MutableMap.of(USE_PID_FILE, true), STOPPING).execute();
    }

    @Override
    public Map<String, String> getShellEnvironment() {
        return MutableMap.<String, String>builder().putAll(super.getShellEnvironment())
                .put("PORT", Integer.toString(getHttpPort()))
                .build();
    }

}
