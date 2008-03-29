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


import java.util.List;
import java.util.ArrayList;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.core.partition.impl.btree.IndexAssertion;
import org.apache.directory.server.core.partition.impl.btree.IndexAssertionEnumeration;
import org.apache.directory.server.core.cursor.Cursor;
import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.filter.AssertionNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;


/**
 * Builds Cursors over candidates that satisfy a filter expression.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 642490 $
 */
public class CursorBuilder
{
    /** The database used by this builder */
    private Store<Attributes> db = null;
    /** Evaluator dependency on a EvaluatorBuilder */
    private EvaluatorBuilder evaluatorBuilder;
    private final Registries registries;


    /**
     * Creates an expression tree enumerator.
     *
     * @param db database used by this enumerator
     * @param evaluatorBuilder the evaluator builder
     * @param registries the schema registries
     */
    public CursorBuilder( Store<Attributes> db,
                          EvaluatorBuilder evaluatorBuilder,
                          Registries registries )
    {
        this.db = db;
        this.evaluatorBuilder = evaluatorBuilder;
        this.registries = registries;
    }


    public Cursor<IndexEntry<?,Attributes>> build( ExprNode node ) throws Exception
    {
        switch ( node.getAssertionType() )
        {
            /* ---------- LEAF NODE HANDLING ---------- */

            case APPROXIMATE:
                throw new NotImplementedException();
            case EQUALITY:
                throw new NotImplementedException();
            case GREATEREQ:
                throw new NotImplementedException();
            case LESSEQ:
                throw new NotImplementedException();
            case PRESENCE:
                throw new NotImplementedException();
            case SCOPE:
                throw new NotImplementedException();
            case SUBSTRING:
                return new SubstringCursor( db, ( SubstringEvaluator ) evaluatorBuilder.build( node ) );

            /* ---------- LOGICAL OPERATORS ---------- */

            case AND:
                return buildAndCursor( ( AndNode ) node );
            case NOT:
                return new NotCursor( db, evaluatorBuilder.build( ( ( NotNode ) node).getFirstChild() ) );
            case OR:
                return buildOrCursor( ( OrNode ) node );

            /* ----------  NOT IMPLEMENTED  ---------- */

            case ASSERTION:
            case EXTENSIBLE:
                throw new NotImplementedException();

            default:
                throw new IllegalStateException( "Unknown assertion type: " + node.getAssertionType() );
        }
    }


    /**
     * Creates an enumeration to enumerate through the set of candidates 
     * satisfying a filter expression.
     * 
     * @param node a filter expression root
     * @return an enumeration over the 
     * @throws NamingException if database access fails
     */
    public NamingEnumeration<ForwardIndexEntry> enumerate( ExprNode node ) throws NamingException
    {
    	NamingEnumeration<ForwardIndexEntry> list = null;

        if ( node instanceof ScopeNode )
        {
            list = scopeEnumerator.enumerate( node );
        }
        else if ( node instanceof AssertionNode )
        {
            throw new IllegalArgumentException( "Cannot produce enumeration " + "on an AssertionNode" );
        }
        else if ( node.isLeaf() )
        {
            LeafNode leaf = ( LeafNode ) node;

            if ( node instanceof PresenceNode )
            {
                list = enumPresence( ( PresenceNode ) node );
            }
            else if ( node instanceof EqualityNode )
            {
                list = enumEquality( ( EqualityNode ) node );
            }
            else if ( node instanceof GreaterEqNode )
            {
                list = enumGreaterOrLesser( ( SimpleNode ) node, SimpleNode.EVAL_GREATER );
            }
            else if ( node instanceof LessEqNode )
            {
                list = enumGreaterOrLesser( ( SimpleNode ) node, SimpleNode.EVAL_LESSER );
            }
            else if ( node instanceof SubstringNode )
            {
                list = substringEnumerator.enumerate( leaf );
            }
            else if ( node instanceof ExtensibleNode )
            {
                // N O T   I M P L E M E N T E D   Y E T !
                throw new NotImplementedException();
            }
            else if ( node instanceof ApproximateNode )
            {
                list = enumEquality( ( EqualityNode ) node );
            }
            else
            {
                throw new IllegalArgumentException( "Unknown leaf assertion" );
            }
        }
        else
        {
            BranchNode branch = ( BranchNode ) node;

            if ( node instanceof AndNode )
            {
                list = enumConj( (AndNode)branch );
            }
            else if ( node instanceof OrNode )
            {
                list = enumDisj( (OrNode)branch );
            }
            else if ( node instanceof NotNode )
            {
                list = enumNeg( (NotNode)branch );
            }
            else
            {
                throw new IllegalArgumentException( "Unknown branch logical operator" );
            }
        }

        return list;
    }


