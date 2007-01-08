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
package org.apache.directory.shared.ldap.message;


import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;

import junit.framework.TestCase;

import org.apache.commons.lang.ArrayUtils;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Test cases for the methods of the LockableAttributeImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class AttributeImplTest extends TestCase
{
    /**
     * Creates and populates a LockableAttributeImpl instance for tests.
     */
    private AttributeImpl getAttribute()
    {
        AttributeImpl attr = new AttributeImpl( "test-attr1" );
        attr.add( "value0" );
        attr.add( "value1" );
        attr.add( "value2" );
        return attr;
    }


    /**
     * Tests to see the same reference returns true on equals.
     */
    public void testEqualsSameObj()
    {
        AttributeImpl attr = getAttribute();
        assertTrue( "same object should be equal", attr.equals( attr ) );
    }


    /**
     * Tests to see if two exact copies are equal.
     */
    public void testEqualsExactCopy()
    {
        AttributeImpl attr0 = getAttribute();
        AttributeImpl attr1 = getAttribute();
        assertTrue( "exact copies should be equal", attr0.equals( attr1 ) );
        assertTrue( "exact copies should be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different id.
     */
    public void testNotEqualDiffId()
    {
        AttributeImpl attr0 = getAttribute();
        AttributeImpl attr1 = new AttributeImpl( "test-attr2" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        attr1.add( "value2" );
        assertFalse( "Attributes with different ids should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different ids should not be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different id with only case differences.
     */
    public void testNotEqualDiffCasedId()
    {
        AttributeImpl attr0 = getAttribute();
        AttributeImpl attr1 = new AttributeImpl( "TEST-attr1" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        attr1.add( "value2" );
        assertFalse( "Attributes with different id case should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different id case should not be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different values.
     */
    public void testNotEqualDiffValues()
    {
        AttributeImpl attr0 = getAttribute();
        AttributeImpl attr1 = new AttributeImpl( "test-attr1" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertTrue( "Attributes with same values should be equal", attr0.equals( attr1 ) );
        assertTrue( "Attributes with same values should be equal", attr1.equals( attr0 ) );

        attr1.add( "value3" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with same values but different number of replicated
     * values.
     */
    public void testNotEqualWithReplicatedValues()
    {
        AttributeImpl attr0 = getAttribute();
        AttributeImpl attr1 = new AttributeImpl( "test-attr1" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertTrue( "Attributes with same values should be equal", attr0.equals( attr1 ) );
        assertTrue( "Attributes with same values should be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertTrue( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertTrue( "Attributes with different values should not be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different implementations.
     * 
     * @todo start looking at comparing syntaxes to determine if attributes are
     *       really equal
     */
    public void testNotEqualDiffImpl()
    {
        AttributeImpl attr0 = getAttribute();
        Attribute attr1 = new AttributeImpl( "test-attr1" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertTrue( "Attributes with same values diff impl should be equal", attr0.equals( attr1 ) );

        // assertTrue( "Attributes with same values diff impl should be equal",
        // attr1.equals( attr0 ) );

        attr1.add( "value3" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );
    }


    public void testContains()
    {
        AttributeImpl attr = getAttribute();
        assertTrue( attr.contains( "value0" ) );
        assertTrue( attr.contains( "value1" ) );
        assertTrue( attr.contains( "value2" ) );
    }
    
    // Test the clone operation
    public void testCloneAttribute() throws NamingException
    {
        Attribute attr = new AttributeImpl( "test" );

        String zero = "zero";
        attr.add( zero );

        String one = "one";
        attr.add( one );
        
        byte[] two = StringTools.getBytesUtf8( "two" );
        attr.add( two );

        byte[] three = StringTools.getBytesUtf8( "three" );
        attr.add( three );
        
        Object[] allValues = new Object[] { zero, one, two, three };

        Attribute clone = (Attribute)attr.clone();
        
        // Test the atomic elements
        assertTrue( clone instanceof AttributeImpl );
        assertEquals( 4, clone.size() );
        assertEquals( "test", clone.getID() );
        
        // Now test the values
        NamingEnumeration values = clone.getAll();
        
        int i = 0;
        
        while ( values.hasMoreElements() )
        {
            Object value = values.next();
            
            if ( value instanceof String )
            {
                assertEquals( allValues[i++], value );
            }
            else
            {
                byte[] v = (byte[])value;
                
                // The content should be equal
                assertTrue( ArrayUtils.isEquals( allValues[i], v ) );
                
                // but not the container
                assertNotSame( allValues[i++], value );
            }
        }
        
        // Check that if we change the content, the cloned attribute
        // is still the same.
        two[1] = 'o';
        attr.set( 2, two );
        
        // The initial attribute should be modified
        Object attrTwo = attr.get( 2 );
        assertNotSame( two, attrTwo );
        assertTrue( ArrayUtils.isEquals( two, attrTwo ) );
        
        // but the cloned attribute should remain the same
        Object clonedTwo = clone.get( 2 );
        assertNotSame( two, clonedTwo );
        assertFalse( ArrayUtils.isEquals( two, clonedTwo ) );
        
        // Remove a value from the original attribute
        three = (byte[])attr.remove( 3 );
        
        // Check that it does not have modified the cloned attribute
        assertEquals( 4, clone.size() );
        
        // The content should be equal
        assertTrue( ArrayUtils.isEquals( three, clone.get( 3 ) ) );
        
        // but not the container
        assertNotSame( three, clone.get( 3 ) );
    }
    
    public void testEquals()
    {
        Attribute attr = new AttributeImpl( "test" );

        String zero = "zero";
        attr.add( zero );

        String one = "one";
        attr.add( one );
        
        byte[] two = StringTools.getBytesUtf8( "two" );
        attr.add( two );

        byte[] three = StringTools.getBytesUtf8( "three" );
        attr.add( three );
        
        Attribute clone = (Attribute)attr.clone();
        
        // Check that both attributes are equals
        assertTrue( attr.equals( clone ) );

        clone.set(  3, "three" );
        assertFalse( attr.equals(  clone  ) );

        two[1] = 'o';
        attr.set( 2, two );
        
        assertFalse( attr.equals(  clone  ) );

        attr.set( 2, "two" );
        assertFalse( attr.equals(  clone  ) );
    }
}
