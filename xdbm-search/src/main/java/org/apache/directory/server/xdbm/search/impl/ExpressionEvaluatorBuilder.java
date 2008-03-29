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


import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.filter.*;


/**
 * Top level filter expression evaluator implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExpressionEvaluatorBuilder implements EvaluatorBuilder<ExprNode,Attributes>
{
    private final Store<Attributes> db;
    private final Registries registries;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a top level Evaluator where leaves are delegated to a leaf node
     * evaluator which will be created.
     *
     * @param db the database this evaluator operates upon
     */
    public ExpressionEvaluatorBuilder( Store<Attributes> db, Registries registries ) throws Exception
    {
        this.db = db;
        this.registries = registries;
    }


    // ------------------------------------------------------------------------
    // EvaluatorBuilder.build() implementation
    // ------------------------------------------------------------------------


    /**
     * @see EvaluatorBuilder#build(ExprNode)
     */
    public Evaluator<ExprNode, Attributes> build( ExprNode node ) throws NamingException
    {
        if ( node.isLeaf() )
        {
            switch ( )
        }

        BranchNode bnode = ( BranchNode ) node;

        if ( bnode instanceof OrNode )
        {
            for ( ExprNode child:bnode.getChildren() )
            {
                if ( evaluate( child, entry ) )
                {
                    return true;
                }
            }

            return false;
        }
        else if ( bnode instanceof AndNode )
        {
            for ( ExprNode child:bnode.getChildren() )
            {
                if ( !evaluate( child, entry ) )
                {
                    return false;
                }
            }

            return true;
        }
        else if ( bnode instanceof NotNode )
        {
            if ( null != bnode.getFirstChild() )
            {
                return !evaluate( bnode.getFirstChild(), entry );
            }

            throw new NamingException( "Negation has no child: " + node );
        }
        else
        {
                throw new NamingException( "Unrecognized branch node operator: " + bnode );
        }
    }
}
