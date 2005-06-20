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
package org.apache.ldap.server.authn;


import org.apache.ldap.server.configuration.AuthenticatorConfiguration;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.partition.PartitionNexus;


/**
 * Default implementation of AuthenticatorContext.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class GenericAuthenticatorContext implements AuthenticatorContext
{
    private StartupConfiguration rootConfiguration;
    private AuthenticatorConfiguration configuration;
    /** the root nexus to all database partitions */
    private PartitionNexus partitionNexus;

    /**
     * Create a new AuthenticatorContext.
     */
    public GenericAuthenticatorContext(
            StartupConfiguration rootConfiguration,
            AuthenticatorConfiguration configuration,
            PartitionNexus partitionNexus )
    {
        this.rootConfiguration = rootConfiguration;
        this.configuration = configuration;
        this.partitionNexus = partitionNexus;
    }
    
    public StartupConfiguration getRootConfiguration()
    {
        return rootConfiguration;
    }
    
    public AuthenticatorConfiguration getConfiguration()
    {
        return configuration;
    }

    public PartitionNexus getPartitionNexus()
    {
        return partitionNexus;
    }
}