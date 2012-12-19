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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.filter.AndNode;
import org.apache.directory.api.ldap.model.filter.ApproximateNode;
import org.apache.directory.api.ldap.model.filter.EqualityNode;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.filter.GreaterEqNode;
import org.apache.directory.api.ldap.model.filter.LessEqNode;
import org.apache.directory.api.ldap.model.filter.NotNode;
import org.apache.directory.api.ldap.model.filter.OrNode;
import org.apache.directory.api.ldap.model.filter.PresenceNode;
import org.apache.directory.api.ldap.model.filter.ScopeNode;
import org.apache.directory.api.ldap.model.filter.SubstringNode;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.exception.NotImplementedException;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.server.xdbm.search.evaluator.AndEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.ApproximateEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.EqualityEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.GreaterEqEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.LessEqEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.NotEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.OneLevelScopeEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.OrEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.PresenceEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.SubstringEvaluator;
import org.apache.directory.server.xdbm.search.evaluator.SubtreeScopeEvaluator;


/**
 * Top level filter expression evaluator builder implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EvaluatorBuilder
{
    private final Store db;
    private final SchemaManager schemaManager;


    /**
     * Creates a top level Evaluator where leaves are delegated to a leaf node
     * evaluator which will be created.
     *
     * @param db the database this evaluator operates upon
     * @param schemaManager the schema manager
     * @throws Exception failure to access db or lookup schema in registries
     */
    public EvaluatorBuilder( Store db, SchemaManager schemaManager ) throws Exception
    {
        this.db = db;
        this.schemaManager = schemaManager;
    }


    public <T> Evaluator<? extends ExprNode> build( ExprNode node ) throws Exception
    {
        Object count = node.get( "count" );
        if ( ( count != null ) && ( ( Long ) count == 0L ) )
        {
            return null;
        }

        switch ( node.getAssertionType() )
        {
        /* ---------- LEAF NODE HANDLING ---------- */

            case APPROXIMATE:
                return new ApproximateEvaluator<T>( ( ApproximateNode<T> ) node, db, schemaManager );

            case EQUALITY:
                return new EqualityEvaluator<T>( ( EqualityNode<T> ) node, db, schemaManager );

            case GREATEREQ:
                return new GreaterEqEvaluator<T>( ( GreaterEqNode<T> ) node, db, schemaManager );

            case LESSEQ:
                return new LessEqEvaluator<T>( ( LessEqNode<T> ) node, db, schemaManager );

            case PRESENCE:
                return new PresenceEvaluator( ( PresenceNode ) node, db, schemaManager );

            case SCOPE:
                if ( ( ( ScopeNode ) node ).getScope() == SearchScope.ONELEVEL )
                {
                    return new OneLevelScopeEvaluator<Entry>( db, ( ScopeNode ) node );
                }
                else
                {
                    return new SubtreeScopeEvaluator( db, ( ScopeNode ) node );
                }

            case SUBSTRING:
                return new SubstringEvaluator( ( SubstringNode ) node, db, schemaManager );

                /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                return buildAndEvaluator( ( AndNode ) node );

            case NOT:
                return new NotEvaluator( ( NotNode ) node, build( ( ( NotNode ) node ).getFirstChild() ) );

            case OR:
                return buildOrEvaluator( ( OrNode ) node );

                /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_260, node.getAssertionType() ) );
        }
    }


    private <T> Evaluator<? extends ExprNode> buildAndEvaluator( AndNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode>> evaluators = buildList( children );

        int size = evaluators.size();

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                return evaluators.get( 0 );

            default:
                return new AndEvaluator( node, evaluators );
        }
    }


    private <T> Evaluator<? extends ExprNode> buildOrEvaluator( OrNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<Evaluator<? extends ExprNode>> evaluators = buildList( children );

        int size = evaluators.size();

        switch ( size )
        {
            case 0:
                return null;

            case 1:
                return evaluators.get( 0 );

            default:
                return new OrEvaluator( node, evaluators );
        }
    }


    private List<Evaluator<? extends ExprNode>> buildList( List<ExprNode> children ) throws Exception
    {
        List<Evaluator<? extends ExprNode>> evaluators = new ArrayList<Evaluator<? extends ExprNode>>(
            children.size() );

        for ( ExprNode child : children )
        {
            Evaluator<? extends ExprNode> evaluator = build( child );

            if ( evaluator != null )
            {
                evaluators.add( evaluator );
            }
        }

        return evaluators;
    }
}
