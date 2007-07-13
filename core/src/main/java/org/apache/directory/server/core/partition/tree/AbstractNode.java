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
package org.apache.directory.server.core.partition.tree;

import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * This abstract class is just used to implement the utility method needed to build the
 * global partition structue used by the getBackend method. 
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */

public abstract class AbstractNode implements Node
{
    /**
     * @see Node#buildNode(Node, LdapDN, int, Partition)
     */
    public Node buildNode( Node current, LdapDN dn, int index, Partition partition )
    {
        if ( index == dn.size() - 1 )
        {
            return current.addNode( dn.getRdn( index ).toString(), new LeafNode( partition ) );
        }
        else
        {
            return current.addNode( dn.getRdn( index ).toString(), 
                buildNode( new BranchNode(), dn, index + 1, partition ) );
        }
    }
}
