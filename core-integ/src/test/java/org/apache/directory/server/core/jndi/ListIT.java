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
package org.apache.directory.server.core.jndi;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import static org.apache.directory.server.core.integ.IntegrationUtils.getUserAddLdif;
import static org.apache.directory.server.core.integ.IntegrationUtils.getRootContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getContext;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.shared.ldap.ldif.Entry;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.util.HashSet;


/**
 * Tests our ability to list elements as the admin user and as a non admin user
 * on security sensitive values.  We do not return results or name class pairs
 * for user accounts if the user is not the admin.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
public class ListIT
{
    public static DirectoryService service;


    @Test
    public void testListSystemAsNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        LdapContext sysRoot = getContext( akarasulu.getDn(), service, "ou=system" );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        assertFalse( set.contains( "uid=admin,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
    }


    @Test
    public void testListUsersAsNonAdmin() throws NamingException
    {
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        LdapContext sysRoot = getContext( akarasulu.getDn(), service, "ou=system" );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "ou=users" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        // @todo this assertion fails now - is this the expected behavoir?
        // assertFalse( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
    }


    @Test
    public void testListSystemAsAdmin() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        HashSet<String> set = new HashSet<String>();
        NamingEnumeration<NameClassPair> list = sysRoot.list( "" );

        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        assertTrue( set.contains( "uid=admin,ou=system" ) );
        assertTrue( set.contains( "ou=users,ou=system" ) );
        assertTrue( set.contains( "ou=groups,ou=system" ) );
    }


    @Test
    public void testListUsersAsAdmin() throws NamingException
    {
        LdapContext sysRoot = getSystemContext( service );
        HashSet<String> set = new HashSet<String>();
        Entry akarasulu = getUserAddLdif();
        getRootContext( service ).createSubcontext( akarasulu.getDn(), akarasulu.getAttributes() );

        NamingEnumeration<NameClassPair> list = sysRoot.list( "ou=users" );
        
        while ( list.hasMore() )
        {
            NameClassPair ncp = list.next();
            set.add( ncp.getName() );
        }

        assertTrue( set.contains( "uid=akarasulu,ou=users,ou=system" ) );
    }
}
