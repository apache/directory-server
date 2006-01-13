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
package org.apache.ldap.server;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.schema.GlobalRegistries;


/**
 * Default implementation of {@link DirectoryServiceConfiguration}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
class DefaultDirectoryServiceConfiguration implements DirectoryServiceConfiguration
{
    private DefaultDirectoryService parent;
    
    DefaultDirectoryServiceConfiguration( DefaultDirectoryService parent )
    {
        this.parent = parent;
    }
    
    public String getInstanceId()
    {
        return parent.getInstanceId();
    }
    
    public DirectoryService getService()
    {
        return parent;
    }

    public DirectoryServiceListener getServiceListener()
    {
        return parent.getServiceListener();
    }

    public Hashtable getEnvironment()
    {
        return parent.getEnvironment();
    }
    
    public StartupConfiguration getStartupConfiguration()
    {
        return parent.getStartupConfiguration();
    }
    
    public GlobalRegistries getGlobalRegistries()
    {
        return parent.getGlobalRegistries();
    }

    public DirectoryPartitionNexus getPartitionNexus()
    {
        return parent.getPartitionNexus();
    }
    
    public InterceptorChain getInterceptorChain()
    {
        return parent.getInterceptorChain();
    }
    
    public boolean isFirstStart()
    {
        return parent.isFirstStart();
    }

    public Context getJndiContext( String baseName ) throws NamingException
    {
        return parent.getJndiContext( baseName );
    }

    public Context getJndiContext( String principal, byte[] credential, String authentication, String baseName ) throws NamingException
    {
        return parent.getJndiContext( principal, credential, authentication, baseName );
    }
}
