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
package org.apache.directory.server.tools.request;


import org.apache.directory.server.tools.ToolCommand;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Interface that defines the operations available for commands
 * that want to be used in program with a user interface
 *
 */
public interface ToolCommandGui extends ToolCommand
{
    /**
     * Executes the command
     * @param params the parameters of the command
     * @param listeners the listeners of the command
     * @throws Exception
     */
    public void execute( Parameter[] params, ListenerParameter[] listeners ) throws Exception;


    /**
     * Processes the parameters given.
     * Checks and verifies the data entered by the user.
     * @param params the params
     * @throws Exception
     */
    public void processParams( Parameter[] params ) throws Exception;


    /**
     * Returns the ToolCommandExecutorStub of the command
     * @return the ToolCommandExecutorStub
     */
    public ToolCommandExecutorStub getStub();
}
