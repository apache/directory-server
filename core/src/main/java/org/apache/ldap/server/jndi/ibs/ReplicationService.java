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
package org.apache.ldap.server.jndi.ibs;


import org.apache.ldap.server.RootNexus;
import org.apache.ldap.server.jndi.BaseInterceptor;
import org.apache.ldap.server.RootNexus;


/**
 * A service used for replicating changes to other backing stores and applying
 * changes to from other replicas to this backing store.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReplicationService extends BaseInterceptor
{
    /** the root nexus to all database partitions */
    private final RootNexus nexus;


    /**
     * Creates a replication service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     */
    public ReplicationService( RootNexus nexus )
    {
        this.nexus = nexus;
    }
}
