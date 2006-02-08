/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.ldap.message;


import junit.framework.TestCase;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.ldap.message.LockableAttributeImpl;


/**
 * Test cases for the methods of the LockableAttributeImpl class.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev$
 */
public class LockableAttributeImplTest extends TestCase
{
    /**
     * Creates and populates a LockableAttributeImpl instance for tests.
     */
    private LockableAttributeImpl getAttribute()
    {
        LockableAttributeImpl attr = new LockableAttributeImpl( "test-attr1" );
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
        LockableAttributeImpl attr = getAttribute();
        assertTrue( "same object should be equal", attr.equals( attr ) );
    }


    /**
     * Tests to see if two exact copies are equal.
     */
    public void testEqualsExactCopy()
    {
        LockableAttributeImpl attr0 = getAttribute();
        LockableAttributeImpl attr1 = getAttribute();
        assertTrue( "exact copies should be equal", attr0.equals( attr1 ) );
        assertTrue( "exact copies should be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different id.
     */
    public void testNotEqualDiffId()
    {
        LockableAttributeImpl attr0 = getAttribute();
        LockableAttributeImpl attr1 = new LockableAttributeImpl( "test-attr2" );
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
        LockableAttributeImpl attr0 = getAttribute();
        LockableAttributeImpl attr1 = new LockableAttributeImpl( "TEST-attr1" );
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
        LockableAttributeImpl attr0 = getAttribute();
        LockableAttributeImpl attr1 = new LockableAttributeImpl( "test-attr1" );
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
        LockableAttributeImpl attr0 = getAttribute();
        LockableAttributeImpl attr1 = new LockableAttributeImpl( "test-attr1" );
        attr1.add( "value0" );
        attr1.add( "value1" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertTrue( "Attributes with same values should be equal", attr0.equals( attr1 ) );
        assertTrue( "Attributes with same values should be equal", attr1.equals( attr0 ) );

        attr1.add( "value2" );
        assertFalse( "Attributes with different values should not be equal", attr0.equals( attr1 ) );
        assertFalse( "Attributes with different values should not be equal", attr1.equals( attr0 ) );
    }


    /**
     * Tests for inequality with different implementations.
     * 
     * @todo start looking at comparing syntaxes to determine if attributes are
     *       really equal
     */
    public void testNotEqualDiffImpl()
    {
        LockableAttributeImpl attr0 = getAttribute();
        Attribute attr1 = new BasicAttribute( "test-attr1" );
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
        LockableAttributeImpl attr = getAttribute();
        assertTrue( attr.contains( "value0" ) );
        assertTrue( attr.contains( "value1" ) );
        assertTrue( attr.contains( "value2" ) );
    }
}
