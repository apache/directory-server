/*
 * $Id: Normalizer.java,v 1.4 2003/03/13 18:28:02 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;
import javax.naming.NamingException ;


/**
 * Equality matching rule attribute value normalizer.
 */
public interface Normalizer
{
    String getEqualityMatch() ;
    String normalize(String a_value)
        throws NamingException ;
}
