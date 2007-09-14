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
package org.apache.directory.server.core.partition.impl.btree;


import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

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
 * Optimizer that annotates the filter using scan counts.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultOptimizer implements Optimizer
{
    /** the database this optimizer operates on */
    private BTreePartition db;

    /**
     * Creates an optimizer on a database.
     *
     * @param db the database this optimizer works for.
     */
    public DefaultOptimizer(BTreePartition db)
    {
        this.db = db;
    }


    /**
     * Annotates the expression tree to determine optimal evaluation order based
     * on the scan count for indices that exist for each expression node.  If an
     * index on the attribute does not exist an IndexNotFoundException will be
     * thrown.
     *
     * @see Optimizer#annotate(ExprNode)
     */
    public void annotate( ExprNode node ) throws NamingException
    {
        // Start off with the worst case unless scan count says otherwise.
        long count = Long.MAX_VALUE;

        /* --------------------------------------------------------------------
         *                 H A N D L E   L E A F   N O D E S          
         * --------------------------------------------------------------------
         * 
         * Each leaf node is based on an attribute and it represents a condition
         * that needs to be statisfied.  We ask the index (if one exists) for 
         * the attribute to give us a scan count of all the candidates that 
         * would satisfy the attribute assertion represented by the leaf node.
         * 
         * This is conducted differently based on the type of the leaf node.
         * Comments on each node type explain how each scan count is arrived at.
         */

        if ( node instanceof ScopeNode )
        {
            count = getScopeScan( ( ScopeNode ) node );
        }
        else if ( node instanceof AssertionNode )
        {
            /* 
             * Leave it up to the assertion node to determine just how much it
             * will cost us.  Anyway it defaults to a maximum scan count if a
             * scan count is not specified by the implementation.
             */
        }
        else if ( node.isLeaf() )
        {
            LeafNode leaf = ( LeafNode ) node;

            if ( node instanceof PresenceNode )
            {
                count = getPresenceScan( ( PresenceNode ) leaf );
            }
            else if ( node instanceof EqualityNode )
            {
                count = getEqualityScan( ( EqualityNode ) leaf );
            }
            else if ( node instanceof GreaterEqNode )
            {
                count = getGreaterLessScan( ( GreaterEqNode ) leaf, SimpleNode.EVAL_GREATER );
            }
            else if ( node instanceof LessEqNode )
            {
                count = getGreaterLessScan( ( SimpleNode ) leaf, SimpleNode.EVAL_LESSER );
            }
            else if ( node instanceof SubstringNode )
            {
                /** Cannot really say so we presume the total index count */
                count = getFullScan( leaf );
            }
            else if ( node instanceof ExtensibleNode )
            {
                /** Cannot really say so we presume the total index count */
                count = getFullScan( leaf );
            }
            else if ( node instanceof ApproximateNode )
            {
                /** Feature not implemented so we just use equality matching */
                count = getEqualityScan( ( ApproximateNode ) leaf );
            }
            else
            {
                throw new IllegalArgumentException( "Unrecognized leaf node" );
            }
        }
        // --------------------------------------------------------------------
        //                 H A N D L E   B R A N C H   N O D E S       
        // --------------------------------------------------------------------
        else
        {
            if ( node instanceof AndNode )
            {
            	count = getConjunctionScan( (AndNode)node );
            }
            else if ( node instanceof OrNode )
            {
            	count = getDisjunctionScan( (OrNode)node );
            }
            else if ( node instanceof NotNode )
            {
            	count = getNegationScan( (NotNode)node );
            }
            else
            {
            	throw new IllegalArgumentException( "Unrecognized branch node type" );
            }
        }

        // Protect against overflow when counting.
        if ( count < 0L )
        {
            count = Long.MAX_VALUE;
        }

        node.set( "count", count );
    }


    /**
     * ANDs or Conjunctions take the count of the smallest child as their count.
     * This is the best that a conjunction can do and should be used rather than
     * the worst case. Notice that we annotate the child node with a recursive 
     * call before accessing its count parameter making the chain recursion 
     * depth first.
     *
     * @param node a AND (Conjunction) BranchNode
     * @return the calculated scan count
     * @throws NamingException if there is an error
     */
    private long getConjunctionScan( BranchNode node ) throws NamingException
    {
        long count = Long.MAX_VALUE;
        List<ExprNode> children = node.getChildren();

        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = children.get( ii );
            annotate( child );
            count = Math.min( ( ( Long ) child.get( "count" ) ), count );
        }

        return count;
    }


    /**
     * A negation filter is always worst case since we will have to retrieve all
     * entries from the master table then test each one against the negated
     * child filter.  There is no way to use the indices.
     *
     * @param node the negation node
     * @return the scan count
     * @throws NamingException if there is an error
     */
    private long getNegationScan( BranchNode node ) throws NamingException
    {
        return Long.MAX_VALUE;
    }


    /**
     * Disjunctions (OR) are the union of candidates across all subexpressions 
     * so we add all the counts of the child nodes. Notice that we annotate the 
     * child node with a recursive call.
     *
     * @param node the OR branch node
     * @return the scan count on the OR node
     * @throws NamingException if there is an error
     */
    private long getDisjunctionScan( BranchNode node ) throws NamingException
    {
        List<ExprNode> children = node.getChildren();
        long total = 0L;

        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = children.get( ii );
            annotate( child );
            total += ( Long ) child.get( "count" );
        }
        
        return total;
    }


    /**
     * Gets the worst case scan count for all entries that satisfy the equality
     * assertion in the SimpleNode argument.  
     *
     * @param node the node to get a scan count for 
     * @return the worst case
     * @throws NamingException if there is an error accessing an index
     */
    private long getEqualityScan( SimpleNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            return Long.valueOf( idx.count( node.getValue() ) );
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return Long.MAX_VALUE;
    }


    /**
     * Gets a scan count of the nodes that satisfy the greater or less than test
     * specified by the node.
     *
     * @param node the greater or less than node to get a count for 
     * @param isGreaterThan if true test is for >=, otherwise <=
     * @return the scan count of all nodes satisfying the AVA
     * @throws NamingException if there is an error accessing an index
     */
    private long getGreaterLessScan( SimpleNode node, boolean isGreaterThan ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            int count = idx.count( node.getValue(), isGreaterThan );
            return Long.valueOf( count );
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return Long.MAX_VALUE;
    }


    /**
     * Gets the total number of entries within the database index if one is 
     * available otherwise the count of all the entries within the database is
     * returned.
     *
     * @param node the leaf node to get a full scan count for 
     * @return the worst case full scan count
     * @throws NamingException if there is an error access database indices
     */
    private long getFullScan( LeafNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            int count = idx.count();
            return Long.valueOf( count );
        }

        return Long.MAX_VALUE;
    }


    /**
     * Gets the number of entries that would be returned by a presence node
     * assertion.  Leverages the existance system index for scan counts.
     *
     * @param node the presence node
     * @return the number of entries matched for the presence of an attribute
     * @throws NamingException if errors result
     */
    private long getPresenceScan( PresenceNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getExistanceIndex();
            int count = idx.count( node.getAttribute() );
            return Long.valueOf( count );
        }

        return Long.MAX_VALUE;
    }


    /**
     * Gets the scan count for the scope node attached to this filter.
     *
     * @param node the ScopeNode
     * @return the scan count for scope
     * @throws NamingException if any errors result
     */
    private long getScopeScan( ScopeNode node ) throws NamingException
    {
        switch ( node.getScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                return 1L;
            
            case ( SearchControls.ONELEVEL_SCOPE  ):
                long id = db.getEntryId( node.getBaseDn() );
                return Long.valueOf( db.getChildCount( id ) );
                
            case ( SearchControls.SUBTREE_SCOPE  ):
                return Long.valueOf( db.count() );
            
            default:
                throw new IllegalArgumentException( "Unrecognized search scope " + "value for filter scope node" );
        }
    }
}
