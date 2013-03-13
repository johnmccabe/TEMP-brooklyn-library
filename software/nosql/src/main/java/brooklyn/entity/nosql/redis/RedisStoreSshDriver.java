package brooklyn.entity.nosql.redis;

import static java.lang.String.format;

import java.util.List;

import brooklyn.entity.basic.AbstractSoftwareProcessSshDriver;
import brooklyn.entity.basic.lifecycle.CommonCommands;
import brooklyn.entity.drivers.downloads.DownloadResolver;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableList;

/**
 * Start a {@link RedisStore} in a {@link Location} accessible over ssh.
 */
public class RedisStoreSshDriver extends AbstractSoftwareProcessSshDriver implements RedisStoreDriver {

    private String expandedInstallDir;

    public RedisStoreSshDriver(RedisStore entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public RedisStore getEntity() {
        return (RedisStore) super.getEntity();
    }
    
    protected Integer getRedisPort() {
        return getEntity().getAttribute(RedisStore.REDIS_PORT);
    }

    private String getExpandedInstallDir() {
        if (expandedInstallDir == null) throw new IllegalStateException("expandedInstallDir is null; most likely install was not called");
        return expandedInstallDir;
    }
    
    @Override
    public void install() {
        DownloadResolver resolver = entity.getManagementContext().getEntityDownloadsManager().resolve(this);
        List<String> urls = resolver.getTargets();
        String saveAs = resolver.getFilename();
        expandedInstallDir = getInstallDir()+"/"+resolver.getUnpackedDirectorName(format("redis-%s", getVersion()));

        /*
         * FIXME On jenkins releng3 box, needed to explicitly install jemalloc:
         *   wget http://www.canonware.com/download/jemalloc/jemalloc-3.3.0.tar.bz2
         *   tar -xvjpf jemalloc-3.3.0.tar.bz2 
         *   cd jemalloc-3.3.0
         *   ./configure
         *   make
         *   sudo make install
         *   cd ..
         *   
         *   cd redis-2.6.7
         *   make distclean
         *   cd deps; make hiredis lua jemalloc linenoise; cd ..
         *   "make LDFLAGS="-all-static"
         */

        List<String> commands = ImmutableList.<String>builder()
                .addAll(CommonCommands.downloadUrlAs(urls, saveAs))
                .add(CommonCommands.INSTALL_TAR)
                .add("tar xzfv " + saveAs)
                .add(format("cd redis-%s", getVersion()))
                .add("make distclean")
                .add("cd deps; make hiredis lua jemalloc linenoise; cd ..")
                .add("make LDFLAGS=\"-all-static\"")
                .build();

        newScript(INSTALLING)
                .failOnNonZeroResultCode()
                .body.append(commands).execute();
    }

    @Override
    public void customize() {
        newScript(MutableMap.of("usePidFile", false), CUSTOMIZING)
                .failOnNonZeroResultCode()
                .body.append(
                        format("cd %s", getExpandedInstallDir()),
                        "make install PREFIX="+getRunDir())
                .execute();
        
        getEntity().doExtraConfigurationDuringStart();
    }
    
    @Override
    public void launch() {
        // TODO Should we redirect stdout/stderr: format(" >> %s/console 2>&1 </dev/null &", getRunDir())
        newScript(MutableMap.of("usePidFile", false), LAUNCHING)
                .failOnNonZeroResultCode()
                .body.append("./bin/redis-server redis.conf")
                .execute();
    }
 

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING)
                .body.append("./bin/redis-cli ping > /dev/null")
                .execute() == 0;
    }

    /**
     * Restarts redis with the current configuration.
     */
    @Override
    public void stop() {
        newScript(MutableMap.of("usePidFile", false), STOPPING)
                .failOnNonZeroResultCode()
                .body.append("./bin/redis-cli shutdown")
                .execute();
    }
}
