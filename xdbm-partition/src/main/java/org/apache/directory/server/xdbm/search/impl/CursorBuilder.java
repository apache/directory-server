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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.util.exception.NotImplementedException;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SearchScope;


/**
 * Builds Cursors over candidates that satisfy a filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CursorBuilder<ID extends Comparable<ID>>
{
    /** The database used by this builder */
    private Store<Entry, ID> db = null;

    /** Evaluator dependency on a EvaluatorBuilder */
    private EvaluatorBuilder<ID> evaluatorBuilder;


    /**
     * Creates an expression tree enumerator.
     *
     * @param db database used by this enumerator
     * @param evaluatorBuilder the evaluator builder
     */
    public CursorBuilder( Store<Entry, ID> db, EvaluatorBuilder<ID> evaluatorBuilder )
    {
        this.db = db;
        this.evaluatorBuilder = evaluatorBuilder;
    }


    public <T> IndexCursor<?, Entry, ID> build( ExprNode node ) throws Exception
    {
        switch ( node.getAssertionType() )
        {
            /* ---------- LEAF NODE HANDLING ---------- */

            case APPROXIMATE:
                return new ApproximateCursor<T, ID>( db, ( ApproximateEvaluator<T, ID> ) evaluatorBuilder.build( node ) );

            case EQUALITY:
                return new EqualityCursor<T, ID>( db, ( EqualityEvaluator<T, ID> ) evaluatorBuilder.build( node ) );

            case GREATEREQ:
                return new GreaterEqCursor<T, ID>( db, ( GreaterEqEvaluator<T, ID> ) evaluatorBuilder.build( node ) );

            case LESSEQ:
                return new LessEqCursor<T, ID>( db, ( LessEqEvaluator<T, ID> ) evaluatorBuilder.build( node ) );

            case PRESENCE:
                return new PresenceCursor<ID>( db, ( PresenceEvaluator<ID> ) evaluatorBuilder.build( node ) );

            case SCOPE:
                if ( ( ( ScopeNode ) node ).getScope() == SearchScope.ONELEVEL )
                {
                    return new OneLevelScopeCursor<ID>( db,
                        ( OneLevelScopeEvaluator<Entry, ID> ) evaluatorBuilder.build( node ) );
                }
                else
                {
                    return new SubtreeScopeCursor<ID>( db, ( SubtreeScopeEvaluator<Entry, ID> ) evaluatorBuilder
                        .build( node ) );
                }

            case SUBSTRING:
                return new SubstringCursor<ID>( db, ( SubstringEvaluator<ID> ) evaluatorBuilder.build( node ) );

                /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                return buildAndCursor( ( AndNode ) node );

            case NOT:
                return new NotCursor<ID, ID>( db, evaluatorBuilder.build( ( ( NotNode ) node ).getFirstChild() ) );

            case OR:
                return buildOrCursor( ( OrNode ) node );

                /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_260, node.getAssertionType() ) );
        }
    }


    /**
     * Creates a OrCursor over a disjunction expression branch node.
     *
     * @param node the disjunction expression branch node
     * @return Cursor over candidates satisfying disjunction expression
     * @throws Exception on db access failures
     */
    private IndexCursor<?, Entry, ID> buildOrCursor( OrNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<IndexCursor<? extends Object, Entry, ID>> childCursors = new ArrayList<IndexCursor<?, Entry, ID>>(
            children.size() );
        List<Evaluator<? extends ExprNode, Entry, ID>> childEvaluators = new ArrayList<Evaluator<? extends ExprNode, Entry, ID>>(
            children.size() );

        // Recursively create Cursors and Evaluators for each child expression node
        for ( ExprNode child : children )
        {
            childCursors.add( build( child ) );
            childEvaluators.add( evaluatorBuilder.build( child ) );
        }

        return new OrCursor( childCursors, childEvaluators );
    }


    /**
     * Creates an AndCursor over a conjunction expression branch node.
     *
     * @param node a conjunction expression branch node
     * @return Cursor over the conjunction expression
     * @throws Exception on db access failures
     */
    private IndexCursor<?, Entry, ID> buildAndCursor( AndNode node ) throws Exception
    {
        int minIndex = 0;
        long minValue = Long.MAX_VALUE;
        long value = Long.MAX_VALUE;

        /*
         * We scan the child nodes of a branch node searching for the child
         * expression node with the smallest scan count.  This is the child
         * we will use for iteration by creating a Cursor over its expression.
         */
        final List<ExprNode> children = node.getChildren();

        for ( int i = 0; i < children.size(); i++ )
        {
            ExprNode child = children.get( i );
            Object count = child.get( "count" );
            
            if ( count == null )
            {
                continue;
            }
            
            value = ( Long ) count;
            minValue = Math.min( minValue, value );

            if ( minValue == value )
            {
                minIndex = i;
            }
        }

        // Once found we build the child Evaluators minus the one for the minChild
        ExprNode minChild = children.get( minIndex );
        List<Evaluator<? extends ExprNode, Entry, ID>> childEvaluators = new ArrayList<Evaluator<? extends ExprNode, Entry, ID>>(
            children.size() - 1 );
        
        for ( ExprNode child : children )
        {
            if ( child == minChild )
            {
                continue;
            }

            childEvaluators.add( evaluatorBuilder.build( child ) );
        }

        // Do recursive call to build min child Cursor then create AndCursor
        IndexCursor<?, Entry, ID> childCursor = build( minChild );
        
        return new AndCursor( childCursor, childEvaluators );
    }
}
