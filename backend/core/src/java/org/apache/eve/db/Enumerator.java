package org.apache.eve.db;


import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;


/**
 * An enumeration builder or factory for filter expressions.
 * 
 */
public interface Enumerator extends DatabaseEnabled
{
    /**
     * Creates an enumeration to enumerate through the set of candidates 
     * satisfying a filter expression.
     * 
     * @param node a filter expression root
     * @return an enumeration over the 
     * @throws NamingException if database access fails
     */
    NamingEnumeration enumerate( ExprNode node ) throws NamingException;
}
