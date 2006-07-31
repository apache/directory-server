/*
 *   @(#) $Id$
 *
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
package org.apache.directory.server.core.configuration;


import java.io.File;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.DirectoryService;


/**
 * A mutable version of {@link StartupConfiguration}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MutableStartupConfiguration extends StartupConfiguration
{
    private static final long serialVersionUID = -987437370955222007L;


    /**
     * Creates a new instance.
     */
    public MutableStartupConfiguration()
    {
    }

    
    /**
     * Creates a new instance that operates on the {@link DirectoryService} with
     * the specified ID.
     */
    public MutableStartupConfiguration(String instanceId)
    {
        super( instanceId );
    }


    public void setSystemPartitionConfiguration( PartitionConfiguration systemPartitionConfiguration )
    {
        super.setSystemPartitionConfiguration( systemPartitionConfiguration );
    }
    
    
    public void setMaxThreads( int maxThreads )
    {
        super.setMaxThreads( maxThreads );
    }
    
    
    public void setMaxTimeLimit( int maxTimeLimit )
    {
        super.setMaxTimeLimit( maxTimeLimit );
    }
    
    
    public void setMaxSizeLimit( int maxSizeLimit )
    {
        super.setMaxSizeLimit( maxSizeLimit );
    }
    

    public void setInstanceId( String instanceId )
    {
        super.setInstanceId( instanceId );
    }


    public void setAuthenticatorConfigurations( Set authenticators )
    {
        super.setAuthenticatorConfigurations( authenticators );
    }


    public void setBootstrapSchemas( Set bootstrapSchemas )
    {
        super.setBootstrapSchemas( bootstrapSchemas );
    }


    public void setContextPartitionConfigurations( Set contextParitionConfigurations )
    {
        super.setContextPartitionConfigurations( contextParitionConfigurations );
    }


    public void setAccessControlEnabled( boolean accessControlEnabled )
    {
        super.setAccessControlEnabled( accessControlEnabled );
    }


    public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
    {
        super.setAllowAnonymousAccess( enableAnonymousAccess );
    }


    public void setInterceptorConfigurations( List interceptorConfigurations )
    {
        super.setInterceptorConfigurations( interceptorConfigurations );
    }


    public void setTestEntries( List testEntries )
    {
        super.setTestEntries( testEntries );
    }


    public void setWorkingDirectory( File workingDirectory )
    {
        super.setWorkingDirectory( workingDirectory );
    }


    public void setShutdownHookEnabled( boolean shutdownHookEnabled )
    {
        super.setShutdownHookEnabled( shutdownHookEnabled );
    }


    public void setExitVmOnShutdown( boolean exitVmOnShutdown )
    {
        super.setExitVmOnShutdown( exitVmOnShutdown );
    }
}