    /**
     * Creates a OrCursor over a disjunction expression branch node.
     *
     * @param node the disjunction expression branch node
     * @return Cursor over candidates satisfying disjunction expression
     * @throws Exception on db or registry access failures
     */
    private Cursor<IndexEntry<?,Attributes>> buildOrCursor( OrNode node ) throws Exception
    {
        List<ExprNode> children = node.getChildren();
        List<Cursor<IndexEntry<?,Attributes>>> childCursors = new ArrayList<Cursor<IndexEntry<?,Attributes>>>(children.size());
        List<Evaluator> childEvaluators = new ArrayList<Evaluator>( children.size() );

        // Recursively create Cursors and Evaluators for each child expression node
        for ( ExprNode child : children )
        {
            childCursors.add( build( child ) );
            childEvaluators.add( evaluatorBuilder.build( child ) );
        }

        //noinspection unchecked
        return new OrCursor( childCursors, childEvaluators );
    }


    /**
     * Creates an AndCursor over a conjunction expression branch node.
     *
     * @param node a conjunction expression branch node
     * @return Cursor over the conjunction expression
     */
    private Cursor<IndexEntry<?,Attributes>> buildAndCursor( AndNode node ) throws Exception
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
        
        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = children.get( ii );
            value = ( Long ) child.get( "count" );
            minValue = Math.min( minValue, value );

            if ( minValue == value )
            {
                minIndex = ii;
            }
        }

        // Once found we build the child Evaluators minus the one for the minChild
        ExprNode minChild = children.get( minIndex );
        List<Evaluator<? extends ExprNode, Attributes>> childEvaluators =
            new ArrayList<Evaluator<? extends ExprNode, Attributes>>( children.size() - 1 );
        for ( ExprNode child : children )
        {
            if ( child == minChild )
            {
                continue;
            }

            childEvaluators.add( evaluatorBuilder.build( child ) );
        }

        // Do recursive call to build min child Cursor then create AndCursor
        Cursor<IndexEntry<?,Attributes>> childCursor = build( minChild );
        return new AndCursor( childCursor, childEvaluators );
    }


    /**
     * Returns an enumeration over candidates that satisfy a presence attribute 
     * value assertion.
     * 
     * @param node the presence AVA node
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration<ForwardIndexEntry> enumPresence( final PresenceNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getExistanceIndex();
            try
            {
                return idx.listIndices( node.getAttribute() );
            }
            catch ( java.io.IOException e )
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return nonIndexedScan( node );
    }


    /**
     * Returns an enumeration over candidates that satisfy a simple greater than
     * or less than or equal to attribute value assertion.
     * 
     * @param node the AVA node
     * @param isGreater true if >= false if <= is used
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration<ForwardIndexEntry> enumGreaterOrLesser( final SimpleNode node, final boolean isGreaterOrLesser ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );

            try
            {
                return idx.listIndices( node.getValue(), isGreaterOrLesser );
            }
            catch ( java.io.IOException e )
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return nonIndexedScan( node );
    }


    /**
     * Returns an enumeration over candidates that satisfy a simple equality 
     * attribute value assertion.
     * 
     * @param node the equality AVA node
     * @return an enumeration over the index records matching the AVA
     * @throws NamingException if there is a failure while accessing the db
     */
    private NamingEnumeration<ForwardIndexEntry> enumEquality( final EqualityNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            try
            {
                return idx.listIndices( node.getValue() );
            }
            catch ( java.io.IOException e )
            {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return nonIndexedScan( node );
    }


    /**
     * Creates a scan over all entries in the database with an assertion to test
     * for the correct evaluation of a filter expression on a LeafNode.
     * 
     * @param node the leaf node to produce a scan over
     * @return the enumeration over all perspective candidates satisfying expr
     * @throws NamingException if db access failures result
     */
    private NamingEnumeration<ForwardIndexEntry> nonIndexedScan( final LeafNode node ) throws NamingException
    {
        try
        {
            NamingEnumeration<ForwardIndexEntry> underlying = db.getNdnIndex().listIndices();
        }
        catch ( java.io.IOException e )
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( IndexEntry entry ) throws NamingException
            {
                return evaluatorBuilder.getLeafEvaluator().evaluate( node, entry );
            }
        };

        return new IndexAssertionEnumeration( underlying, assertion );
    }
}
