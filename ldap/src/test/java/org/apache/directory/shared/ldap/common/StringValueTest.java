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


import org.junit.Test;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

/**
 * A test for StringValue
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class StringValueTest
{
    /**
     * Test a null value
     */
    @Test public void testNullValue()
    {
        StringValue value = new StringValue();
        
        assertNull( value.getValue() );
    }
    

    /**
     * Test an empty value
     */
    @Test public void testEmptyValue()
    {
        StringValue value = new StringValue( "" );
        
        assertEquals( "", value.getValue() );
    }
    
    /**
     * Test the clone method
     */
    @Test public void testClone() throws CloneNotSupportedException
    {
        StringValue value = new StringValue( "Test1" );
        
        Value clone = (Value)value.clone();
        
        assertTrue( clone instanceof StringValue );
        assertTrue( clone.getValue() instanceof String );
        assertEquals( "Test1", clone.getValue() );
        assertEquals( value, clone );
        
        value.setValue( "Test2" );
        assertTrue( clone instanceof StringValue );
        assertTrue( clone.getValue() instanceof String );
        assertEquals( "Test1", clone.getValue() );
        assertNotSame( value, clone );
    }
    
    /**
     * Test StringValue equalities
     *
     */
    @Test public void testEquals()
    {
        StringValue value1 = new StringValue( "Test1" );
        StringValue value2 = new StringValue( "Test2" );
        StringValue value11 = new StringValue( "Test1" );
        
        assertNotSame( value1, value2 );
        assertEquals( value1, value1 );
        assertEquals( value1, value11 );
        
        value11.setValue( "Test2" );
        assertNotSame( value1, value11 );
        
        StringValue valueNull1 = new StringValue();
        StringValue valueNull2 = new StringValue();
        
        assertEquals( valueNull1, valueNull2 );
        assertNotSame( valueNull1, value1 );
    }
}
