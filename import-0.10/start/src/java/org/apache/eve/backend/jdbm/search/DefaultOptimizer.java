/*
 * $Id: DefaultOptimizer.java,v 1.2 2003/03/13 18:27:28 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import java.util.ArrayList ;
import java.math.BigInteger ;
import javax.naming.NamingException ;

import org.apache.eve.schema.Schema ;
import org.apache.eve.backend.Backend ;
import org.apache.eve.backend.jdbm.Database ;
import org.apache.eve.backend.BackendException ;

import org.apache.ldap.common.filter.ExprNode ;
import org.apache.ldap.common.filter.LeafNode ;
import org.apache.ldap.common.filter.ScopeNode ;
import org.apache.ldap.common.filter.SimpleNode ;
import org.apache.ldap.common.filter.BranchNode ;
import org.apache.ldap.common.filter.PresenceNode ;


/**
 *
 */
public class DefaultOptimizer
    implements Optimizer
{
    public static final BigInteger MAX = BigInteger.valueOf(Integer.MAX_VALUE) ;

    /**
     * Annotates the expression tree to determine optimal evaluation order based
     * on the scan count for indices that exist for each expression node.  If an
     * index on the attribute does not exist an IndexNotFoundException will be
     * thrown.
     */
    public void annotate(Database a_db, ExprNode a_node)
        throws BackendException, NamingException
    {
        // Start off with the worst case unless scan count say otherwise.
        BigInteger l_count = MAX ;

        if ( a_node instanceof ScopeNode )
        {
            ScopeNode l_scopeNode = (ScopeNode) a_node ;

            if(l_scopeNode.getScope() == Backend.BASE_SCOPE) {
                l_count = BigInteger.ONE ;
            } else if(l_scopeNode.getScope() == Backend.SINGLE_SCOPE) {
                BigInteger l_id = a_db.getEntryId(l_scopeNode.getBaseDn()) ;
                l_count = BigInteger.valueOf(a_db.getChildCount(l_id)) ;
            } else if(l_scopeNode.getScope() == Backend.SUBTREE_SCOPE) {
                l_count = BigInteger.valueOf(a_db.count()) ;
            }
        }

            //////////////////////////////////////////////////////////
            //           H A N D L E   L E A F   N O D E S          //
            //////////////////////////////////////////////////////////

        // Each assertion leaf node is on an attribute.  We ask the index on
        // that attribute for the scan count representing all the candidates
        // that would satisfy the assertion.
        else if(a_node.isLeaf()) {
            LeafNode l_leaf = (LeafNode) a_node ;
            switch(l_leaf.getAssertionType()) {
            case(LeafNode.APPROXIMATE):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute(),
                    ((SimpleNode) l_leaf).getValue())) ;
                break ;
            case(LeafNode.EQUALITY):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute(),
                    ((SimpleNode) l_leaf).getValue())) ;
                break ;
            case(LeafNode.EXTENSIBLE):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute())) ;
                break ;
            case(LeafNode.GREATEREQ):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute(),
                    ((SimpleNode) l_leaf).getValue(), true)) ;
                break ;
            case(LeafNode.LESSEQ):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute(),
                    ((SimpleNode) l_leaf).getValue(), false)) ;
                break ;
            case(LeafNode.PRESENCE):
                PresenceNode l_presenceNode = (PresenceNode) a_node ;
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    Schema.EXISTANCE_ATTR,
                    l_presenceNode.getAttribute())) ;
                break ;
            case(LeafNode.SUBSTRING):
                l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                    l_leaf.getAttribute())) ;
                break ;
            default:
                throw new IllegalArgumentException("Unrecognized leaf node") ;
            }

            //////////////////////////////////////////////////////////
            //         H A N D L E   B R A N C H   N O D E S        //
            //////////////////////////////////////////////////////////

        } else {
            ArrayList l_children = null ;
            BranchNode l_node = (BranchNode) a_node ;

            switch(l_node.getOperator()) {
            case(BranchNode.AND):
                //
                // ANDs or Conjunctions take the count of the smallest child
                // as their count.  This is the best that a conjunction can do
                // and should be used rather than the worst case. Notice that
                // we annotate the child node with a recursive call before
                // accessing its count parameter.
                //
                l_count = BigInteger.valueOf(Integer.MAX_VALUE) ;
                l_children = l_node.getChildren() ;
                for(int ii = 0; ii < l_children.size(); ii++) {
                    ExprNode l_child = (ExprNode) l_children.get(ii) ;
                    annotate(a_db, l_child) ;
                    l_count =((BigInteger) l_child.get("count")).min(l_count) ;
                }
                break ;
            case(BranchNode.NOT):
                //
                // Negation counts are estimated in two ways.  First if the
                // sole child of the negation is a leaf or simple assertion
                // on an existing index then the count of the negation is set
                // to the count of all records in the respective index table
                // as a very rough estimate of the worst possible case.
                //
                // If the child is a brach node then the count of the negation
                // node is set to the total count of entries in the master
                // table. This tactic is used to get a rough estimate since it
                // is very costly to estimate the count of a negation over
                // a compound expression.
                //
                ExprNode l_onlyChild = (ExprNode) l_node.getChildren().get(0) ;
                annotate(a_db, l_onlyChild) ;

                if(l_onlyChild.isLeaf()) {
                    LeafNode l_leaf = (LeafNode) l_onlyChild ;
                    String l_attribute = l_leaf.getAttribute() ;

                    if(l_leaf instanceof PresenceNode) {
                        l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                            Schema.EXISTANCE_ATTR)) ;
                    } else {
                        l_count = BigInteger.valueOf(a_db.getIndexScanCount(
                            l_attribute)) ;
                    }
                } else {
                    l_count = BigInteger.valueOf(a_db.count()) ;
                }

                break ;
            case(BranchNode.OR):
                //
                // Disjunctions (OR) are the union of candidates across all
                // subexpressions for this reason we accumulate the counts of
                // all the child nodes. Notice that we annotate the child node
                // with a recursive call before accessing its count parameter.
                //
                l_children = l_node.getChildren() ;
                BigInteger l_total = BigInteger.ZERO ;
                for(int ii = 0; ii < l_children.size(); ii++) {
                    ExprNode l_child = (ExprNode) l_children.get(ii) ;
                    annotate(a_db, l_child) ;
                    l_total = l_total.add((BigInteger) l_child.get("count")) ;
                }

                l_count = l_total ;
                break ;
            default:
                throw new IllegalArgumentException("Unrecognized branch node") ;
            }
        }

        // Protect against overflow when counting.
        if(l_count.compareTo(BigInteger.ZERO) < 0) {
            l_count = MAX ;
        }

        a_node.set("count", l_count) ;
    }
}
