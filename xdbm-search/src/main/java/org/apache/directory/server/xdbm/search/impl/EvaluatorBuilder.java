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


import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.filter.*;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.NotImplementedException;

import java.util.List;
import java.util.ArrayList;


/**
 * Top level filter expression evaluator builder implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class EvaluatorBuilder
{
    private final Store<ServerEntry> db;
    private final Registries registries;


    /**
     * Creates a top level Evaluator where leaves are delegated to a leaf node
     * evaluator which will be created.
     *
     * @param db the database this evaluator operates upon
     * @param registries the schema registries
     * @throws Exception failure to access db or lookup schema in registries
     */
    public EvaluatorBuilder( Store<ServerEntry> db, Registries registries ) throws Exception
    {
        this.db = db;
        this.registries = registries;
    }


    public Evaluator<? extends ExprNode, ServerEntry> build( ExprNode node ) throws Exception
    {
        switch ( node.getAssertionType() )
        {
            /* ---------- LEAF NODE HANDLING ---------- */

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
                if ( ( ( ScopeNode ) node ).getScope() == SearchScope.ONELEVEL )
                {
                    return new OneLevelScopeEvaluator<ServerEntry>( db, ( ScopeNode ) node );
                }
                else
                {
                    return new SubtreeScopeEvaluator<ServerEntry>( db, ( ScopeNode ) node );
                }
            case SUBSTRING:
                return new SubstringEvaluator( ( SubstringNode ) node, db, registries );

            /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                return buildAndEvaluator( ( AndNode ) node );
            case NOT:
                return new NotEvaluator( ( NotNode ) node, build( ( ( NotNode ) node).getFirstChild() ) );
            case OR:
                return buildOrEvaluator( ( OrNode ) node );

            /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( "Unknown assertion type: " + node.getAssertionType() );
        }
    }


    AndEvaluator buildAndEvaluator( AndNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode,ServerEntry>> evaluators =
            new ArrayList<Evaluator<? extends ExprNode,ServerEntry>>( children.size() );
        for ( ExprNode child : children )
        {
            evaluators.add( build( child ) );
        }
        return new AndEvaluator( node, evaluators );
    }


    OrEvaluator buildOrEvaluator( OrNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode,ServerEntry>> evaluators =
            new ArrayList<Evaluator<? extends ExprNode,ServerEntry>>( children.size() );
        for ( ExprNode child : children )
        {
            evaluators.add( build( child ) );
        }
        return new OrEvaluator( node, evaluators );
    }
}
