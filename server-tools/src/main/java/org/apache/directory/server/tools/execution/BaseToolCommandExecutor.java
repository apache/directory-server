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
package org.apache.directory.server.tools.execution;


import java.io.File;

import org.apache.directory.daemon.InstallationLayout;
import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.tools.BaseToolCommand;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Abstract class that must be extended by every Executor
 *
 */
public abstract class BaseToolCommandExecutor extends BaseToolCommand implements ToolCommandExecutor
{
    // Parameters
    public static final String HOST_PARAMETER = "host";
    public static final String PORT_PARAMETER = "port";
    public static final String USER_PARAMETER = "user";
    public static final String PASSWORD_PARAMETER = "password";
    public static final String AUTH_PARAMETER = "auth";
    public static final String INSTALLPATH_PARAMETER = "install-path";
    public static final String CONFIGURATION_PARAMETER = "configuration";
    public static final String DEBUG_PARAMETER = "debug";
    public static final String VERBOSE_PARAMETER = "verbose";
    public static final String QUIET_PARAMETER = "quiet";

    // Listeners Parameters
    public static final String OUTPUTLISTENER_PARAMETER = "ouputListener";
    public static final String ERRORLISTENER_PARAMETER = "errorListener";
    public static final String EXCEPTIONLISTENER_PARAMETER = "exceptionListener";

    private InstallationLayout layout;
    private ServerStartupConfiguration configuration;


    public BaseToolCommandExecutor( String name )
    {
        super( name );
    }


    public void setLayout( File installationDirectory )
    {
        this.layout = new InstallationLayout( installationDirectory );
    }


    public void setLayout( String installationPath )
    {
        this.layout = new InstallationLayout( installationPath );
    }


    public void setLayout( InstallationLayout layout )
    {
        this.layout = layout;
    }


    public InstallationLayout getLayout()
    {
        return layout;
    }


    public void setConfiguration( ServerStartupConfiguration configuration )
    {
        this.configuration = configuration;
    }


    public ServerStartupConfiguration getConfiguration()
    {
        return configuration;
    }


    public abstract void execute( Parameter[] params, ListenerParameter[] listeners );
}
