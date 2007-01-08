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

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;


/**
 * Test case for all modify replace operations.
 * 
 * Testcase to demonstrate DIRSERVER-646 ("Replacing an unknown attribute with
 * no values (deletion) causes an error").
 */
public class ModifyReplaceITest extends AbstractServerTest
{
    DirContext ctx = null;


    protected Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );

        return attrs;
    }


    protected void setUp() throws Exception
    {
        super.setUp();
        
        Hashtable env = new Hashtable();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + super.port + "/ou=system" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        ctx = new InitialDirContext( env );
    }


    protected void tearDown() throws Exception
    {
        ctx.close();
        super.tearDown();
    }


    /**
     * Create a person entry and try to remove a not present attribute
     */
    public void testReplaceNotPresentAttribute() throws NamingException
    {
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        Attribute attr = new AttributeImpl( "description" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );

        ctx.modifyAttributes( rdn, new ModificationItemImpl[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration enm = ctx.search( base, filter, sctls );
        while ( enm.hasMore() )
        {
            SearchResult sr = ( SearchResult ) enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry and try to remove a non existing attribute
     */
    public void testReplaceNonExistingAttribute() throws NamingException
    {
        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush";
        ctx.createSubcontext( rdn, attrs );

        Attribute attr = new AttributeImpl( "numberOfOctaves" );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, attr );

        ctx.modifyAttributes( rdn, new ModificationItemImpl[]
            { item } );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration enm = ctx.search( base, filter, sctls );
        while ( enm.hasMore() )
        {
            SearchResult sr = ( SearchResult ) enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        ctx.destroySubcontext( rdn );
    }
}
