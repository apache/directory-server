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
package org.apache.directory.server.core.collective;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.CiRunner;
import org.apache.directory.server.core.integ.SetupMode;
import org.apache.directory.server.core.integ.annotations.Mode;
import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import java.util.HashMap;
import java.util.Map;


/**
 * Test cases for the collective attribute service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
@RunWith ( CiRunner.class )
@Mode ( SetupMode.ROLLBACK )
public class CollectiveAttributeServiceIT
{
    public static DirectoryService service;


    public Attributes getTestEntry( String cn )
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "person" );
        subentry.put( objectClass );
        subentry.put( "cn", cn );
        subentry.put( "sn", "testentry" );
        return subentry;
    }


    public Attributes getTestSubentry()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "c-ou", "configuration" );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "cn", "testsubentry" );
        return subentry;
    }


    public Attributes getTestSubentry2()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "c-ou", "configuration2" );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "cn", "testsubentry2" );
        return subentry;
    }


    public Attributes getTestSubentry3()
    {
        Attributes subentry = new AttributesImpl();
        Attribute objectClass = new AttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "subentry" );
        objectClass.add( "collectiveAttributeSubentry" );
        subentry.put( objectClass );
        subentry.put( "c-st", "FL" );
        subentry.put( "subtreeSpecification", "{ base \"ou=configuration\" }" );
        subentry.put( "cn", "testsubentry3" );
        return subentry;
    }


    public void addAdministrativeRole( String role ) throws NamingException
    {
        Attribute attribute = new AttributeImpl( "administrativeRole" );
        attribute.add( role );
        ModificationItemImpl item = new ModificationItemImpl( DirContext.ADD_ATTRIBUTE, attribute );
        getSystemContext( service ).modifyAttributes( "", new ModificationItemImpl[] { item } );
    }


    public Map<String, Attributes> getAllEntries() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        NamingEnumeration<SearchResult> results = getSystemContext( service ).search( "", "(objectClass=*)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }


    public SearchResult getEntry( String name ) throws NamingException
    {
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "+", "*" } );
        
        NamingEnumeration<SearchResult> results = getSystemContext( service ).search( name, "(objectClass=*)", controls );
        
        if ( results.hasMore() )
        {
            return results.next();
        }
        
        return null;
    }


    public Map<String, Attributes> getAllEntriesRestrictAttributes() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { "cn" } );
        NamingEnumeration<SearchResult> results = getSystemContext( service ).search( "", "(objectClass=*)", controls );
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }
    
    
    public Map<String, Attributes> getAllEntriesCollectiveAttributesOnly() throws NamingException
    {
        Map<String, Attributes> resultMap = new HashMap<String, Attributes>();
        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
                                                    { "c-ou", "c-st" } );
        NamingEnumeration<SearchResult> results = getSystemContext( service ).search( "", "(objectClass=*)", controls );
        
        while ( results.hasMore() )
        {
            SearchResult result = results.next();
            resultMap.put( result.getName(), result.getAttributes() );
        }
        return resultMap;
    }
    

    @Test
    public void testLookup() throws Exception
    {
        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------

        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        getSystemContext( service ).createSubcontext( "cn=testsubentry", getTestSubentry() );

        // -------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou
        // -------------------------------------------------------------------

        Attributes attributes = getSystemContext( service ).getAttributes( "ou=services,ou=configuration" );
        Attribute c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.get() );

        // -------------------------------------------------------------------
        // test an entry that should not show the collective attribute
        // -------------------------------------------------------------------

        attributes = getSystemContext( service ).getAttributes( "ou=users" );
        c_ou = attributes.get( "c-ou" );
        assertNull( "the c-ou collective attribute should not be present", c_ou );

        // -------------------------------------------------------------------
        // now modify entries included by the subentry to have collectiveExclusions
        // -------------------------------------------------------------------

        ModificationItemImpl[] items = new ModificationItemImpl[]
            { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE,
                new AttributeImpl( "collectiveExclusions", "c-ou" ) ) };
        getSystemContext( service ).modifyAttributes( "ou=services,ou=configuration", items );

        // entry should not show the c-ou collective attribute anymore
        attributes = getSystemContext( service ).getAttributes( "ou=services,ou=configuration" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // now add more collective subentries - the c-ou should still not show due to exclusions
        getSystemContext( service ).createSubcontext( "cn=testsubentry2", getTestSubentry2() );

        attributes = getSystemContext( service ).getAttributes( "ou=services,ou=configuration" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // entries without the collectiveExclusion should still show both values of c-ou
        attributes = getSystemContext( service ).getAttributes( "ou=interceptors,ou=configuration" );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        // request the collective attribute specifically
        
        attributes = getSystemContext( service ).getAttributes(
                "ou=interceptors,ou=configuration", new String[] { "c-ou" } );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );
        
        // unspecify the collective attribute in the returning attribute list

        attributes = getSystemContext( service ).getAttributes(
                "ou=interceptors,ou=configuration", new String[] { "objectClass" } );
        c_ou = attributes.get( "c-ou" );
        assertNull( "a collective c-ou attribute should not be present", c_ou );
        
        // -------------------------------------------------------------------
        // now add the subentry for the c-st collective attribute
        // -------------------------------------------------------------------

        getSystemContext( service ).createSubcontext( "cn=testsubentry3", getTestSubentry3() );

        // the new attribute c-st should appear in the node with the c-ou exclusion
        attributes = getSystemContext( service ).getAttributes( "ou=services,ou=configuration" );
        Attribute c_st = attributes.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // in node without exclusions both values of c-ou should appear with c-st value
        attributes = getSystemContext( service ).getAttributes( "ou=interceptors,ou=configuration" );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );
        c_st = attributes.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // -------------------------------------------------------------------
        // now modify an entry to exclude all collective attributes
        // -------------------------------------------------------------------

        items = new ModificationItemImpl[]
            { new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "collectiveExclusions",
                "excludeAllCollectiveAttributes" ) ) };
        getSystemContext( service ).modifyAttributes( "ou=interceptors,ou=configuration", items );

        // none of the attributes should appear any longer
        attributes = getSystemContext( service ).getAttributes( "ou=interceptors,ou=configuration" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }
        c_st = attributes.get( "c-st" );
        if ( c_st != null )
        {
            assertEquals( "the c-st collective attribute should not be present", 0, c_st.size() );
        }
    }


    @Test
    public void testSearch() throws Exception
    {
        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------

        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        getSystemContext( service ).createSubcontext( "cn=testsubentry", getTestSubentry() );

        // -------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou
        // -------------------------------------------------------------------

        Map<String, Attributes> entries = getAllEntries();
        Attributes attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        Attribute c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.get() );

        
        // ------------------------------------------------------------------
        // test an entry that should show the collective attribute c-ou, 
        // but restrict returned attributes to c-ou and c-st
        // ------------------------------------------------------------------
        
        entries = getAllEntriesCollectiveAttributesOnly();
        attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertEquals( "configuration", c_ou.get() );   
        
        
        // -------------------------------------------------------------------
        // test an entry that should not show the collective attribute
        // -------------------------------------------------------------------

        attributes = entries.get( "ou=users,ou=system" );
        c_ou = attributes.get( "c-ou" );
        assertNull( "the c-ou collective attribute should not be present", c_ou );

        // -------------------------------------------------------------------
        // now modify entries included by the subentry to have collectiveExclusions
        // -------------------------------------------------------------------

        ModificationItemImpl[] items = new ModificationItemImpl[]
            { new ModificationItemImpl( DirContext.ADD_ATTRIBUTE,
                new AttributeImpl( "collectiveExclusions", "c-ou" ) ) };
        getSystemContext( service ).modifyAttributes( "ou=services,ou=configuration", items );
        entries = getAllEntries();

        // entry should not show the c-ou collective attribute anymore
        attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // now add more collective subentries - the c-ou should still not show due to exclusions
        getSystemContext( service ).createSubcontext( "cn=testsubentry2", getTestSubentry2() );
        entries = getAllEntries();

        attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }

        // entries without the collectiveExclusion should still show both values of c-ou
        attributes = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );

        // -------------------------------------------------------------------
        // now add the subentry for the c-st collective attribute
        // -------------------------------------------------------------------

        getSystemContext( service ).createSubcontext( "cn=testsubentry3", getTestSubentry3() );
        entries = getAllEntries();

        // the new attribute c-st should appear in the node with the c-ou exclusion
        attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        Attribute c_st = attributes.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // in node without exclusions both values of c-ou should appear with c-st value
        attributes = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
        assertTrue( c_ou.contains( "configuration2" ) );
        c_st = attributes.get( "c-st" );
        assertNotNull( "a collective c-st attribute should be present", c_st );
        assertTrue( c_st.contains( "FL" ) );

        // -------------------------------------------------------------------
        // now modify an entry to exclude all collective attributes
        // -------------------------------------------------------------------

        items = new ModificationItemImpl[]
            { new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl( "collectiveExclusions",
                "excludeAllCollectiveAttributes" ) ) };
        getSystemContext( service ).modifyAttributes( "ou=interceptors,ou=configuration", items );
        entries = getAllEntries();

        // none of the attributes should appear any longer
        attributes = entries.get( "ou=interceptors,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        if ( c_ou != null )
        {
            assertEquals( "the c-ou collective attribute should not be present", 0, c_ou.size() );
        }
        c_st = attributes.get( "c-st" );
        if ( c_st != null )
        {
            assertEquals( "the c-st collective attribute should not be present", 0, c_st.size() );
        }

        // -------------------------------------------------------------------
        // Now search attributes but restrict returned attributes to cn and ou
        // -------------------------------------------------------------------

        entries = getAllEntriesRestrictAttributes();

        // we should no longer see collective attributes with restricted return attribs
        attributes = entries.get( "ou=services,ou=configuration,ou=system" );
        c_st = attributes.get( "c-st" );
        assertNull( "a collective c-st attribute should NOT be present", c_st );

        attributes = entries.get( "ou=partitions,ou=configuration,ou=system" );
        c_ou = attributes.get( "c-ou" );
        c_st = attributes.get( "c-st" );
        assertNull( c_ou );
        assertNull( c_st );
    }
    
    
    @Test
    public void testAddRegularEntryWithCollectiveAttribute()
    {
        Attributes entry = getTestEntry( "Ersin Er" );
        entry.put( "c-l", "Turkiye" );
        try
        {
            getSystemContext( service ).createSubcontext( "cn=Ersin Er", entry );
            fail( "Entry addition with collective attribute should have failed." );
        }
        catch ( NamingException e )
        {
            // Intended execution point
        }
    }
    
    
    @Test
    public void testModifyRegularEntryAddingCollectiveAttribute() throws NamingException
    {
        Attributes entry = getTestEntry( "Ersin Er" );
        getSystemContext( service ).createSubcontext( "cn=Ersin Er", entry );
        Attributes changeSet = new AttributesImpl( "c-l", "Turkiye", true );
        try
        {
            
            getSystemContext( service ).modifyAttributes( "cn=Ersin Er", DirContext.ADD_ATTRIBUTE, changeSet );
            fail( "Collective attribute addition to non-collectiveAttributeSubentry should have failed." );
        }
        catch ( NamingException e )
        {
            // Intended execution point
        }
    }
    
    
    @Test
    public void testModifyRegularEntryAddingCollectiveAttribute2() throws NamingException
    {
        Attributes entry = getTestEntry( "Ersin Er" );
        getSystemContext( service ).createSubcontext( "cn=Ersin Er", entry );
        Attribute change = new AttributeImpl( "c-l", "Turkiye");
        ModificationItemImpl mod = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, change);
        try
        {
            getSystemContext( service ).modifyAttributes( "cn=Ersin Er", new ModificationItemImpl[] { mod } );
            fail( "Collective attribute addition to non-collectiveAttributeSubentry should have failed." );
        }
        catch ( NamingException e )
        {
            // Intended execution point
        }
    }
    
    
    @Test
    public void testPolymorphicReturnAttrLookup() throws Exception
    {
        // -------------------------------------------------------------------
        // Setup the collective attribute specific administration point
        // -------------------------------------------------------------------
    
        addAdministrativeRole( "collectiveAttributeSpecificArea" );
        getSystemContext( service ).createSubcontext( "cn=testsubentry", getTestSubentry() );
    
        // request the collective attribute's super type specifically
        Attributes attributes = getSystemContext( service ).getAttributes( "ou=interceptors,ou=configuration",
            new String[] { "ou" } );
        Attribute c_ou = attributes.get( "c-ou" );
        assertNotNull( "a collective c-ou attribute should be present", c_ou );
        assertTrue( c_ou.contains( "configuration" ) );
    }
    
}
