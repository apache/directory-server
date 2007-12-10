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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.unit.AbstractAdminTestCase;
import org.apache.directory.shared.ldap.constants.JndiPropertyConstants;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.DerefAliasesEnum;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.Attribute;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * Test DIRSERVER-757 : a UniqueMember attribute should only contain a DN completed with an
 * optional UID. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UniqueMemberITest extends AbstractAdminTestCase
{
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    
    /**
     * Test a valid entry
     */
    public void testValidUniqueMember() throws Exception
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey, dc=example, dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }
        
        Attributes returned = sysRoot.getAttributes( "cn=kevin Spacey" );
      
        NamingEnumeration attrList = returned.getAll();
        
        while ( attrList.hasMore() )
        {
            Attribute attr = ( Attribute ) attrList.next();
          
            if ( attr.getID().equalsIgnoreCase( "cn" ) )
            {
                assertEquals( "kevin Spacey", attr.get() );
                continue;
            }
          
            if ( attr.getID().equalsIgnoreCase( "objectClass" ) )
            {
                NamingEnumeration values = attr.getAll();
                Set<String> expectedValues = new HashSet<String>();
                
                expectedValues.add( "top" );
                expectedValues.add( "groupofuniquenames" );
                
                while ( values.hasMoreElements() )
                {
                    String value = ( (String)values.nextElement() ).toLowerCase();
                    assertTrue( expectedValues.contains( value ) );
                    expectedValues.remove( value );
                }
                
                assertEquals( 0, expectedValues.size() );
                continue;
            }
          
            if ( attr.getID().equalsIgnoreCase( "uniqueMember" ) )
            {
                assertEquals( "cn=kevin spacey, dc=example, dc=org", attr.get() );
                continue;
            }
        }
    }

    /**
     * Test a valid entry, with an optional UID
     */
    public void testValidUniqueMemberWithOptionnalUID() throws Exception
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey 2" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey 2, dc=example, dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey 2";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }
        
        Attributes returned = sysRoot.getAttributes( "cn=kevin Spacey 2" );
      
        NamingEnumeration attrList = returned.getAll();
        
        while ( attrList.hasMore() )
        {
            Attribute attr = ( Attribute ) attrList.next();
          
            if ( attr.getID().equalsIgnoreCase( "cn" ) )
            {
                assertEquals( "kevin Spacey 2", attr.get() );
                continue;
            }
          
            if ( attr.getID().equalsIgnoreCase( "objectClass" ) )
            {
                NamingEnumeration values = attr.getAll();
                Set<String> expectedValues = new HashSet<String>();
                
                expectedValues.add( "top" );
                expectedValues.add( "groupofuniquenames" );
                
                while ( values.hasMoreElements() )
                {
                    String value = ( (String)values.nextElement() ).toLowerCase();
                    assertTrue( expectedValues.contains( value ) );
                    expectedValues.remove( value );
                }
                
                assertEquals( 0, expectedValues.size() );
                continue;
            }
          
            if ( attr.getID().equalsIgnoreCase( "uniqueMember" ) )
            {
                assertEquals( "cn=kevin spacey 2, dc=example, dc=org#'010101'B", attr.get() );
                continue;
            }
        }
    }

    /**
     * Test a valid entry, with an optional UID
     */
    public void testInvalidUniqueMemberBadDN() throws Exception
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey bad" );
        Attribute dc = new AttributeImpl( "uniqueMember", "kevin spacey bad, dc=example, dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey bad";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }

    /**
     * Test a valid entry, with an optional UID
     */
    public void testInvalidUniqueMemberBadUID() throws Exception
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey bad 2" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey bad 2, dc=example, dc=org#'010101'" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey bad 2";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
            fail();
        }
        catch ( NamingException ne )
        {
            assertTrue( true );
        }
    }
    
    public void testSearchUniqueMemberFilter() throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey, dc=example, dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[] { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, DerefAliasesEnum.NEVER_DEREF_ALIASES );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration list = sysRoot.search( "", "(uniqueMember=cn = kevin spacey, dc=example, dc=org)", controls );
        
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            map.put( result.getName().toLowerCase(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        
        attrs = map.get( "cn=kevin spacey,ou=system" );
        
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }

    public void testSearchUniqueMemberFilterWithSpaces() throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey,dc=example,dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[] { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, DerefAliasesEnum.NEVER_DEREF_ALIASES );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration list = sysRoot.search( "", "(uniqueMember=cn = Kevin  Spacey , dc = example , dc = ORG)", controls );
        
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            map.put( result.getName().toLowerCase(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        
        attrs = map.get( "cn=kevin spacey,ou=system" );
        
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }

    public void testSearchUniqueMemberFilterWithBadDN() throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey,dc=example,dc=org" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[] { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, DerefAliasesEnum.NEVER_DEREF_ALIASES );

        NamingEnumeration list = sysRoot.search( "", "(uniqueMember=cn=cevin spacey,dc=example,dc=org)", controls );
        
        assertFalse( list.hasMore() );
    }

    public void testSearchUniqueMemberFilterWithUID() throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );
        Attribute oc = new AttributeImpl( "ObjectClass", "top" );
        oc.add( "groupOfUniqueNames" );
        Attribute cn = new AttributeImpl( "cn", "kevin Spacey" );
        Attribute dc = new AttributeImpl( "uniqueMember", "cn=kevin spacey,dc=example,dc=org#'010101'B" );
        attrs.put( oc );
        attrs.put( cn );
        attrs.put( dc);

        String base = "cn=kevin Spacey";

        //create subcontext
        try
        {
            sysRoot.createSubcontext( base, attrs );
        }
        catch ( NamingException ne )
        {
            fail();
        }

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setDerefLinkFlag( false );
        controls.setReturningAttributes( new String[] { "*" } );
        sysRoot.addToEnvironment( JndiPropertyConstants.JNDI_LDAP_DAP_DEREF_ALIASES, DerefAliasesEnum.NEVER_DEREF_ALIASES );
        HashMap<String, Attributes> map = new HashMap<String, Attributes>();

        NamingEnumeration list = sysRoot.search( "", "(uniqueMember=cn= Kevin Spacey, dc=example, dc=org #'010101'B)", controls );
        
        while ( list.hasMore() )
        {
            SearchResult result = ( SearchResult ) list.next();
            map.put( result.getName().toLowerCase(), result.getAttributes() );
        }

        assertEquals( "Expected number of results returned was incorrect!", 1, map.size() );
        
        attrs = map.get( "cn=kevin spacey,ou=system" );
        
        assertNotNull( attrs.get( "objectClass" ) );
        assertNotNull( attrs.get( "cn" ) );
        assertNotNull( attrs.get( "uniqueMember" ) );
    }
}
