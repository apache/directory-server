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
package org.apache.ldap.server.auth;


import org.apache.ldap.server.PartitionNexus;


/**
 * Base class for all Authenticators.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 124525 $
 */
public class AuthenticatorContext
{

    /** the root nexus to all database partitions */
    private PartitionNexus partitionNexus;
    /** whether or not to allow anonymous users */
    private boolean allowAnonymous = false;

    /**
     * Create a new AuthenticatorContext.
     */
    public AuthenticatorContext()
    {
    }

    public PartitionNexus getPartitionNexus()
    {
        return partitionNexus;
    }
    public void setPartitionNexus( PartitionNexus rootNexus )
    {
        this.partitionNexus = rootNexus;
    }

    public boolean getAllowAnonymous()
    {
        return allowAnonymous;
    }

    public void setAllowAnonymous( boolean allowAnonymous )
    {
        this.allowAnonymous = allowAnonymous;
    }

}