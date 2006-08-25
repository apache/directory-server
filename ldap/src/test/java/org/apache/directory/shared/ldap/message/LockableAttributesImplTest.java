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


import junit.framework.TestCase;

import javax.naming.directory.BasicAttributes;

import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.util.ArrayUtils;

/**
 * Test cases for the methods of the LockableAttributeImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class LockableAttributesImplTest extends TestCase
{
    /**
     * Creates and populates a LockableAttributeImpl with a specific id.
     * 
     * @param id
     *            the id for the attribute
     * @return the LockableAttributeImpl assembled for testing
     */
    private LockableAttributeImpl getAttribute( String id )
    {
        LockableAttributeImpl attr = new LockableAttributeImpl( id );
        attr.add( "value0" );
        attr.add( "value1" );
        attr.add( "value2" );
        return attr;
    }


    /**
     * Creates and populates a LockableAttributes object
     * 
     * @return
     */
    private LockableAttributesImpl getAttributes()
    {
        LockableAttributesImpl attrs = new LockableAttributesImpl();
        attrs.put( getAttribute( "attr0" ) );
        attrs.put( getAttribute( "attr1" ) );
        attrs.put( getAttribute( "attr2" ) );
        return attrs;
    }


    /**
     * Tests that toString works properly.
     */
    public void testToString()
    {
        LockableAttributesImpl attrs = getAttributes();
        attrs.put( "binaryNullAttr", null );
        attrs.put( "binaryEmptyAttr", ArrayUtils.EMPTY_BYTE_ARRAY );
        attrs.put( "binaryFullAttr", new byte[]
            { 0x44, 0x23 } );
        String str = attrs.toString();
        assertTrue( str.indexOf( "binaryNullAttr" ) != -1 );
        assertTrue( str.indexOf( "binaryEmptyAttr" ) != -1 );
        assertTrue( str.indexOf( "binaryFullAttr" ) != -1 );
    }


    /**
     * Tests equality for the same object reference.
     */
    public void testEqualsSameObj()
    {
        LockableAttributesImpl attrs = getAttributes();
        assertTrue( "same object should return true", attrs.equals( attrs ) );
    }


    /**
     * Tests two exact replicas for equality.
     */
    public void testEqualsExactCopy()
    {
        LockableAttributesImpl attrs0 = getAttributes();
        LockableAttributesImpl attrs1 = getAttributes();
        assertTrue( "exact copies should be equal", attrs0.equals( attrs1 ) );
        assertTrue( "exact copies should be equal", attrs1.equals( attrs0 ) );
    }


    /**
     * Tests two exact copies with multiple copies of the same attribute added
     * for equality.
     */
    public void testEqualsExactCopyWithReplicas()
    {
        LockableAttributesImpl attrs0 = getAttributes();
        LockableAttributesImpl attrs1 = getAttributes();
        attrs1.put( getAttribute( "attr0" ) );
        attrs1.put( getAttribute( "attr0" ) );
        assertTrue( "exact copies with repeated adds should be equal", attrs0.equals( attrs1 ) );
        assertTrue( "exact copies with repeated adds should be equal", attrs1.equals( attrs0 ) );
    }


    /**
     * Test inequality with different attributes.
     */
    public void testNotEqualDiffAttr()
    {
        LockableAttributesImpl attrs0 = getAttributes();
        LockableAttributesImpl attrs1 = getAttributes();
        attrs1.put( getAttribute( "blah" ) );
        assertFalse( "different attributes should not be equal", attrs0.equals( attrs1 ) );
        assertFalse( "different attributes should not be equal", attrs1.equals( attrs0 ) );
    }


    /**
     * Tests equality of same attributes with different implementations.
     */
    public void testEqualsDiffImpl()
    {
        LockableAttributesImpl attrs0 = getAttributes();
        BasicAttributes attrs1 = new BasicAttributes( true );
        attrs1.put( getAttribute( "attr0" ) );
        attrs1.put( getAttribute( "attr1" ) );
        attrs1.put( getAttribute( "attr2" ) );

        assertTrue( "different implementations of the same " + "attributes should be equal", attrs0.equals( attrs1 ) );
        assertTrue( "different implementations of the same " + "attributes should be equal", attrs1.equals( attrs0 ) );
    }


    public void testCompareToBasicAttributes()
    {
        LockableAttributesImpl attrs0 = new LockableAttributesImpl();
        attrs0.put( "attr0", "value0" );
        attrs0.put( "attr1", "value1" );
        attrs0.put( "attr2", "value2" );
        attrs0.put( "attr2", "value3" );

        BasicAttributes attrs1 = new BasicAttributes( true );
        attrs1.put( "attr0", "value0" );
        attrs1.put( "attr1", "value1" );
        attrs1.put( "attr2", "value2" );
        attrs1.put( "attr2", "value3" );

        assertTrue( "both implementations should produce the same outcome", attrs0.equals( attrs1 ) );
    }
}
