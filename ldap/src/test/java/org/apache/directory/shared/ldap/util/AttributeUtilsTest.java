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
package org.apache.directory.shared.ldap.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.junit.Test;

/**
 * A test case for the AttributeUtils methods 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeUtilsTest
{
    /**
     * Test a addModification applied to an empty entry
     */
    @Test
    public void testApplyAddModificationToEmptyEntry() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        Modification modification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get(  "cn" ) );
        assertEquals( 1, entry.size() );
        assertEquals( attr, entry.get( "cn" ) );
    }


    /**
     * Test a addModification applied to an entry 
     */
    @Test
    public void testApplyAddModificationToEntry() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.add( "dc", "apache" );
        assertEquals( 1, entry.size() );

        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        Modification modification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );

        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get(  "cn" ) );
        assertEquals( 2, entry.size() );
        assertEquals( attr, entry.get( "cn" ) );
    }


    /**
     * Test a addModification applied to an entry with the same attribute
     * but with another value 
     */
    @Test
    public void testApplyAddModificationToEntryWithValues() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "cn", "apache" );
        assertEquals( 1, entry.size() );

        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        Modification modification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get(  "cn" ) );
        assertEquals( 1, entry.size() );
        
        EntryAttribute attribute = entry.get( "cn" );
        
        assertTrue( attribute.size() != 0 );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "apache" );
        expectedValues.add( "test" );
        
        for ( Value<?> value:attribute )
        {
            String valueStr = value.getString();
            
            assertTrue( expectedValues.contains( valueStr ) );
            
            expectedValues.remove( valueStr );
        }
        
        assertEquals( 0, expectedValues.size() );
    }


    /**
     * Test a addModification applied to an entry with the same attribute
     * and the same value 
     */
    @Test
    public void testApplyAddModificationToEntryWithSameValue() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "cn", "test", "apache" );
        assertEquals( 1, entry.size() );

        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        Modification modification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.size() );
        
        EntryAttribute cnAttr = entry.get( "cn" );
        
        assertTrue( cnAttr.size() != 0 );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "apache" );
        expectedValues.add( "test" );
        
        for ( Value<?> value:cnAttr )
        {
            String valueStr = value.getString();
            
            assertTrue( expectedValues.contains( valueStr ) );
            
            expectedValues.remove( valueStr );
        }
        
        assertEquals( 0, expectedValues.size() );
    }

    
    /**
     * Test the deletion of an attribute into an empty entry
     */
    @Test
    public void testApplyRemoveModificationFromEmptyEntry() throws NamingException
    {
        Entry entry = new DefaultClientEntry();

        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );

        Modification modification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNull( entry.get( "cn" ) );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test the deletion of an attribute into an entry which does not contain the attribute
     */
    @Test
    public void testApplyRemoveModificationFromEntryAttributeNotPresent() throws NamingException
    {
        Entry entry = new DefaultClientEntry();

        EntryAttribute dc = new DefaultClientAttribute( "dc", "apache" );
        entry.put( dc );
        
        EntryAttribute cn = new DefaultClientAttribute( "cn", "test" );
        
        Modification modification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, cn );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertNull( entry.get( "cn" ) );
        assertNotNull( entry.get( "dc" ) );
        assertEquals( 1, entry.size() );
        assertEquals( dc, entry.get( "dc" ) );
    }


    /**
     * Test the deletion of an attribute into an entry which contains the attribute
     * but without the value to be deleted
     */
    @Test
    public void testApplyRemoveModificationFromEntryAttributeNotSameValue() throws NamingException
    {
        Entry entry = new DefaultClientEntry();

        EntryAttribute cn = new DefaultClientAttribute( "cn", "apache" );
        entry.put( cn );
        
        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        
        Modification modification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.size() );
        assertEquals( cn, entry.get( "cn" ) );
    }


    /**
     * Test the deletion of an attribute into an entry which contains the attribute.
     * 
     * The entry should not contain the attribute after the operation
     */
    @Test
    public void testApplyRemoveModificationFromEntrySameAttributeSameValue() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "cn", "test" );
        
        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );

        Modification modification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertNull( entry.get( "cn" ) );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test the deletion of an attribute into an entry which contains the attribute
     * with more than one value
     * 
     * The entry should contain the attribute after the operation, but with one less value
     */
    @Test
    public void testApplyRemoveModificationFromEntrySameAttributeValues() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "cn", "test", "apache" );
        
        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );
        
        Modification modification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE, attr );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.size() );
        
        EntryAttribute modifiedAttr = entry.get( "cn" );
        
        assertTrue( modifiedAttr.size() != 0 );
        
        boolean isFirst = true;
        
        for ( Value<?> value:modifiedAttr )
        {
            assertTrue( isFirst );
            
            isFirst = false;
            assertEquals( "apache", value.getString() );
        }
    }
    
    /**
     * test the addition by modification of an attribute in an empty entry.
     * 
     * As we are replacing a non existing attribute, it should be added.
     *
     * @throws NamingException
     */
    @Test
    public void testApplyModifyModificationFromEmptyEntry() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        
        EntryAttribute attr = new DefaultClientAttribute( "cn", "test" );

        
        Modification modification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.size() );
    }

    
    /**
     * Test the replacement by modification of an attribute in an empty entry.
     * 
     * As we are replacing a non existing attribute, it should not change the entry.
     *
     * @throws NamingException
     */
    @Test
    public void testApplyModifyEmptyModificationFromEmptyEntry() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        
        EntryAttribute attr = new DefaultClientAttribute( "cn" );

        Modification modification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNull( entry.get( "cn" ) );
        assertEquals( 0, entry.size() );
    }


    /**
     * Test the replacement by modification of an attribute in an empty entry.
     * 
     * As we are replacing a non existing attribute, it should not change the entry.
     *
     * @throws NamingException
     */
    @Test
    public void testApplyModifyAttributeModification() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put( "cn", "test" );
        entry.put( "ou", "apache", "acme corp" );
        
        EntryAttribute newOu = new DefaultClientAttribute( "ou", "Big Company", "directory" );
        
        Modification modification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, newOu );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertEquals( 2, entry.size() );
        
        assertNotNull( entry.get( "cn" ) );
        assertNotNull( entry.get( "ou" ) );
        
        EntryAttribute modifiedAttr = entry.get( "ou" );
        
        assertTrue( modifiedAttr.size() != 0 );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "Big Company" );
        expectedValues.add( "directory" );

        for ( Value<?> value:modifiedAttr )
        {
            String valueStr = value.getString();
            
            assertTrue( expectedValues.contains( valueStr ) );
            
            expectedValues.remove( valueStr );
        }
        
        assertEquals( 0, expectedValues.size() );
    }


    /**
     * Test the removing by modification of an existing attribute in an .
     * 
     * @throws NamingException
     */
    @Test
    public void testApplyModifyModificationRemoveAttribute() throws NamingException
    {
        Entry entry = new DefaultClientEntry();
        entry.put(  "cn", "test" );
        entry.put( "ou", "apache", "acme corp" );
        
        EntryAttribute newOu = new DefaultClientAttribute( "ou" );
        
        Modification modification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, newOu );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertEquals( 1, entry.size() );
        
        assertNotNull( entry.get( "cn" ) );
        assertNull( entry.get( "ou" ) );
    }
    
    @Test
    public void testCreateAttributesVarargs() throws NamingException
    {
        String mOid = "m-oid: 1.2.3.4";
        String description = "description";
        
        Attributes attrs = AttributeUtils.createAttributes( 
            "objectClass: top",
            "objectClass: metaTop",
            "objectClass: metaSyntax",
            mOid,
            "m-description", description );
        
        assertEquals( "top", attrs.get( "objectClass" ).get( 0 ) );
        assertEquals( "metaTop", attrs.get( "objectClass" ).get( 1 ) );
        assertEquals( "metaSyntax", attrs.get( "objectClass" ).get( 2 ) );
        assertEquals( "1.2.3.4", attrs.get( "m-oid" ).get() );
        assertEquals( "description", attrs.get( "m-description" ).get() );

        try
        {
            AttributeUtils.createAttributes( 
                "objectClass", "top",
                "objectClass" );
            fail();
        }
        catch ( InvalidAttributeValueException iave )
        {
            assertTrue( true );
        }
    }
}

