/*
 * $Id: SchemaManager.java,v 1.3 2003/03/13 18:28:08 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


import javax.naming.NamingException ;
import javax.naming.InvalidNameException ;
import javax.naming.NameNotFoundException ;


/**
 * Manages schemas based on namespace.
 */
public interface SchemaManager
{
	public static final String ROLE = SchemaManager.class.getName() ;

    Schema getSchema(String a_dn)
        throws NamingException ;
    Schema getCompleteSchema() ;
}
