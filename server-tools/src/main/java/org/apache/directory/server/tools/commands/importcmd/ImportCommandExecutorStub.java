/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.tools.commands.importcmd;


import org.apache.directory.server.tools.execution.ToolCommandExecutorSkeleton;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


public class ImportCommandExecutorStub implements ToolCommandExecutorStub
{

    public void execute( Parameter[] params, ListenerParameter[] listeners ) throws Exception
    {
        ToolCommandExecutorSkeleton skeleton = new ImportCommandExecutorSkeleton();

        skeleton.execute( params, listeners );
    }
}
