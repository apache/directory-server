package org.apache.eve.db;


import javax.naming.NamingException;

import org.apache.ldap.common.filter.ExprNode;


/**
 * An optimizer applies heuristics to determine best execution path to a search
 * filter based on scan counts within database indices.  It annotates the nodes
 * of an expression subtree by setting a "count" key in the node.  Its goal is
 * to annotate nodes with counts to indicate which nodes to iterate over thereby
 * minimizing the number cycles in a search.  The SearchEngine relies on these
 * count markers to determine the appropriate path.
 * 
 */
public interface Optimizer extends DatabaseEnabled
{
    /**
     * TODO Document me!
     *
     * @param node TODO
     * @throws NamingException TODO
     */
    void annotate( ExprNode node ) throws NamingException;
}
