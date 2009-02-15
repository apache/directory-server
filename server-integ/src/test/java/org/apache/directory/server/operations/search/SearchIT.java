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
package org.apache.directory.server.operations.search;


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.message.control.SubentriesControl;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Testcase with different modify operations on a person entry. Each includes a
 * single add op only. Created to demonstrate DIREVE-241 ("Adding an already
 * existing attribute value with a modify operation does not cause an error.").
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 682556 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@ApplyLdifs( {
    
    // Entry # 0
    "dn: cn=Kate Bush,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: strongAuthenticationUser\n" +
    "objectClass: top\n" +
    "userCertificate:: NFZOXw==\n" +
    "cn: Kate Bush\n" +
    "description: this is a person\n" +
    "sn: Bush\n" +
    "jpegPhoto:: /9j/4AAQSkZJRgABAQEASABIAAD/4QAWRXhpZgAATU0AKgAAAAgAAAAAAAD//gAX\n" +
    " Q3JlYXRlZCB3aXRoIFRoZSBHSU1Q/9sAQwAQCwwODAoQDg0OEhEQExgoGhgWFhgxIyUdKDozPTw\n" +
    " 5Mzg3QEhcTkBEV0U3OFBtUVdfYmdoZz5NcXlwZHhcZWdj/9sAQwEREhIYFRgvGhovY0I4QmNjY2\n" +
    " NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj/8AAEQgAAQABA\n" +
    " wEiAAIRAQMRAf/EABUAAQEAAAAAAAAAAAAAAAAAAAAF/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/E\n" +
    " ABUBAQEAAAAAAAAAAAAAAAAAAAUG/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8\n" +
    " AigC14//Z\n\n" +
    
    // Entry # 2
    "dn: cn=Tori Amos,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: strongAuthenticationUser\n" +
    "objectClass: top\n" +
    "userCertificate:: NFZOXw==\n" +
    "cn: Tori Amos\n" +
    "description: an American singer-songwriter\n" +
    "sn: Amos\n" +
    "jpegPhoto:: /9j/4AAQSkZJRgABAQEASABIAAD/4QAWRXhpZgAATU0AKgAAAAgAAAAAAAD//gAX\n" +
    " Q3JlYXRlZCB3aXRoIFRoZSBHSU1Q/9sAQwAQCwwODAoQDg0OEhEQExgoGhgWFhgxIyUdKDozPTw\n" +
    " 5Mzg3QEhcTkBEV0U3OFBtUVdfYmdoZz5NcXlwZHhcZWdj/9sAQwEREhIYFRgvGhovY0I4QmNjY2\n" +
    " NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj/8AAEQgAAQABA\n" +
    " wEiAAIRAQMRAf/EABUAAQEAAAAAAAAAAAAAAAAAAAAF/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/E\n" +
    " ABUBAQEAAAAAAAAAAAAAAAAAAAUG/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8\n" +
    " AigC14//Z\n\n" + 

    // Entry # 3
    "dn: cn=Rolling-Stones,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: strongAuthenticationUser\n" +
    "objectClass: top\n" +
    "userCertificate:: NFZOXw==\n" +
    "cn: Rolling-Stones\n" +
    "description: an English singer-songwriter\n" +
    "sn: Jagger\n" +
    "jpegPhoto:: /9j/4AAQSkZJRgABAQEASABIAAD/4QAWRXhpZgAATU0AKgAAAAgAAAAAAAD//gAX\n" +
    " Q3JlYXRlZCB3aXRoIFRoZSBHSU1Q/9sAQwAQCwwODAoQDg0OEhEQExgoGhgWFhgxIyUdKDozPTw\n" +
    " 5Mzg3QEhcTkBEV0U3OFBtUVdfYmdoZz5NcXlwZHhcZWdj/9sAQwEREhIYFRgvGhovY0I4QmNjY2\n" +
    " NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj/8AAEQgAAQABA\n" +
    " wEiAAIRAQMRAf/EABUAAQEAAAAAAAAAAAAAAAAAAAAF/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/E\n" +
    " ABUBAQEAAAAAAAAAAAAAAAAAAAUG/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8\n" +
    " AigC14//Z\n\n" + 

    // Entry # 4
    "dn: cn=Heather Nova,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: strongAuthenticationUser\n" +
    "objectClass: top\n" +
    "userCertificate:: NFZOXw==\n" +
    "cn: Heather Nova\n" +
    "sn: Nova\n" +
    "jpegPhoto:: /9j/4AAQSkZJRgABAQEASABIAAD/4QAWRXhpZgAATU0AKgAAAAgAAAAAAAD//gAX\n" +
    " Q3JlYXRlZCB3aXRoIFRoZSBHSU1Q/9sAQwAQCwwODAoQDg0OEhEQExgoGhgWFhgxIyUdKDozPTw\n" +
    " 5Mzg3QEhcTkBEV0U3OFBtUVdfYmdoZz5NcXlwZHhcZWdj/9sAQwEREhIYFRgvGhovY0I4QmNjY2\n" +
    " NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2NjY2Nj/8AAEQgAAQABA\n" +
    " wEiAAIRAQMRAf/EABUAAQEAAAAAAAAAAAAAAAAAAAAF/8QAFBABAAAAAAAAAAAAAAAAAAAAAP/E\n" +
    " ABUBAQEAAAAAAAAAAAAAAAAAAAUG/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8\n" +
    " AigC14//Z\n\n" +
    
    // Entry #5
    "dn: cn=Janis Joplin,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: organizationalPerson\n" +
    "objectClass: inetOrgPerson\n" +
    "objectClass: top\n" +
    "objectClass: strongAuthenticationUser\n" +
    "cn: Janis Joplin\n" +
    "sn: Joplin\n" +
    "userCertificate:: \n\n" +
    
    // Entry #6
    "dn: cn=Kim Wilde,ou=system\n" +
    "objectClass: person\n" +
    "objectClass: top\n" +
    "cn: Kim Wilde\n" +
    "sn: Wilde\n\n"
    }
)
public class SearchIT 
{
    private static final String BASE = "ou=system";
    
    public static LdapService ldapService;
    
    private static final String RDN = "cn=Tori Amos";
    private static final String RDN2 = "cn=Rolling-Stones";
    private static final String HEATHER_RDN = "cn=Heather Nova";
    private static final String FILTER = "(objectclass=*)";


    private static final byte[] JPEG = new byte[]
        {
            (byte)0xff, (byte)0xd8, (byte)0xff, (byte)0xe0, 0x00, 0x10, 0x4a, 0x46, 
            0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48,
            0x00, 0x48, 0x00, 0x00, (byte)0xff, (byte)0xe1, 0x00, 0x16, 
            0x45, 0x78, 0x69, 0x66, 0x00, 0x00, 0x4d, 0x4d,
            0x00, 0x2a, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 
            0x00, 0x00, 0x00, 0x00, (byte)0xff, (byte)0xfe, 0x00, 0x17,
            0x43, 0x72, 0x65, 0x61, 0x74, 0x65, 0x64, 0x20, 
            0x77, 0x69, 0x74, 0x68, 0x20, 0x54, 0x68, 0x65,
            0x20, 0x47, 0x49, 0x4d, 0x50, (byte)0xff, (byte)0xdb, 0x00, 
            0x43, 0x00, 0x10, 0x0b, 0x0c, 0x0e, 0x0c, 0x0a,
            0x10, 0x0e, 0x0d, 0x0e, 0x12, 0x11, 0x10, 0x13, 
            0x18, 0x28, 0x1a, 0x18, 0x16, 0x16, 0x18, 0x31,
            0x23, 0x25, 0x1d, 0x28, 0x3a, 0x33, 0x3d, 0x3c, 
            0x39, 0x33, 0x38, 0x37, 0x40, 0x48, 0x5c, 0x4e,
            0x40, 0x44, 0x57, 0x45, 0x37, 0x38, 0x50, 0x6d, 
            0x51, 0x57, 0x5f, 0x62, 0x67, 0x68, 0x67, 0x3e,
            0x4d, 0x71, 0x79, 0x70, 0x64, 0x78, 0x5c, 0x65, 
            0x67, 0x63, (byte)0xff, (byte)0xdb, 0x00, 0x43, 0x01, 0x11,
            0x12, 0x12, 0x18, 0x15, 0x18, 0x2f, 0x1a, 0x1a, 
            0x2f, 0x63, 0x42, 0x38, 0x42, 0x63, 0x63, 0x63,
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63,
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63,
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 
            0x63, 0x63, 0x63, 0x63, 0x63, 0x63, 0x63, (byte)0xff,
            (byte)0xc0, 0x00, 0x11, 0x08, 0x00, 0x01, 0x00, 0x01, 
            0x03, 0x01, 0x22, 0x00, 0x02, 0x11, 0x01, 0x03,
            0x11, 0x01, (byte)0xff, (byte)0xc4, 0x00, 0x15, 0x00, 0x01, 
            0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
            0x05, (byte)0xff, (byte)0xc4, 0x00, 0x14, 0x10, 0x01, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff,
            (byte)0xc4, 0x00, 0x15, 0x01, 0x01, 0x01, 0x00, 0x00, 
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x05, 0x06, (byte)0xff, (byte)0xc4, 
            0x00, 0x14, 0x11, 0x01, 0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 
            0x00, 0x00, 0x00, 0x00, (byte)0xff, (byte)0xda, 0x00, 0x0c,
            0x03, 0x01, 0x00, 0x02, 0x11, 0x03, 0x11, 0x00, 
            0x3f, 0x00, (byte)0x8a, 0x00, (byte)0xb5, (byte)0xe3, (byte)0xff, (byte)0xd9,
        };
                                            


    /**
     * Creation of required attributes of a person entry.
     */
    private Attributes getPersonAttributes( String sn, String cn )
    {
        Attributes attributes = new BasicAttributes( true );
        Attribute attribute = new BasicAttribute( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attribute.add( "organizationalPerson" );
        attribute.add( "inetOrgPerson" );
        attributes.put( attribute );
        attributes.put( "cn", cn );
        attributes.put( "sn", sn );
        attributes.put( "jpegPhoto", JPEG );

        return attributes;
    }


    private void checkForAttributes( Attributes attrs, String[] attrNames )
    {
        for ( String attrName : attrNames )
        {
            assertNotNull( "Check if attr " + attrName + " is present", attrs.get( attrName ) );
        }
    }


    /**
     * For DIRSERVER-715 and part of DIRSERVER-169.  May include other tests
     * for binary attribute based searching.
     */
    @Test
    public void testSearchByBinaryAttribute() throws Exception 
    {
        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( BASE );
        byte[] certData = new byte[] { 0x34, 0x56, 0x4e, 0x5f };
        
        // Search for kate by cn first
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> enm = ctx.search( "", "(cn=Kate Bush)", controls );
        assertTrue( enm.hasMore() );
        SearchResult sr = enm.next();
        assertNotNull( sr );
        assertFalse( enm.hasMore() );
        assertEquals( "cn=Kate Bush", sr.getName() );

        // TODO enable this test here
        // Failing here below this due to the frontend interpretting the byte[]
        // as a String value.  I see the value sent to the SearchHandler of the
        // filter to be a String looking like: "[B@17210a5".
        
        enm = ctx.search( "", "(&(cn=Kate Bush)(userCertificate={0}))", new Object[] {certData}, controls );
        assertTrue( enm.hasMore() );
        sr = ( SearchResult ) enm.next();
        assertNotNull( sr );
        assertFalse( enm.hasMore() );
        assertEquals( "cn=Kate Bush", sr.getName() );
    }

    
    @Test
    public void testSearch() throws Exception
    {
        LdapContext ctx = getWiredContext( ldapService );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setTimeLimit( 10 );
        
        try 
        {
            ctx.search( "myBadDN", "(objectClass=*)", controls );

            fail(); // We should get an exception here
        } 
        catch ( InvalidNameException ine ) 
        {
            // Expected.
        } 
        catch ( NamingException ne )
        {
            fail();
        }
        catch( Exception e )
        {
            fail();
        }
        
        try
        {
            controls = new SearchControls();
            controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
            controls.setTimeLimit( 10 );
            
            NamingEnumeration<SearchResult> result = ctx.search( "ou=system", "(objectClass=*)", controls );

            assertTrue( result.hasMore() ); 
        } 
        catch ( InvalidNameException ine ) 
        {
            fail();
            // Expected.
        } 
        catch ( NamingException ne )
        {
            fail();
        }
    }

    
    /**
     * Performs a single level search from ou=system base and 
     * returns the set of DNs found.
     */
    private Set<String> search( String filter ) throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> ii = ctx.search( "", filter, controls );
        
        // collect all results 
        HashSet<String> results = new HashSet<String>();
        while ( ii.hasMore() )
        {
            SearchResult result = ii.next();
            results.add( result.getName() );
        }
        
        return results;
    }

    
    @Test
    public void testDirserver635() throws Exception
    {
        // -------------------------------------------------------------------
        Set<String> results = search( "(|(cn=Kate*)(cn=Tori*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=*Amos)(cn=Kate*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=Kate Bush)(cn=Tori*))" );
        assertEquals( "returned size of results", 2, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // -------------------------------------------------------------------
        results = search( "(|(cn=*Amos))" );
        assertEquals( "returned size of results", 1, results.size() );
        assertTrue( "contains cn=Tori Amos", results.contains( "cn=Tori Amos" ) );
    }

    
    /**
     * Search operation with a base DN which contains a BER encoded value.
     */
    //@Test
    /*public void testSearchBEREncodedBase() throws NamingException
    {
        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Ferry", "Bryan Ferry" );
        ctx.createSubcontext( "sn=Ferry", attributes );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String FILTER = "(cn=Bryan Ferry)";

        // sn=Ferry with BEROctetString values
        String base = "2.5.4.4=#4665727279";

        try
        {
            // Check entry
            NamingEnumeration enm = ctx.search( base, FILTER, sctls );
            assertTrue( enm.hasMore() );
            while ( enm.hasMore() )
            {
                SearchResult sr = ( SearchResult ) enm.next();
                Attributes attrs = sr.getObject();
                Attribute sn = attrs.get( "sn" );
                assertNotNull( sn );
                assertTrue( sn.contains( "Ferry" ) );
            }
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }
    }*/

    
    /**
     * Search operation with a base DN which contains a BER encoded value.
     */
    @Test
    public void testSearchWithBackslashEscapedBase() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        // create additional entry
        Attributes attributes = this.getPersonAttributes( "Ferry", "Bryan Ferry" );
        ctx.createSubcontext( "sn=Ferry", attributes );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(cn=Bryan Ferry)";

        // sn=Ferry with BEROctetString values
        String base = "sn=\\46\\65\\72\\72\\79";

        try
        {
            // Check entry
            NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls );
            assertTrue( enm.hasMore() );
            while ( enm.hasMore() )
            {
                SearchResult sr = enm.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get( "sn" );
                assertNotNull( sn );
                assertTrue( sn.contains( "Ferry" ) );
            }
        }
        catch ( Exception e )
        {
            fail( e.getMessage() );
        }
    }

    
    /**
     * Add a new attribute to a person entry.
     * 
     * @throws NamingException
     */
    @Test
    public void testSearchValue() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        // Setting up search controls for compare op
        SearchControls ctls = new SearchControls();
        ctls.setReturningAttributes( new String[]
            { "*" } ); // no attributes
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );

        // Search for all entries
        NamingEnumeration<SearchResult> results = ctx.search( RDN, "(cn=*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries ending by Amos
        results = ctx.search( RDN, "(cn=*Amos)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*Amos)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries ending by amos
        results = ctx.search( RDN, "(cn=*amos)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*amos)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries starting by Tori
        results = ctx.search( RDN, "(cn=Tori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=Tori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries starting by tori
        results = ctx.search( RDN, "(cn=tori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=tori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries containing ori
        results = ctx.search( RDN, "(cn=*ori*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*ori*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries containing o and i
        results = ctx.search( RDN, "(cn=*o*i*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o*i*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries containing o, space and o
        results = ctx.search( RDN, "(cn=*o* *o*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o* *o*)", ctls );
        assertFalse( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*o*-*o*)", ctls );
        assertTrue( results.hasMore() );

        // Search for all entries starting by To and containing A
        results = ctx.search( RDN, "(cn=To*A*)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=To*A*)", ctls );
        assertFalse( results.hasMore() );

        // Search for all entries ending by os and containing ri
        results = ctx.search( RDN, "(cn=*ri*os)", ctls );
        assertTrue( results.hasMore() );

        results = ctx.search( RDN2, "(cn=*ri*os)", ctls );
        assertFalse( results.hasMore() );
    }
    
    /**
     * Search operation with a base DN with quotes
     *
    @Test
    public void testSearchWithQuotesInBase() throws NamingException {

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope(SearchControls.OBJECT_SCOPE);
        String filter = "(cn=Tori Amos)";

        // cn="Tori Amos" (with quotes)
        String base = "cn=\"Tori Amos\"";

        try {
            // Check entry
            NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls );
            assertTrue( enm.hasMore() );
            
            while ( enm.hasMore() ) {
                SearchResult sr = enm.next();
                Attributes attrs = sr.getAttributes();
                Attribute sn = attrs.get("sn");
                assertNotNull(sn);
                assertTrue( sn.contains( "Amos" ) );
            }
        } catch (Exception e) {
            fail( e.getMessage() );
        }
    }
 
    
    /**
     * Tests for <a href="http://issues.apache.org/jira/browse/DIRSERVER-645">
     * DIRSERVER-645<\a>: Wrong search FILTER evaluation with AND
     * operator and undefined operands.
     */
    @Test
    public void testUndefinedAvaInBranchFilters() throws Exception
    {
        // -------------------------------------------------------------------
        Set<String> results = search( "(|(sn=Bush)(numberOfOctaves=4))" );
        assertEquals( "returned size of results", 1, results.size() );
        assertTrue( "contains cn=Kate Bush", results.contains( "cn=Kate Bush" ) );

        // if numberOfOctaves is undefined then this whole FILTER is undefined
        results = search( "(&(sn=Bush)(numberOfOctaves=4))" );
        assertEquals( "returned size of results", 0, results.size() );
    }
    
    
    @Test
    public void testSearchSchema() throws Exception
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.OBJECT_SCOPE );
        controls.setReturningAttributes( new String[] { "objectClasses" } );

        LdapContext ctx = getWiredContext( ldapService );

        NamingEnumeration<SearchResult> results = ctx.search( "cn=schema", "objectClass=subschema", controls );
        assertTrue( results.hasMore() );
        SearchResult result = results.next();
        assertNotNull( result );
        assertFalse( results.hasMore() );
        
        NamingEnumeration<? extends Attribute> attrs = result.getAttributes().getAll();
        
        while ( attrs.hasMoreElements() )
        {
            Attribute attr = ( Attribute ) attrs.next();
            String ID = attr.getID();
            assertEquals( "objectClasses", ID );
        }
        
        assertNotNull( result.getAttributes().get( "objectClasses" ) );
        assertEquals( 1, result.getAttributes().size() );
    }
    
    
    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param subtree the subtreeSpecification for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    private void createAccessControlSubentry( String cn, String subtree, String aciItem ) throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        DirContext adminCtx = ctx;

        // modify ou=system to be an AP for an A/C AA if it is not already
        Attributes ap = adminCtx.getAttributes( "", new String[] { "administrativeRole" } );
        Attribute administrativeRole = ap.get( "administrativeRole" );
        if ( administrativeRole == null || !administrativeRole.contains( SubentryInterceptor.AC_AREA ) )
        {
            Attributes changes = new BasicAttributes( "administrativeRole", SubentryInterceptor.AC_AREA, true );
            adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }

        // now add the A/C subentry below ou=system
        Attributes subentry = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", subtree );
        subentry.put( "prescriptiveACI", aciItem );
        adminCtx.createSubcontext( "cn=" + cn, subentry );
    }
    

    /**
     * Test case to demonstrate DIRSERVER-705 ("object class top missing in search
     * result, if scope is base and attribute objectClass is requested explicitly").
     */
    @Test
    public void testAddWithObjectclasses() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "objectclass" } );
        String filter = "(objectclass=*)";
        String rdn = "cn=Kim Wilde";

        NamingEnumeration<SearchResult> result = ctx.search( rdn, filter, ctls );
        
        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes heatherReloaded = entry.getAttributes();
            Attribute loadedOcls = heatherReloaded.get( "objectClass" );
            assertNotNull( loadedOcls );
            assertTrue( loadedOcls.contains( "person" ) );
            assertTrue( loadedOcls.contains( "top" ) );
        }
        else
        {
            fail( "entry " + rdn + " not found" );
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Test case to demonstrate DIRSERVER-705 ("object class top missing in search
     * result, if scope is base and attribute objectClass is requested explicitly").
     */
    @Test
    public void testAddWithMissingObjectclasses() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        String rdn = "cn=Kate Bush";
        SearchControls ctls = new SearchControls();
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "objectclass" } );
        String filter = "(objectclass=*)";

        NamingEnumeration<SearchResult> result = ctx.search( rdn, filter, ctls );
        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes kateReloaded = entry.getAttributes();
            Attribute loadedOcls = kateReloaded.get( "objectClass" );
            assertNotNull( loadedOcls );
            assertTrue( loadedOcls.contains( "top" ) );
            assertTrue( loadedOcls.contains( "person" ) );
            assertTrue( loadedOcls.contains( "organizationalPerson" ) );

        }
        else
        {
            fail( "entry " + rdn + " not found" );
        }

        ctx.destroySubcontext( rdn );
    }


    @Test
    public void testSubentryControl() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        // create a real access control subentry
        createAccessControlSubentry( 
            "anyBodyAdd", 
            "{}", 
            "{ " + 
            "  identificationTag \"addAci\", " + 
            "  precedence 14, " +
            "  authenticationLevel none, " + 
            "  itemOrUserFirst userFirst: " +
            "  { " +
            "    userClasses " + 
            "    { " + 
            "      allUsers " + 
            "    }, " +
            "    userPermissions " + 
            "    { " + 
            "      { " + 
            "        protectedItems " + 
            "        { " + 
            "          entry, allUserAttributeTypesAndValues" + 
            "        }, " +
            "        grantsAndDenials " + 
            "        { " + 
            "          grantAdd, grantBrowse " +
            "        } " +
            "      } " +
            "    } " + 
            "  } " + 
            "}"
        );
        
        // prepare the subentry control to make the subentry visible
        SubentriesControl control = new SubentriesControl();
        control.setVisibility( true );
        Control[] reqControls = new Control[] { control };
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        ctx.setRequestControls( reqControls );
        NamingEnumeration<SearchResult> enm = ctx.search( "", "(objectClass=*)", searchControls );
        Set<String> results = new HashSet<String>();
        
        while ( enm.hasMore() )
        {
            SearchResult result = enm.next();
            results.add( result.getName() );
        }
        
        assertEquals( "expected results size of", 1, results.size() );
        assertTrue( results.contains( "cn=anyBodyAdd" ) );
    }

    
    /**
     * Create a person entry with multivalued RDN and check its content. This
     * testcase was created to demonstrate DIRSERVER-628.
     */
    @Test
    public void testMultiValuedRdnContent() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush+sn=Bush";
        ctx.createSubcontext( rdn, attrs );

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        String filter = "(sn=Bush)";
        String base = "";

        NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls );
        while ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            attrs = sr.getAttributes();
            Attribute cn = sr.getAttributes().get( "cn" );
            assertNotNull( cn );
            assertTrue( cn.contains( "Kate Bush" ) );
            Attribute sn = sr.getAttributes().get( "sn" );
            assertNotNull( sn );
            assertTrue( sn.contains( "Bush" ) );
        }

        ctx.destroySubcontext( rdn );
    }


    /**
     * Create a person entry with multivalued RDN and check its name.
     */
    @Test
    public void testMultiValuedRdnName() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        Attributes attrs = getPersonAttributes( "Bush", "Kate Bush" );
        String rdn = "cn=Kate Bush+sn=Bush";
        DirContext entry = ctx.createSubcontext( rdn, attrs );
        String nameInNamespace = entry.getNameInNamespace();

        SearchControls sctls = new SearchControls();
        sctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        String filter = "(sn=Bush)";
        String base = rdn;

        NamingEnumeration<SearchResult> enm = ctx.search( base, filter, sctls );
        if ( enm.hasMore() )
        {
            SearchResult sr = enm.next();
            assertNotNull( sr );
            assertEquals( "Name in namespace", nameInNamespace, sr.getNameInNamespace() );
        }
        else
        {
            fail( "Entry not found:" + nameInNamespace );
        }

        ctx.destroySubcontext( rdn );
    }
    
    
    @Test
    public void testSearchJpeg() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> res = ctx.search( "", "(cn=Tori*)", controls );
        
        // collect all results 
        while ( res.hasMore() )
        {
            SearchResult result = res.next();

            Attributes attrs = result.getAttributes();
            
            NamingEnumeration<? extends Attribute> all = attrs.getAll();
                
            while ( all.hasMoreElements() )
            {
                Attribute attr = all.next();
                
                if ( "jpegPhoto".equalsIgnoreCase( attr.getID() ) )
                {
                    byte[] jpegVal = (byte[])attr.get();
                    
                    assertTrue( Arrays.equals( jpegVal, JPEG ) );
                }
            }
        }
    }
    
    
    @Test
    public void testSearchOID() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        NamingEnumeration<SearchResult> res = ctx.search( "", "(2.5.4.3=Tori*)", controls );
        
        // ensure that the entry "cn=Tori Amos" was found
        assertTrue( res.hasMore() );

        SearchResult result = ( SearchResult ) res.next();

        // ensure that result is not null
        assertNotNull( result );
        
        String rdn = result.getName();
        
        // ensure that the entry "cn=Tori Amos" was found
        assertEquals( "cn=Tori Amos", rdn );
        
        // ensure that no other value was found
        assertFalse( res.hasMore() );
    }

    
    @Test
    public void testSearchAttrCN() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"cn"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori*)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();

        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure the one and only attribute is "cn"
        assertEquals( 1, attrs.size() );
        assertNotNull( attrs.get( "cn" ) );
        assertEquals( 1, attrs.get( "cn" ).size() );
        assertEquals( "Tori Amos", ( String ) attrs.get("cn").get() );
    }

    
    @Test
    public void testSearchAttrName() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"name"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori*)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure that "cn" and "sn" are returned
        assertEquals( 2, attrs.size() );
        assertNotNull( attrs.get( "cn" ) );
        assertEquals( 1, attrs.get("cn").size() );
        assertEquals( "Tori Amos", ( String ) attrs.get( "cn" ).get() );
        assertNotNull( attrs.get( "sn" ) );
        assertEquals( 1, attrs.get( "sn" ).size() );
        assertEquals( "Amos", ( String ) attrs.get( "sn" ).get() );
    }

    
    @Test
    public void testSearchAttrCommonName() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[] { "commonName" } );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori*)", controls );
        
        assertTrue( res.hasMore() );
        

        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // requested attribute was "commonName", but ADS returns "cn". 
        //       Other servers do the following:
        //       - OpenLDAP: also return "cn"
        //       - Siemens DirX: return "commonName"
        //       - Sun Directory 5.2: return "commonName"
        // ensure the one and only attribute is "cn"
        assertEquals( 1, attrs.size() );
        assertNotNull( attrs.get("cn") );
        assertEquals( 1, attrs.get("cn").size() );
        assertEquals( "Tori Amos", (String)attrs.get("cn").get() );
    }
    

    @Test
    public void testSearchAttrOID() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"2.5.4.3"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori*)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // requested attribute was "2.5.4.3", but ADS returns "cn". 
        //       Other servers do the following:
        //       - OpenLDAP: also return "cn"
        //       - Siemens DirX: also return "cn"
        //       - Sun Directory 5.2: return "2.5.4.3"
        // ensure the one and only attribute is "cn"
        assertEquals( 1, attrs.size() );
        assertNotNull( attrs.get("cn") );
        assertEquals( 1, attrs.get("cn").size() );
        assertEquals( "Tori Amos", (String)attrs.get("cn").get() );
    }
    
    
    @Test
    public void testSearchAttrC_L() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        // create administrative area
        Attributes aaAttrs = new BasicAttributes( true );
        Attribute aaObjectClass = new BasicAttribute( "objectClass" );
        aaObjectClass.add( "top" );
        aaObjectClass.add( "organizationalUnit" );
        aaObjectClass.add( "extensibleObject" );
        aaAttrs.put( aaObjectClass );
        aaAttrs.put( "ou", "Collective Area" );
        aaAttrs.put( "administrativeRole", "collectiveAttributeSpecificArea" );
        DirContext aaCtx = ctx.createSubcontext( "ou=Collective Area", aaAttrs );
        
        // create subentry
        Attributes subentry = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "c-l", "Munich" );
        subentry.put( "cn", "Collective Subentry" );
        subentry.put( "subtreeSpecification", "{ }" );
        aaCtx.createSubcontext( "cn=Collective Subentry", subentry );
        
        // create real enty
        Attributes attributes = this.getPersonAttributes( "Bush", "Kate Bush" );
        aaCtx.createSubcontext( "cn=Kate Bush", attributes );
        
        // search
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"c-l" } );
        
        NamingEnumeration<SearchResult> res = aaCtx.search( "", "(cn=Kate Bush)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure the one and only attribute is "c-l"
        assertEquals( 1, attrs.size() );
        assertNotNull( attrs.get("c-l") );
        assertEquals( 1, attrs.get("c-l").size() );
        assertEquals( "Munich", (String)attrs.get("c-l").get() );
    }


    @Test
    public void testSearchUsersAttrs() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"*"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori Amos)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure that all user attributes are returned
        assertEquals( 6, attrs.size() );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "sn" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "jpegPhoto" ) );
        assertNotNull( attrs.get( "description" ) );
        assertNotNull( attrs.get( "userCertificate" ) );
        assertNull( attrs.get( "createtimestamp" ) );
        assertNull( attrs.get( "creatorsname" ) );
    }


    @Test
    public void testSearchOperationalAttrs() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"+"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori Amos)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure that all operational attributes are returned
        // and no user attributes
        assertEquals( 4, attrs.size() );
        assertNull( attrs.get( "cn" ) );
        assertNull( attrs.get( "sn" ) );
        assertNull( attrs.get( "objectClass" ) );
        assertNull( attrs.get( "jpegPhoto" ) );
        assertNull( attrs.get( "description" ) );
        assertNotNull( attrs.get( "createtimestamp" ) );
        assertNotNull( attrs.get( "creatorsname" ) );
        assertNotNull( attrs.get( "entryuuid" ) );
        assertNotNull( attrs.get( "entrycsn" ) );
    }
    

    @Test
    public void testSearchAllAttrs() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]{"+", "*"} );
        
        NamingEnumeration<SearchResult> res = ctx.search( "", "(commonName=Tori Amos)", controls );
        
        assertTrue( res.hasMore() );
        
        SearchResult result = ( SearchResult ) res.next();
        
        // ensure that result is not null
        assertNotNull( result );
        
        Attributes attrs = result.getAttributes();
        
        // ensure that all user attributes are returned
        assertEquals( 10, attrs.size() );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "sn" ) );
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "jpegPhoto" ) );
        assertNotNull( attrs.get( "userCertificate" ) );
        assertNotNull( attrs.get( "description" ) );
        assertNotNull( attrs.get( "createtimestamp" ) );
        assertNotNull( attrs.get( "creatorsname" ) );
        assertNotNull( attrs.get( "entryuuid" ) );
        assertNotNull( attrs.get( "entrycsn" ) );
    }


    @Test
    public void testSearchBadDN() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        try
        {
            ctx.search( "cn=admin", "(objectClass=*)", controls );
        }
        catch ( NameNotFoundException nnfe )
        {
            assertTrue( true );
        }
    }
    

    @Test
    public void testSearchInvalidDN() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        
        try
        {
            ctx.search( "myBadDN", "(objectClass=*)", controls );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
    

    /**
     * Check if operational attributes are present, if "+" is requested.
     */
    @Test
    public void testSearchOperationalAttributes() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        SearchControls ctls = new SearchControls();

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "+" } );

        NamingEnumeration<SearchResult> result = ctx.search( HEATHER_RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();

            String[] opAttrNames =
                { "creatorsName", "createTimestamp" };

            checkForAttributes( entry.getAttributes(), opAttrNames );
        }
        else
        {
            fail( "entry " + HEATHER_RDN + " not found" );
        }

        result.close();
    }


    /**
     * Check if user attributes are present, if "*" is requested.
     */
    @Test
    public void testSearchUserAttributes() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        SearchControls ctls = new SearchControls();

        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "*" } );

        NamingEnumeration<SearchResult> result = ctx.search( HEATHER_RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();

            String[] userAttrNames =
                { "objectClass", "sn", "cn" };

            checkForAttributes( entry.getAttributes(), userAttrNames );
        }
        else
        {
            fail( "entry " + HEATHER_RDN + " not found" );
        }

        result.close();
    }
    
    
    /**
     * Check if user and operational attributes are present, if both "*" and "+" are requested.
     */
    @Test
    public void testSearchOperationalAndUserAttributes() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        SearchControls ctls = new SearchControls();
 
        ctls.setSearchScope( SearchControls.OBJECT_SCOPE );
        ctls.setReturningAttributes( new String[]
            { "+", "*" } );

        String[] userAttrNames =
            { "objectClass", "sn", "cn" };

        String[] opAttrNames =
            { "creatorsName", "createTimestamp" };

        NamingEnumeration<SearchResult> result = ctx.search( HEATHER_RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = result.next();
            Attributes attrs = entry.getAttributes();

            assertNotNull( attrs );

            checkForAttributes( attrs, userAttrNames );
            checkForAttributes( attrs, opAttrNames );
        }
        else
        {
            fail( "entry " + HEATHER_RDN + " not found" );
        }

        result.close();

        ctls.setReturningAttributes( new String[]
            { "*", "+" } );

        result = ctx.search( HEATHER_RDN, FILTER, ctls );

        if ( result.hasMore() )
        {
            SearchResult entry = ( SearchResult ) result.next();
            Attributes attrs = entry.getAttributes();

            assertNotNull( attrs );
            
            checkForAttributes( attrs, userAttrNames );
            checkForAttributes( attrs, opAttrNames );
        }
        else
        {
            fail( "entry " + HEATHER_RDN + " not found" );
        }

        result.close();
    }
   
    
    @Test
    public void testSubstringSearchWithEscapedCharsInFilter() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        Attributes attrs = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        attrs.get( "objectClass" ).add( "organizationalPerson" );
        attrs.get( "objectClass" ).add( "person" );
        attrs.put( "givenName", "Jim" );
        attrs.put( "sn", "Bean" );
        attrs.put( "cn", "jimbean" );
        attrs.put( "description", "(sex*pis\\tols)" );
        ctx.createSubcontext( "cn=jimbean", attrs );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]
            { "cn" } );

        String[] filters = new String[]
            { "(description=*\\28*)", "(description=*\\29*)", "(description=*\\2A*)", "(description=*\\5C*)" };
        for ( String filter : filters )
        {
            NamingEnumeration<SearchResult> res = ctx.search( "", filter, controls );
            assertTrue( res.hasMore() );
            SearchResult result = res.next();
            assertNotNull( result );
            attrs = result.getAttributes();
            assertEquals( 1, attrs.size() );
            assertNotNull( attrs.get( "cn" ) );
            assertEquals( 1, attrs.get( "cn" ).size() );
            assertEquals( "jimbean", ( String ) attrs.get( "cn" ).get() );
            assertFalse( res.hasMore() );
        }
    }


    /**
     * Test for DIRSERVER-1180 where search hangs when an invalid a substring 
     * expression missing an any field is used in a filter: i.e. (cn=**).
     * 
     * @see https://issues.apache.org/jira/browse/DIRSERVER-1180
     */
    @Test
    public void testMissingAnyInSubstring_DIRSERVER_1180() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );
        Attributes attrs = new BasicAttributes( "objectClass", "inetOrgPerson", true );
        attrs.get( "objectClass" ).add( "organizationalPerson" );
        attrs.get( "objectClass" ).add( "person" );
        attrs.put( "givenName", "Jim" );
        attrs.put( "sn", "Bean" );
        attrs.put( "cn", "jimbean" );
        
        ctx.createSubcontext( "cn=jimbean", attrs );
        
        try
        {
            ctx.search( "", "(cn=**)", new SearchControls() );
            fail();
        }
        catch ( Exception e )
        {
            assertTrue( true );
        }
    }

    
    @Test
    public void testSubstringSearchWithEscapedAsterisksInFilter_DIRSERVER_1181() throws Exception
    {
        LdapContext ctx = ( LdapContext ) getWiredContext( ldapService ).lookup( BASE );

        Attributes vicious = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" );
        vicious.put( ocls );
        vicious.put( "cn", "x*y*z*" );
        vicious.put( "sn", "x*y*z*" );
        ctx.createSubcontext( "cn=x*y*z*", vicious );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.ONELEVEL_SCOPE );
        controls.setReturningAttributes( new String[]
            { "cn" } );
        NamingEnumeration<SearchResult> res;

        res = ctx.search( "", "(cn=*x\\2Ay\\2Az\\2A*)", controls );
        assertTrue( res.hasMore() );
        assertEquals( "x*y*z*", res.next().getAttributes().get( "cn" ).get() );
        assertFalse( res.hasMore() );

        res = ctx.search( "", "(cn=*{0}*)", new String[]
            { "x*y*z*" }, controls );
        assertTrue( res.hasMore() );
        assertEquals( "x*y*z*", res.next().getAttributes().get( "cn" ).get() );
        assertFalse( res.hasMore() );
    }
}
