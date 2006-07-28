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


import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.directory.server.tools.BaseToolCommand;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Abstract class that must extended by every command that want
 * to be used via Command Line
 *
 */
public abstract class BaseToolCommandCL extends BaseToolCommand implements ToolCommandCL
{
    /** The List of parameters that will be filled when parsing the command line */
    protected List parameters;


    /**
     * Defalut contructor
     * @param name the name of the command
     */
    public BaseToolCommandCL( String name )
    {
        super( name );

        parameters = new ArrayList();
    }


    /* (non-Javadoc)
     * @see org.safehaus.ldapstudio.server.tools.request.ToolCommandCL#execute(org.apache.commons.cli.CommandLine, org.safehaus.ldapstudio.server.tools.util.ListenerParameter[])
     */
    public void execute( CommandLine cmd, ListenerParameter[] listeners ) throws Exception
    {
        assert ( parameters != null );
        assert ( parameters.size() != 0 );

        processOptions( cmd );

        ToolCommandExecutorStub toolCommandExecutorStub = getStub();
        toolCommandExecutorStub.execute( ( Parameter[] ) parameters.toArray( new Parameter[0] ), listeners );
    }
}
