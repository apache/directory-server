/*
 * $Id: SearchEngine.java,v 1.5.4.1 2003/10/01 03:37:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import java.util.Iterator ;
import java.util.ArrayList ;
import java.math.BigInteger ;
import javax.naming.NamingException ;


import org.apache.eve.schema.Schema ;
import org.apache.eve.backend.Cursor ;
import org.apache.eve.backend.Backend ;
import org.apache.ldap.common.filter.ExprNode ;
import org.apache.ldap.common.filter.LeafNode ;
import org.apache.ldap.common.filter.ScopeNode ;
import org.apache.ldap.common.filter.SimpleNode ;
import org.apache.ldap.common.filter.BranchNode ;
import org.apache.ldap.common.filter.PresenceNode ;
import org.apache.ldap.common.filter.SubstringNode ;
import org.apache.eve.backend.jdbm.Database ;
import org.apache.ldap.common.NotImplementedException ;
import org.apache.ldap.common.message.DerefAliasesEnum;
import org.apache.eve.backend.BackendException ;
import org.apache.eve.backend.jdbm.index.Index ;
import org.apache.eve.backend.jdbm.index.IndexRecord ;


import org.apache.avalon.framework.logger.AbstractLogEnabled ;

import org.apache.regexp.RE ;
import org.apache.regexp.RESyntaxException ;


/**
 * Given a search filter and a scope the search engine identifies valid
 * candidate entries returning their ids.
 */
