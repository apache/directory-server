/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.tools;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.InstallationLayout;
import org.apache.directory.server.configuration.ServerStartupConfiguration;


/**
 * Simple base class for tool commands.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class ToolCommand
{
    private final String name;
    private boolean debugEnabled = false;
    private boolean verboseEnabled = false;
    private boolean quietEnabled = false;
    private String version;
    private InstallationLayout layout;
    private ServerStartupConfiguration configuration;


    protected ToolCommand(String name)
    {
        this.name = name;
    }


    public abstract void execute( CommandLine cmd ) throws Exception;


    public abstract Options getOptions();


    public String getName()
    {
        return this.name;
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


    public void setVersion( String version )
    {
        this.version = version;
    }


    public String getVersion()
    {
        return version;
    }


    public String toString()
    {
        return getName();
    }


    public void setDebugEnabled( boolean debugEnabled )
    {
        this.debugEnabled = debugEnabled;
    }


    public boolean isDebugEnabled()
    {
        return debugEnabled;
    }


    public void setVerboseEnabled( boolean verboseEnabled )
    {
        this.verboseEnabled = verboseEnabled;
    }


    public boolean isVerboseEnabled()
    {
        return verboseEnabled;
    }


    public void setQuietEnabled( boolean quietEnabled )
    {
        this.quietEnabled = quietEnabled;
    }


    public boolean isQuietEnabled()
    {
        return quietEnabled;
    }
}
