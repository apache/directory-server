package org.apache.directory.server.tools.commands.exportcmd;


import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.execution.ToolCommandExecutorSkeleton;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


public class ExportCommandExecutorSkeleton implements ToolCommandExecutorSkeleton
{

    public void execute( Parameter[] params, ListenerParameter[] listeners )
    {
        BaseToolCommandExecutor executor = new ExportCommandExecutor();

        executor.execute( params, listeners );
    }

}