public class SearchEngine
    extends AbstractLogEnabled
{
    private Optimizer m_optimizer = new DefaultOptimizer() ;


	public Optimizer getOptimizer()
    {
        return m_optimizer ;
    }


    public ExprNode addScopeNode(ExprNode a_node, String a_baseDn, int a_scope)
    {
        ScopeNode l_scopeNode = new ScopeNode(
            DerefAliasesEnum.NEVERDEREFALIASES, a_baseDn, a_scope) ;
        BranchNode l_top = new BranchNode(BranchNode.AND) ;
        l_top.getChildren().add(l_scopeNode) ;
        l_top.getChildren().add(a_node) ;
        return l_top ;
    }


    public Cursor search(Database a_db,
        ExprNode a_filter,
        String a_baseDn,
        int a_scope)
        throws BackendException, NamingException
    {
        Cursor l_cursor = null ;
        ExprNode l_root = addScopeNode(a_filter, a_baseDn, a_scope) ;
        m_optimizer.annotate(a_db, l_root) ;
        l_cursor = buildSearchCursor(a_db, l_root) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    ///////////////////////////////////////////////////////////////
    //     C U R S O R   B U I L D I N G   F U N C T I O N S     //
    ///////////////////////////////////////////////////////////////


    public Cursor buildSearchCursor(final Database a_db, final ExprNode a_node)
        throws BackendException, NamingException
    {
        Cursor l_cursor = null ;

        if ( a_node instanceof ScopeNode )
        {
            return buildScopeCursor( a_db, ( ScopeNode ) a_node ) ;
        }

        if(a_node.isLeaf()) {
            LeafNode l_leaf = (LeafNode) a_node ;

            switch(l_leaf.getAssertionType()) {
            case(LeafNode.APPROXIMATE):
                l_cursor = a_db.getIndexCursor(l_leaf.getAttribute(),
                    ((SimpleNode)l_leaf).getValue()) ;
                break ;
            case(LeafNode.EQUALITY):
                l_cursor = a_db.getIndexCursor(l_leaf.getAttribute(),
                    ((SimpleNode)l_leaf).getValue()) ;
                break ;
            case(LeafNode.EXTENSIBLE):
                // N O T   I M P L E M E N T E D   Y E T !
                throw new NotImplementedException() ;
                //break ;
            case(LeafNode.GREATEREQ):
                l_cursor = a_db.getIndexCursor(l_leaf.getAttribute(),
                    ((SimpleNode)l_leaf).getValue(), true) ;
                break ;
            case(LeafNode.LESSEQ):
                l_cursor = a_db.getIndexCursor(l_leaf.getAttribute(),
                    ((SimpleNode)l_leaf).getValue(), false) ;
                break ;
            case(LeafNode.PRESENCE):
                l_cursor = a_db.getIndexCursor(Schema.EXISTANCE_ATTR,
                    l_leaf.getAttribute().toLowerCase()) ;
                break ;
            case(LeafNode.SUBSTRING):
                l_cursor = buildSubstringCursor(a_db, (SubstringNode) l_leaf) ;
                break ;
            default:
                throw new IllegalArgumentException("Unknown leaf assertion") ;
            }
        }
        else if (a_node instanceof ScopeNode )
        {
            l_cursor = buildScopeCursor(a_db, (ScopeNode) a_node) ;
        }
        else
        {
            BranchNode l_branch = (BranchNode) a_node ;
            switch(l_branch.getOperator()) {
            case(BranchNode.AND):
                l_cursor = buildConjunctionCursor(a_db, l_branch) ;
                break ;
            case(BranchNode.NOT):
                l_cursor = buildNegationCursor(a_db, l_branch) ;
                break ;
            case(BranchNode.OR):
                l_cursor = buildDisjunctionCursor(a_db, l_branch) ;
                break ;
            default:
                throw new IllegalArgumentException("Unknown branch operator") ;
            }
        }

        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    public Cursor buildScopeCursor(final Database a_db, final ScopeNode a_node)
        throws BackendException, NamingException
	{
        switch(a_node.getScope()) {
        case(Backend.BASE_SCOPE):
            final BigInteger l_id = a_db.getEntryId(a_node.getBaseDn()) ;
            final IndexRecord l_record =
                new IndexRecord() ;
            l_record.setEntryId(l_id) ;
            l_record.setIndexKey(a_node.getBaseDn()) ;

            return new Cursor() {
                public Object advance()
                    throws NamingException
                { super.close() ; return l_record ; }
                public void freeResources() { }
                public boolean canAdvance()
                { return(!super.isClosed()) ; }
            } ;
        case(Backend.SINGLE_SCOPE):
            return a_db.getChildren(a_db.getEntryId(a_node.getBaseDn())) ;
        case(Backend.SUBTREE_SCOPE):
            Assertion l_assertion = new Assertion()
            {
                public boolean assertCandidate(Object a_candidate)
                    throws BackendException
                {
                    IndexRecord l_rec = (IndexRecord) a_candidate ;
                    String l_dn = a_db.getEntryDn(l_rec.getEntryId()) ;
                    return l_dn.endsWith(a_node.getBaseDn()) ;
                }
            } ;

            // Gets a cursor over all elements
            Cursor l_cursor = a_db.getIndexCursor(Schema.DN_ATTR) ;
            return new PrefetchCursor(l_cursor, l_assertion) ;
        default:
            throw new BackendException("Unrecognized search scope!") ;
        }
    }


    public Cursor buildSubstringCursor(final Database a_db,
        final SubstringNode a_node)
        throws BackendException, NamingException
	{
        Cursor l_cursor = null ;
        RE l_regex = null ;

        try {
            l_regex = a_node.getRegex() ;
        } catch(RESyntaxException e) {
            throw new BackendException("SubstringNode '" + a_node + "' had "
                + "encountered syntax exception: " + e.getMessage(), e) ;
        }

        if(a_node.getInitial() != null) {
	        l_cursor = a_db.getIndexCursor(a_node.getAttribute(), l_regex,
                a_node.getInitial()) ;
	    } else {
            l_cursor = a_db.getIndexCursor(a_node.getAttribute(), l_regex) ;
        }

        return l_cursor ;
    }


    /**
     * Method involved in chain recursion while constructing the search cursor
     * used to handle the Disjunction expression case.
     */
    public Cursor buildDisjunctionCursor(final Database a_db,
        final BranchNode a_node)
        throws BackendException, NamingException
    {
        Cursor l_cursor = null ;
        ArrayList l_list = a_node.getChildren() ;
        Cursor [] l_childCursors = new Cursor [l_list.size()] ;

        // Recursively create Cursors for each of the child expression nodes.
        for(int ii = 0 ; ii < l_childCursors.length; ii++) {
            l_childCursors[ii] =
                buildSearchCursor(a_db, (ExprNode) l_list.get(ii)) ;
        }

        // Create the Cursor enable logging on it and return.
        l_cursor = new DisjunctionCursor(l_childCursors) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    /**
     * Method involved in chain recursion while constructing the search cursor
     * used to handle the Negation expression case.
     */
    public Cursor buildNegationCursor(final Database a_db,
        final BranchNode a_node)
        throws BackendException, NamingException
    {
        Cursor l_childCursor = null ;
        Cursor l_cursor = null ;
        final ExprNode l_childNode =
            (ExprNode) a_node.getChildren().get(0) ;

        // Iterates over entire set of index values.
        if(l_childNode.isLeaf()) {
            LeafNode l_child = (LeafNode) l_childNode ;
            l_childCursor = a_db.getIndexCursor(l_child.getAttribute()) ;
        } else { // Iterates over the entire set of entries.
            l_childCursor = a_db.getIndexCursor(Schema.DN_ATTR) ;
        }

        l_childCursor.enableLogging(getLogger()) ;

        Assertion l_assertion = new Assertion()
        {
            public boolean assertCandidate(Object a_candidate)
                throws BackendException, NamingException
            {
                IndexRecord l_rec = (IndexRecord) a_candidate ;
                // NOTICE THE ! HERE
                // The candidate is valid if it does not pass assertion. A
                // candidate that passes assertion is therefore invalid.
                return !assertExpression(a_db, l_childNode, l_rec.getEntryId()) ;
            }
        } ;
        l_cursor = new PrefetchCursor(l_childCursor, l_assertion, true) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    /**
     * Method involved in chain recursion while constructing the search cursor
     * used to handle the Conjunction expression case.
     */
    public Cursor buildConjunctionCursor(final Database a_db,
        final BranchNode a_node)
        throws BackendException, NamingException
    {
        int l_minIndex = 0 ;
        int l_minValue = Integer.MAX_VALUE ;
        int l_value = Integer.MAX_VALUE ;
        ExprNode l_node = null ;
        Cursor l_cursor = null ;

        // We scan the child nodes of a branch node searching for the child
        // expression node with the smallest scan count.  This is the child
        // we will use for iteration by creating a cursor over its expression.
        final ArrayList l_list = a_node.getChildren() ;
        for(int ii = 0 ; ii < l_list.size(); ii++) {
            l_node = (ExprNode) l_list.get(ii) ;
            l_value = ((BigInteger) l_node.get("count")).intValue() ;
            l_minValue = Math.min(l_minValue, l_value) ;

            if(l_minValue == l_value) {
                l_minIndex = ii ;
            }
        }

        // Once found we construct the child cursor and the conjunction cursor.
        final ExprNode l_cursorNode = (ExprNode) l_list.get(l_minIndex) ;

        Assertion l_assertion = new Assertion()
        {
            public boolean assertCandidate(Object a_candidate)
                throws BackendException, NamingException
            {
                IndexRecord l_rec = (IndexRecord) a_candidate ;

                for(int ii = 0 ; ii < l_list.size(); ii++) {
                    ExprNode l_child = (ExprNode) l_list.get(ii) ;

                    if(l_child == l_cursorNode) {
                        continue ;
                    } else if(!assertExpression(a_db, l_child,
                        l_rec.getEntryId())) {
                        return false ;
                    }
                }

                return true ;
            }
        } ;

        Cursor l_childCursor = buildSearchCursor(a_db, l_cursorNode) ;
        l_cursor = new PrefetchCursor(l_childCursor, l_assertion) ;
        l_cursor.enableLogging(getLogger()) ;
        return l_cursor ;
    }


    /////////////////////////////////////////////////////////////////////
    //   E X P R E S S I O N   A S S E R T I O N   F U N C T I O N S   //
    /////////////////////////////////////////////////////////////////////


    public boolean assertExpression(final Database a_db, final ExprNode a_node,
        final BigInteger a_id)
        throws BackendException, NamingException
    {
        if (a_node instanceof ScopeNode)
        {
            return assertScope( a_db, (ScopeNode) a_node, a_id ) ;
        }
        if(a_node.isLeaf()) {
            return assertLeaf(a_db, (LeafNode) a_node, a_id) ;
        }

        return assertBranch(a_db, (BranchNode) a_node, a_id) ;
    }


    public boolean assertBranch(final Database a_db, final BranchNode a_node,
        final BigInteger a_id)
        throws BackendException, NamingException
    {
        switch(a_node.getOperator()) {
        case(BranchNode.OR):
            Iterator l_children = a_node.getChildren().iterator() ;
            while(l_children.hasNext()) {
                ExprNode l_child = (ExprNode) l_children.next() ;
                if(assertExpression(a_db, l_child, a_id)) {
                    return true ;
                }
            }

            return false ;
        case(BranchNode.AND):
            l_children = a_node.getChildren().iterator() ;
            while(l_children.hasNext()) {
                ExprNode l_child = (ExprNode) l_children.next() ;
                if(!assertExpression(a_db, l_child, a_id)) {
                    return false ;
                }
            }

            return true ;
        case(BranchNode.NOT):
            ArrayList l_childArray = a_node.getChildren() ;
            if(l_childArray.size() > 0) {
                return !assertExpression(a_db,
                    (ExprNode) l_childArray.get(0), a_id) ;
            }

            throw new BackendException("Negation has no child: " + a_node) ;
        default:
            throw new BackendException("Unrecognized branch node operator: "
                + a_node.getOperator()) ;
        }
    }


    public boolean assertLeaf(final Database a_db, final LeafNode a_node,
        final BigInteger a_id)
        throws BackendException, NamingException
    {
        switch(a_node.getAssertionType()) {
        case(LeafNode.APPROXIMATE):
            return a_db.assertIndexValue(((SimpleNode) a_node).getAttribute(),
                ((SimpleNode) a_node).getValue(), a_id) ;
        case(LeafNode.EQUALITY):
            return a_db.assertIndexValue(((SimpleNode) a_node).getAttribute(),
                ((SimpleNode) a_node).getValue(), a_id) ;
        case(LeafNode.EXTENSIBLE):
            throw new NotImplementedException() ;
        case(LeafNode.GREATEREQ):
            return a_db.assertIndexValue(((SimpleNode) a_node).getAttribute(),
                ((SimpleNode) a_node).getValue(), a_id, true) ;
        case(LeafNode.LESSEQ):
            return a_db.assertIndexValue(((SimpleNode) a_node).getAttribute(),
                ((SimpleNode) a_node).getValue(), a_id, false) ;
        case(LeafNode.PRESENCE):
            return a_db.assertIndexValue(Schema.EXISTANCE_ATTR,
                ((PresenceNode) a_node).getAttribute().toLowerCase(), a_id) ;
        case(LeafNode.SUBSTRING):
            return assertSubstring(a_db, (SubstringNode) a_node, a_id) ;
        default:
            throw new BackendException("Unrecognized leaf node type: "
                + a_node.getAssertionType()) ;
        }
    }


    public boolean assertScope(final Database a_db, final ScopeNode a_node,
        final BigInteger a_id)
        throws BackendException, NamingException
    {
        String l_dn = a_db.getEntryDn(a_id) ;

        switch(a_node.getScope()) {
        case(Backend.BASE_SCOPE):
            return l_dn.equals(a_node.getBaseDn()) ;
        case(Backend.SINGLE_SCOPE):
            Object l_key = a_db.getEntryId(a_node.getBaseDn()) ;
            return a_db.assertIndexValue(Schema.HIERARCHY_ATTR, l_key, a_id) ;
        case(Backend.SUBTREE_SCOPE):
            return l_dn.endsWith(a_node.getBaseDn()) ;
        default:
            throw new BackendException("Unrecognized search scope!") ;
        }
    }


    public boolean assertSubstring(final Database a_db,
        final SubstringNode a_node,
        final BigInteger a_id)
        throws BackendException, NamingException
    {
        RE l_regex = null ;
        Index l_index = a_db.getIndex(a_node.getAttribute()) ;
        Cursor l_cursor = l_index.getReverseCursor(a_id) ;

        try {
            l_regex = a_node.getRegex() ;
        } catch(RESyntaxException e) {
            throw new BackendException("SubstringNode '" + a_node + "' had "
                + "encountered syntax exception: " + e.getMessage(), e) ;
        }

        while(l_cursor.hasMore()) {
            IndexRecord l_rec = (IndexRecord) l_cursor.next() ;
            if(l_regex.match((String) l_rec.getIndexKey())) {
                l_cursor.close() ;
                return true ;
            }
        }

        return false ;
    }
}
