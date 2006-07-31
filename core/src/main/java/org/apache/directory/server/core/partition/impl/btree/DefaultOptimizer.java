/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import java.math.BigInteger;
import java.util.ArrayList;

import javax.naming.NamingException;
import javax.naming.directory.SearchControls;

import org.apache.directory.shared.ldap.filter.AssertionNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;


/**
 * Optimizer that annotates the filter using scan counts.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DefaultOptimizer implements Optimizer
{
    /** the maximum size for a count Integer.MAX_VALUE as a BigInteger */
    private static final BigInteger MAX = BigInteger.valueOf( Integer.MAX_VALUE );
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
        BigInteger count = MAX;

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

            switch ( leaf.getAssertionType() )
            {
                case ( LeafNode.APPROXIMATE  ):
                    /** Feature not implemented so we just use equality matching */
                    count = getEqualityScan( ( SimpleNode ) leaf );
                    break;
                case ( LeafNode.EQUALITY  ):
                    count = getEqualityScan( ( SimpleNode ) leaf );
                    break;
                case ( LeafNode.EXTENSIBLE  ):
                    /** Cannot really say so we presume the total index count */
                    count = getFullScan( leaf );
                    break;
                case ( LeafNode.GREATEREQ  ):
                    count = getGreaterLessScan( ( SimpleNode ) leaf, true );
                    break;
                case ( LeafNode.LESSEQ  ):
                    count = getGreaterLessScan( ( SimpleNode ) leaf, false );
                    break;
                case ( LeafNode.PRESENCE  ):
                    count = getPresenceScan( ( PresenceNode ) leaf );
                    break;
                case ( LeafNode.SUBSTRING  ):
                    /** Cannot really say so we presume the total index count */
                    count = getFullScan( leaf );
                    break;
                default:
                    throw new IllegalArgumentException( "Unrecognized leaf node" );
            }
        }
        // --------------------------------------------------------------------
        //                 H A N D L E   B R A N C H   N O D E S       
        // --------------------------------------------------------------------
        else
        {
            BranchNode bnode = ( BranchNode ) node;

            switch ( bnode.getOperator() )
            {
                case ( BranchNode.AND  ):
                    count = getConjunctionScan( bnode );
                    break;
                case ( BranchNode.NOT  ):
                    count = getNegationScan( bnode );
                    break;
                case ( BranchNode.OR  ):
                    count = getDisjunctionScan( bnode );
                    break;
                default:
                    throw new IllegalArgumentException( "Unrecognized branch node type" );
            }
        }

        // Protect against overflow when counting.
        if ( count.compareTo( BigInteger.ZERO ) < 0 )
        {
            count = MAX;
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
    private BigInteger getConjunctionScan( BranchNode node ) throws NamingException
    {
        BigInteger count = BigInteger.valueOf( Integer.MAX_VALUE );
        ArrayList children = node.getChildren();

        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = ( ExprNode ) children.get( ii );
            annotate( child );
            count = ( ( BigInteger ) child.get( "count" ) ).min( count );
        }

        return count;
    }


    /**
     * Negation counts are estimated in one of two ways depending on its 
     * composition.  If the sole child of the negation is a leaf and an index
     * exists for the attribute of the leaf then the count on the index is taken
     * as the scan count.  If the child is a branch node then the count of the
     * negation node is set to the total count of entries in the master table.
     * This last resort tactic is used to get a rough estimate because it would 
     * cost too much to get an exact estimate on the count of a negation on a
     * branch node.
     *
     * @param node the negation node
     * @return the scan count
     * @throws NamingException if there is an error
     */
    private BigInteger getNegationScan( BranchNode node ) throws NamingException
    {
        ExprNode onlyChild = ( ExprNode ) node.getChildren().get( 0 );

        annotate( onlyChild );

        if ( onlyChild.isLeaf() && !( onlyChild instanceof ScopeNode ) && !( onlyChild instanceof AssertionNode )
            && !( onlyChild instanceof PresenceNode ) )
        {
            LeafNode leaf = ( LeafNode ) onlyChild;
            Index idx = db.getUserIndex( leaf.getAttribute() );
            return BigInteger.valueOf( idx.count() );
        }

        return BigInteger.valueOf( db.count() );
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
    private BigInteger getDisjunctionScan( BranchNode node ) throws NamingException
    {
        ArrayList children = node.getChildren();
        BigInteger total = BigInteger.ZERO;

        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = ( ExprNode ) children.get( ii );
            annotate( child );
            total = total.add( ( BigInteger ) child.get( "count" ) );
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
    private BigInteger getEqualityScan( SimpleNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            return BigInteger.valueOf( idx.count( node.getValue() ) );
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return MAX;
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
    private BigInteger getGreaterLessScan( SimpleNode node, boolean isGreaterThan ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            int count = idx.count( node.getValue(), isGreaterThan );
            return BigInteger.valueOf( count );
        }

        // count for non-indexed attribute is unknown so we presume da worst
        return MAX;
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
    private BigInteger getFullScan( LeafNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getUserIndex( node.getAttribute() );
            int count = idx.count();
            return BigInteger.valueOf( count );
        }

        return MAX;
    }


    /**
     * Gets the number of entries that would be returned by a presence node
     * assertion.  Leverages the existance system index for scan counts.
     *
     * @param node the presence node
     * @return the number of entries matched for the presence of an attribute
     * @throws NamingException if errors result
     */
    private BigInteger getPresenceScan( PresenceNode node ) throws NamingException
    {
        if ( db.hasUserIndexOn( node.getAttribute() ) )
        {
            Index idx = db.getExistanceIndex();
            int count = idx.count( node.getAttribute() );
            return BigInteger.valueOf( count );
        }

        return MAX;
    }


    /**
     * Gets the scan count for the scope node attached to this filter.
     *
     * @param node the ScopeNode
     * @return the scan count for scope
     * @throws NamingException if any errors result
     */
    private BigInteger getScopeScan( ScopeNode node ) throws NamingException
    {
        switch ( node.getScope() )
        {
            case ( SearchControls.OBJECT_SCOPE  ):
                return BigInteger.ONE;
            case ( SearchControls.ONELEVEL_SCOPE  ):
                BigInteger id = db.getEntryId( node.getBaseDn() );
                return BigInteger.valueOf( db.getChildCount( id ) );
            case ( SearchControls.SUBTREE_SCOPE  ):
                return BigInteger.valueOf( db.count() );
            default:
                throw new IllegalArgumentException( "Unrecognized search scope " + "value for filter scope node" );
        }
    }
}
