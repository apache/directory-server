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


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.model.ldif.LdifUtils;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests the creation of contexts in various ways.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class )
@CreateDS(name = "CreateContextIT")
public class CreateContextIT extends AbstractLdapTestUnit
{
    protected Attributes getPersonAttributes( String sn, String cn ) throws Exception
    {
        Attributes attrs = LdifUtils.createAttributes(
                "objectClass: top",
                "objectClass: person",
                "cn", cn,
                "sn", sn);

        return attrs;
    }


    /**
     * DIRSERVER-628: Creation of entry with multivalued Rdn leads to wrong
     * attribute value.
     *
     * @throws NamingException on error
     */
    @Test
    public void testMultiValuedRdn() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush+sn=Bush";
        sysRoot.createSubcontext( rdn, attrs );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = sysRoot.search( base, filter, sctls );
        
        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
        }

        sysRoot.destroySubcontext( rdn );
    }


    /**
     * Tests the creation and subsequent read of a new JNDI context under the
     * system context root.
     *
     * @throws javax.naming.NamingException if there are failures
     */
    @Test
    public void testCreateContexts() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );
        DirContext ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing01,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing01" );
        ctx = sysRoot.createSubcontext( "ou=testing01", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing01", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing02,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing02" );
        ctx = sysRoot.createSubcontext( "ou=testing02", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing02", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );

        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "subtest" );
        ctx = ctx.createSubcontext( "ou=subtest", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    @Test
    public void testFailCreateExisting() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attribute attribute;
        Attributes attributes;
        DirContext ctx = null;

        /*
         * create ou=testing00,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );
        ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing00" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing00", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * fail on recreate attempt for ou=testing00,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing00" );

        ctx = null;
        try
        {
            ctx = sysRoot.createSubcontext( "ou=testing00", attributes );
            fail( "Attempt to create exiting context should fail!" );
        }
        catch ( NamingException e )
        {
            assertNotNull( e );
        }

        assertNull( ctx );
    }
    
    
    @Test
    public void testCreateContextWithCompositeName() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );
        Attribute objclass = new BasicAttribute( "objectClass" );
        objclass.add( "top" );
        objclass.add( "organizationalUnit" );
        attrs.put( objclass );

        Name relativeName = new CompositeName( "ou=services" );

        //sysRoot.createSubcontext(relativeName.toString(), attrs);//Passes!
        sysRoot.createSubcontext( relativeName, attrs );//Fails!
    }


    /**
     * Tests the creation and subsequent read of a new JNDI context under the
     * system context root.
     *
     * @throws javax.naming.NamingException if there are failures
     */
    @Test
    public void testCreateContextWithBasicAttributesCaseSensitive() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        /*
         * create ou=testing00,ou=system
         */
        Attributes attributes = new BasicAttributes( true );
        attributes.put("objectClass", "organizationalUnit");
        attributes.put("description", "Test OU");
        attributes.put("OU", "Test");
        
        DirContext ctx = sysRoot.createSubcontext( "ou=Test", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=Test" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "Test", attributes.get( "ou" ).get() );
        assertEquals( "Test OU", attributes.get( "Description" ).get() );
        Attribute attribute = attributes.get( "objectclass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing01,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing01" );
        ctx = sysRoot.createSubcontext( "ou=testing01", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing01", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=testing02,ou=system
         */
        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "testing02" );
        ctx = sysRoot.createSubcontext( "ou=testing02", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=testing02" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "testing02", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );

        /*
         * create ou=subtest,ou=testing01,ou=system
         */
        ctx = ( DirContext ) sysRoot.lookup( "ou=testing01" );

        attributes = new BasicAttributes( true );
        attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "organizationalUnit" );
        attributes.put( attribute );
        attributes.put( "ou", "subtest" );
        ctx = ctx.createSubcontext( "ou=subtest", attributes );
        assertNotNull( ctx );

        ctx = ( DirContext ) sysRoot.lookup( "ou=subtest,ou=testing01" );
        assertNotNull( ctx );

        attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        assertEquals( "subtest", attributes.get( "ou" ).get() );
        attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "organizationalUnit" ) );
    }


    @Test
    public void testCreateContextWithNoObjectClass() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        Attributes attrs = new BasicAttributes( true );

        try
        {
            sysRoot.createSubcontext( "ou=subtest", attrs );// should Fails!
            fail( "It is not allowed to create a context with a bad entry");
        }
        catch ( NamingException e )
        {
            assertNotNull( e );
        }
    }


    @Test
    public void testCreateJavaContainer() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );
        
        DirContext ctx = (DirContext)sysRoot.createSubcontext( "cn=subtest" );
        assertNotNull( ctx );
        
        Attributes attributes = ctx.getAttributes( "" );
        assertNotNull( attributes );
        
        assertEquals( "subtest", attributes.get( "cn" ).get() );
        Attribute attribute = attributes.get( "objectClass" );
        assertNotNull( attribute );
        assertTrue( attribute.contains( "top" ) );
        assertTrue( attribute.contains( "javaContainer" ) );
    }


    @Test
    public void testCreateJavaContainerBadRDN() throws Exception
    {
        LdapContext sysRoot = getSystemContext( getService() );

        try
        {
            sysRoot.createSubcontext( "ou=subtest" );
            fail( "It is not allowed to create a context with a bad Rdn. CN is mandatory");
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
}
