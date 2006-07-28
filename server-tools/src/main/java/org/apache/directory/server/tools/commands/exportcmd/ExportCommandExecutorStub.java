package org.apache.directory.server.tools.commands.exportcmd;


import org.apache.directory.server.tools.execution.ToolCommandExecutorSkeleton;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


public class ExportCommandExecutorStub implements ToolCommandExecutorStub
{

    public void execute( Parameter[] params, ListenerParameter[] listeners ) throws Exception
    {
        ToolCommandExecutorSkeleton skeleton = new ExportCommandExecutorSkeleton();

        skeleton.execute( params, listeners );
    }

}
