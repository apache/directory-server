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


import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.unit.AbstractServerFastTest;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * Various del scenario tests.
 * 
 * The tree is :
 *  ou=system
 *   |
 *   +--> cn=test
 *   
 * Scenarios :
 * - try to delete a non existent entry
 * - try to delete a non valid entry
 * - try to delete an existent entry
 *   
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 433031 $
 */
public class FastDelITest extends AbstractServerFastTest
{
    protected static final String ldif = 
        "dn: cn=Test, ou=system\n" +
        "objectClass: top\n" +
        "objectClass: person\n" +
        "objectClass: organizationalPerson\n" +
        "objectClass: inetOrgPerson\n" +
        "cn: Test\n" +
        "sn: test user\n" +
        "description: a test for delete\n";


    /**
     * Performs a single level search from ou=system base and 
     * returns the set of DNs found.
     */
    private NamingEnumeration<SearchResult> search( String name ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        return ctx.search( name, "(objectclass=*)", controls );
    }
    
    /**
     * This method is called before each test, we don't really care if 
     * the data are loaded twice, we just don't care about the exception
     */
    @Before
    public void setUp() throws NamingException
    {
        try
        {
            importLdif( rootDSE, ldif );
        }
        catch ( NamingException ne )
        {
            // Do nothing
        }
    }
    
    /**
     * Try to delete an inexistant name.
     * 
     * WARNING !!! This test is valid ONLY because we have a double inexistant
     * RDN in the context. doing the same thing with a single RDN will miserabilly 
     * fail because of a gross misunderstanding of the RFC by SUN : JNDI is 
     * considering that the matchedDN may be used to modify the resultCode,
     * which is really a bad idea...
     * 
     * A bug report has been filled : ID 1074903
     */
    @Test public void testDeleteInvalidName() throws NamingException
    {
        try
        {
            ctx.destroySubcontext( "cn=This does not exist, cn=at all" );
            fail("deletion should fail");
        } 
        catch ( Exception e) 
        {
            assertTrue( e instanceof NameNotFoundException );
        }
    }

    /**
     * Try to delete an invalid name. 
     */
    @Test public void testDeleteNotExistantName() throws NamingException
    {
        try
        {
            ctx.destroySubcontext( "This does not exist" );
            fail("deletion should fail");
        } 
        catch ( Exception e) 
        {
            assertTrue( e instanceof InvalidNameException );
        }
    }

    /**
     * Try to delete an existing entry with invalid DN. We expect to receive a
     * LdapNameNotFoundException 
     */
    @Test public void testDeleteExistingName() 
    {
        try
        {
            ctx.destroySubcontext( "cn=test" );
        } 
        catch ( NamingException e) 
        {
            fail( "The deleton should have been successfull" );
        }

        try
        {
            NamingEnumeration<SearchResult> results = search( "cn=test, ou=system" );
            assertFalse( results.hasMore() );
        } 
        catch ( NamingException e) 
        {
            assertTrue( e instanceof NameNotFoundException );
        }
    }
}
