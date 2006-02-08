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

/*
 * $Id: Assertion.java,v 1.4 2003/10/15 01:59:57 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.filter;


import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * A candidacy predicate which tests if an entry satisfies some condition before
 * being returned by a search.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public interface Assertion
{
    /**
     * Checks to see if a candidate is valid by asserting an arbitrary predicate
     * against the candidate. Where available entry attributes will be provided
     * however there is no guarantee. The entry's attributes are only provided
     * if they were previously accessed. All assertions should handle cases
     * where the entry argument is null.
     * 
     * @param a_dn
     *            the normalized dn of the candidate entry to be tested
     * @param a_entry
     *            the entry's attributes if available
     * @return true if the candidate satisfies the predicate, false otherwise
     * @throws NamingException
     *             if an error occurs while asserting the predicate
     */
    boolean assertCandidate( String a_dn, Attributes a_entry ) throws NamingException;
}
