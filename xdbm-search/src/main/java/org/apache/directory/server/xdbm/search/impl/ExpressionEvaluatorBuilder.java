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


import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.filter.*;
import org.apache.directory.shared.ldap.NotImplementedException;


/**
 * Top level filter expression evaluator builder implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ExpressionEvaluatorBuilder implements EvaluatorBuilder<Attributes>
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
     * @param registries the schema registries
     * @throws Exception failure to access db or lookup schema in registries
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
    public Evaluator<? extends ExprNode, Attributes> build( ExprNode node ) throws Exception
    {
        switch ( node.getAssertionType() )
        {
            case APPROXIMATE:
                return new ApproximateEvaluator( ( ApproximateNode ) node, db, registries );
            case EQUALITY:
                return new EqualityEvaluator( ( EqualityNode ) node, db, registries );
            case GREATEREQ:
                return new GreaterEqEvaluator( ( GreaterEqNode ) node, db, registries );
            case LESSEQ:
                return new LessEqEvaluator( ( LessEqNode ) node, db, registries );
            case PRESENCE:
                return new PresenceEvaluator( ( PresenceNode ) node, db, registries );
            case SCOPE:
//                return new ScopeEvaluator( ( ScopeNode ) node, db, registries );
                throw new NotImplementedException( "SOON!!!!!" );
            case SUBSTRING:
                return new SubstringEvaluator( ( SubstringNode ) node, db, registries );

                /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                throw new NotImplementedException();
            case NOT:
                throw new NotImplementedException();
            case OR:
                throw new NotImplementedException();

                /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( "Unknown assertion type: " + node.getAssertionType() );
        }

//        BranchNode bnode = ( BranchNode ) node;
//
//        if ( bnode instanceof OrNode )
//        {
//            for ( ExprNode child:bnode.getChildren() )
//            {
//                if ( evaluate( child, entry ) )
//                {
//                    return true;
//                }
//            }
//
//            return false;
//        }
//        else if ( bnode instanceof AndNode )
//        {
//            for ( ExprNode child:bnode.getChildren() )
//            {
//                if ( !evaluate( child, entry ) )
//                {
//                    return false;
//                }
//            }
//
//            return true;
//        }
//        else if ( bnode instanceof NotNode )
//        {
//            if ( null != bnode.getFirstChild() )
//            {
//                return !evaluate( bnode.getFirstChild(), entry );
//            }
//
//            throw new NamingException( "Negation has no child: " + node );
//        }
//        else
//        {
//                throw new NamingException( "Unrecognized branch node operator: " + bnode );
//        }
    }
}
