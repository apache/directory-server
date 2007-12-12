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


import java.util.Hashtable;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.unit.AbstractServerTest;


/**
 * Testcase with different search operations on the cn=schema entry. 
 * Created to demonstrate DIRSERVER-1055
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 569048 $
 */
public class SchemaSearchITest extends AbstractServerTest
{
    private LdapContext ctx = null;
    private static final String DN = "cn=schema";
    private static final String FILTER = "(objectclass=subschema)";


    protected void checkForAttributes( Attributes attrs, String[] attrNames )
    {
        for ( int i = 0; i < attrNames.length; i++ )
        {
            String attrName = attrNames[i];

            assertNotNull( "Check if attr " + attrName + " is present", attrs.get( attrNames[i] ) );
        }
    }


    /**
     * Create context and a person entry.
     */
    public void setUp() throws Exception
    {
        super.setUp();

        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + port );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( env, null );
        assertNotNull( ctx );
    }


    /**
     * Remove person entry and close context.
     */
    public void tearDown() throws Exception
    {
        ctx.close();

        ctx = null;
        super.tearDown();
    }


    /**
     * Check if modifyTimestamp and createTimestamp are present in the search result,
     * if they are requested.
     */
    public void testRequestOperationalAttributes() throws NamingException
    {
        SearchControls ctls = new SearchControls();

        String[] attrNames =
            { "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" };

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( attrNames );

        NamingEnumeration result = ctx.search( DN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = ( SearchResult ) result.next();
            checkForAttributes( entry.getAttributes(), attrNames );
        }
        else
        {
            fail( "entry " + DN + " not found" );
        }

        result.close();
    }


    /**
     * Check if modifyTimestamp and createTimestamp are present in the search result,
     * if + is requested.
     */
    public void testRequestAllOperationalAttributes() throws NamingException
    {
        SearchControls ctls = new SearchControls();

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "+" } );

        NamingEnumeration result = ctx.search( DN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = ( SearchResult ) result.next();
            String[] attrNames =
                { "creatorsName", "createTimestamp", "modifiersName", "modifyTimestamp" };
            checkForAttributes( entry.getAttributes(), attrNames );
        }
        else
        {
            fail( "entry " + DN + " not found" );
        }

        result.close();
    }

}
