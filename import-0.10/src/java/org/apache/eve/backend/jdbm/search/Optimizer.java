/*
 * $Id: Optimizer.java,v 1.2 2003/03/13 18:27:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import javax.naming.NamingException ;

import org.apache.ldap.common.filter.ExprNode ;
import org.apache.eve.backend.jdbm.Database ;
import org.apache.eve.backend.BackendException ;


/**
 * An optimizer applies heuristics to determine best execution path to a search
 * filter based on scan counts within database indices.  It annotates the nodes
 * of an expression subtree by setting a "count" key in the node.  Its goal is
 * to annotate nodes with counts to indicate which nodes to iterate over thereby
 * minimizing the number cycles in a search.  The SearchEngine relies on these
 * count markers to determine the appropriate path.
 */
public interface Optimizer
{
    public void annotate(Database a_db, ExprNode a_node)
        throws BackendException, NamingException ;
}
