package brooklyn.util.task.ssh;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.internal.ssh.SshTool;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.text.Strings;

import com.google.common.base.Function;

public class SshTasks {

    private static final Logger log = LoggerFactory.getLogger(SshTasks.class);
        
    public static SshTaskFactory<Integer> newSshTaskFactory(SshMachineLocation machine, String ...commands) {
        return new PlainSshTaskFactory<Integer>(machine, commands);
    }

    public static SshPutTaskFactory newSshPutTaskFactory(SshMachineLocation machine, String remoteFile) {
        return new SshPutTaskFactory(machine, remoteFile);
    }

    public static SshFetchTaskFactory newSshFetchTaskFactory(SshMachineLocation machine, String remoteFile) {
        return new SshFetchTaskFactory(machine, remoteFile);
    }

    /** creates a task which returns modifies sudoers to ensure non-tty access is permitted;
     * also gives nice warnings if sudo is not permitted */
    public static SshTaskFactory<Boolean> dontRequireTtyForSudo(SshMachineLocation machine, final boolean requireSuccess) {
        return newSshTaskFactory(machine, 
                BashCommands.dontRequireTtyForSudo())
            .summary("setting up sudo")
            .configure(SshTool.PROP_ALLOCATE_PTY, true)
            .allowingNonZeroExitCode()
            .returning(new Function<SshTaskWrapper<?>,Boolean>() { public Boolean apply(SshTaskWrapper<?> task) {
                if (task.getExitCode()==0) return true;
                log.warn("Error setting up sudo for "+task.getMachine().getUser()+"@"+task.getMachine().getAddress().getHostName()+": "+
                        Strings.trim(Strings.isNonBlank(task.getStderr()) ? task.getStderr() : task.getStdout()) + 
                        " (exit code "+task.getExitCode()+")");
                if (requireSuccess) {
                    throw new IllegalStateException("Passwordless sudo is required for "+task.getMachine().getUser()+"@"+task.getMachine().getAddress().getHostName());
                }
                return true; 
            } });
    }

    /** Function for use in {@link SshTaskFactory#returning(Function)} which logs all information, optionally requires zero exit code, 
     * and then returns stdout */
    public static Function<SshTaskWrapper<?>, String> returningStdoutLoggingInfo(final Logger logger, final boolean requireZero) {
        return new Function<SshTaskWrapper<?>, String>() {
          public String apply(@Nullable SshTaskWrapper<?> input) {
            if (logger!=null) logger.info(input+" COMMANDS:\n"+Strings.join(input.getCommands(),"\n"));
            if (logger!=null) logger.info(input+" STDOUT:\n"+input.getStdout());
            if (logger!=null) logger.info(input+" STDERR:\n"+input.getStderr());
            if (requireZero && input.getExitCode()!=0) 
                throw new IllegalStateException("non-zero exit code in "+input.getSummary()+": see log for more details!");
            return input.getStdout();
          }
        };
    }

}
