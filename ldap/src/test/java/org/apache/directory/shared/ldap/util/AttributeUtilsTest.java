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

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

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
        Attributes entry = new AttributesImpl();
        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr );
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
        Attributes entry = new AttributesImpl();
        entry.put( "dc", "apache" );
        assertEquals( 1, entry.size() );

        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr );
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
        Attributes entry = new AttributesImpl();
        entry.put( "cn", "apache" );
        assertEquals( 1, entry.size() );

        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get(  "cn" ) );
        assertEquals( 1, entry.size() );
        
        NamingEnumeration<?> values = entry.get( "cn" ).getAll();
        
        assertTrue( values.hasMoreElements() );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "apache" );
        expectedValues.add( "test" );
        
        while ( values.hasMoreElements() )
        {
            String value = (String)values.nextElement();
            
            assertTrue( expectedValues.contains( value ) );
            
            expectedValues.remove( value );
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn" );
        cn.add( "test" );
        cn.add( "apache" );
        entry.put( cn );
        
        assertEquals( 1, entry.size() );

        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.ADD_ATTRIBUTE, attr );
        AttributeUtils.applyModification( entry, modification );
        assertNotNull( entry.get(  "cn" ) );
        assertEquals( 1, entry.size() );
        
        NamingEnumeration<?> values = entry.get( "cn" ).getAll();
        
        assertTrue( values.hasMoreElements() );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "apache" );
        expectedValues.add( "test" );
        
        while ( values.hasMoreElements() )
        {
            String value = (String)values.nextElement();
            
            assertTrue( expectedValues.contains( value ) );
            
            expectedValues.remove( value );
        }
        
        assertEquals( 0, expectedValues.size() );
    }

    
    /**
     * Test the deletion of an attribute into an empty entry
     */
    @Test
    public void testApplyRemoveModificationFromEmptyEntry() throws NamingException
    {
        Attributes entry = new AttributesImpl();
        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.REMOVE_ATTRIBUTE, attr );
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
        Attributes entry = new AttributesImpl();
        Attribute dc = new AttributeImpl( "dc", "apache" );
        entry.put( dc );
        
        Attribute attr = new AttributeImpl( "cn", "test" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REMOVE_ATTRIBUTE, attr );
        
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn", "apache" );
        entry.put( cn );
        
        Attribute attr = new AttributeImpl( "cn", "test" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REMOVE_ATTRIBUTE, attr );
        
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn", "test" );
        entry.put( cn );
        
        Attribute attr = new AttributeImpl( "cn", "test" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REMOVE_ATTRIBUTE, attr );
        
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn", "test" );
        cn.add( "apache" );
        entry.put( cn );
        
        Attribute attr = new AttributeImpl( "cn", "test" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REMOVE_ATTRIBUTE, attr );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertNotNull( entry.get( "cn" ) );
        assertEquals( 1, entry.size() );
        
        Attribute modifiedAttr = entry.get( "cn" );
        
        NamingEnumeration<?> values = modifiedAttr.getAll();
        
        assertTrue( values.hasMoreElements() );
        
        boolean isFirst = true;
        
        while ( values.hasMoreElements() )
        {
            assertTrue( isFirst );
            
            isFirst = false;
            assertEquals( "apache", values.nextElement() );
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
        Attributes entry = new AttributesImpl();
        Attribute attr = new AttributeImpl( "cn", "test" );
        ModificationItem modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, attr );
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
        Attributes entry = new AttributesImpl();
        Attribute attr = new AttributeImpl( "cn" );
        ModificationItem modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, attr );
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn", "test" );
        
        entry.put( cn );

        Attribute ou = new AttributeImpl( "ou" );
        ou.add( "apache" );
        ou.add( "acme corp" );
        
        entry.put( ou );
        
        Attribute newOu = new AttributeImpl( "ou" );
        newOu.add( "Big Company" );
        newOu.add( "directory" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, newOu );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertEquals( 2, entry.size() );
        
        assertNotNull( entry.get( "cn" ) );
        assertNotNull( entry.get( "ou" ) );
        
        Attribute modifiedAttr = entry.get( "ou" );
        
        NamingEnumeration<?> values = modifiedAttr.getAll();
        
        assertTrue( values.hasMoreElements() );
        
        Set<String> expectedValues = new HashSet<String>();
        expectedValues.add( "Big Company" );
        expectedValues.add( "directory" );
        
        while ( values.hasMoreElements() )
        {
            String value = (String)values.nextElement();
            
            assertTrue( expectedValues.contains( value ) );
            
            expectedValues.remove( value );
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
        Attributes entry = new AttributesImpl();
        Attribute cn = new AttributeImpl( "cn", "test" );
        
        entry.put( cn );

        Attribute ou = new AttributeImpl( "ou" );
        ou.add( "apache" );
        ou.add( "acme corp" );
        
        entry.put( ou );
        
        Attribute newOu = new AttributeImpl( "ou" );
        
        ModificationItem modification = new ModificationItemImpl(DirContext.REPLACE_ATTRIBUTE, newOu );
        
        AttributeUtils.applyModification( entry, modification );
        
        assertEquals( 1, entry.size() );
        
        assertNotNull( entry.get( "cn" ) );
        assertNull( entry.get( "ou" ) );
    }
}

