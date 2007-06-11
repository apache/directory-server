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
package org.apache.directory.server;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DITUtilitiesSP
{
	private static final Logger log = LoggerFactory.getLogger( DITUtilitiesSP.class );
	
	/**
     * Recursively deletes a subtree including the apex given.
     * 
     * If you do not want to wait for the developers to implement the
     * following RFC
     * http://kahuna.telstra.net/ietf/all-ids/draft-armijo-ldap-treedelete-02.txt
     * you can do it yourself!
     * 
     * @param ctx an LDAP context to perform operations on
     * @param rdn ctx relative name of the entry which is root of
     *        the subtree to be deleted
     * @throws NamingException
     */
    public static void deleteSubtree( LdapContext ctx, Name rdn ) throws NamingException
    {
        NamingEnumeration results = ctx.search( rdn, "(objectClass=*)", new SearchControls() );
        while ( results.hasMore() )
        {
            SearchResult result = ( SearchResult ) results.next();
            Name childRdn = new LdapDN( result.getName() );
            childRdn.remove( 0 );
            deleteSubtree( ctx, childRdn );
        }
        ctx.destroySubcontext( rdn );
        log.info( "Deleted: " + rdn );
    }
}
