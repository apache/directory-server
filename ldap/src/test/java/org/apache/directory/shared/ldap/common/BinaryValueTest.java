/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.ldap.common;


import java.util.Arrays;

import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * A test for BinaryValue
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BinaryValueTest
{
    /**
     * Test a null value
     */
    @Test public void testNullValue()
    {
        BinaryValue value = new BinaryValue();
        
        assertNull( value.getValue() );
    }
    

    /**
     * Test an empty value
     */
    @Test public void testEmptyValue()
    {
        BinaryValue value = new BinaryValue( StringTools.EMPTY_BYTES );
        
        assertEquals( StringTools.EMPTY_BYTES, value.getValue() );
    }
    
    /**
     * Test the clone method
     */
    @Test public void testClone() throws CloneNotSupportedException
    {
        byte[] bytes = StringTools.getBytesUtf8( "Test1" );
        byte[] bytes2 = StringTools.getBytesUtf8( "Test2" );
        
        BinaryValue value = new BinaryValue( bytes );
        
        Value<?> clone = value.clone();
        
        assertTrue( clone instanceof BinaryValue );
        assertTrue( clone.getValue() instanceof byte[] );
        assertTrue( Arrays.equals( bytes, (byte[])clone.getValue() ) );
        assertEquals( value, clone );
        
        bytes[0] = 't';
        assertFalse( Arrays.equals( bytes, (byte[])clone.getValue() ) );
        
        bytes[0] = 'T';
        value.setValue( bytes2 );
        assertTrue( clone instanceof BinaryValue );
        assertTrue( clone.getValue() instanceof byte[] );
        assertTrue( Arrays.equals( bytes, (byte[])clone.getValue() ) );
        assertNotSame( value, clone );
    }
    
    /**
     * Test StringValue equalities
     *
     */
    @Test public void testEquals()
    {
        BinaryValue value1 = new BinaryValue( StringTools.getBytesUtf8( "Test1" ) );
        BinaryValue value2 = new BinaryValue( StringTools.getBytesUtf8( "Test2" ) );
        BinaryValue value11 = new BinaryValue( StringTools.getBytesUtf8( "Test1" ) );
        
        assertNotSame( value1, value2 );
        assertEquals( value1, value1 );
        assertEquals( value1, value11 );
        
        value11.setValue( StringTools.getBytesUtf8( "Test2" ) );
        assertNotSame( value1, value11 );
        
        BinaryValue valueNull1 = new BinaryValue();
        BinaryValue valueNull2 = new BinaryValue();
        
        assertEquals( valueNull1, valueNull2 );
        assertNotSame( valueNull1, value1 );
    }
}
