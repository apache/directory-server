/*
 * $Id: Assertion.java,v 1.2 2003/03/13 18:27:28 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.search ;


import javax.naming.NamingException ;
import org.apache.eve.backend.BackendException ;


public interface Assertion
{
    /**
     * Checks to see if a candidate is valid by evaluating an assertion
     * against the candidate.
     * 
     * @param a_candidate the candidate to assert
     * @return true if the candidate is valid, false otherwise
     */
    boolean assertCandidate(Object a_candidate)
        throws BackendException, NamingException ;
}
