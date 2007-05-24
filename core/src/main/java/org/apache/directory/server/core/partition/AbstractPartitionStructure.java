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
package org.apache.directory.server.core.partition;

import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * This abstract class is just used to implement the utility method needed to build the
 * global partition structue used by the getBackend method. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

public abstract class AbstractPartitionStructure implements PartitionStructure
{
    /**
     * @see PartitionStructure#buildPartitionStructure(PartitionStructure, LdapDN, int, Partition)
     */
    public PartitionStructure buildPartitionStructure( PartitionStructure current, LdapDN dn, int index, Partition partition )
    {
        if ( index == dn.size() - 1 )
        {
            return current.addPartitionHandler( dn.getRdn( index ).toString(), new PartitionHandler( partition ) );
        }
        else
        {
            return current.addPartitionHandler( dn.getRdn( index ).toString(), 
                buildPartitionStructure( new PartitionContainer(), dn, index + 1, partition ) );
        }
    }
}
