/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.filter;


import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * A candidacy predicate which tests if an entry satisfies some condition before
 * being returned by a search.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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
