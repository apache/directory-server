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
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.BranchNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.xdbm.search.Optimizer;


/**
 * A do nothing optimizer which labels all nodes with <code>
 * BigInteger.valueOf( Integer.MAX_VALUE ) </code>, instead of actually 
 * taking scan counts.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NoOpOptimizer implements Optimizer
{
    /** the maximum size for a count Integer.MAX_VALUE as a BigInteger */
    private static final Long MAX = Long.MAX_VALUE;


    public Long annotate( PartitionTxn partitionTxn, ExprNode node ) throws LdapException
    {
        if ( node.isLeaf() )
        {
            node.set( "count", MAX );
            return MAX;
        }

        BranchNode bnode = ( BranchNode ) node;
        if ( bnode.getChildren().size() == 0 )
        {
            bnode.set( "count", MAX );
            return MAX;
        }

        final int limit = bnode.getChildren().size();
        for ( int ii = 0; ii < limit; ii++ )
        {
            ExprNode child = bnode.getChildren().get( ii );
            if ( child.isLeaf() )
            {
                child.set( "count", MAX );
            }
            else
            {
                annotate( partitionTxn, child );
            }
        }

        bnode.set( "count", MAX );
        return MAX;
    }
}
