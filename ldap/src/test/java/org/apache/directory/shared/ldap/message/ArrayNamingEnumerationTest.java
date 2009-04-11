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


import java.util.NoSuchElementException;

import org.apache.directory.shared.ldap.message.ArrayNamingEnumeration;
import org.apache.directory.shared.ldap.util.ArrayUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


/**
 * Tests the {@link ArrayNamingEnumeration} class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ArrayNamingEnumerationTest
{
    /**
     * Tests ArrayNamingEnumeration using an null array.
     */
    @Test
    public void testUsingNullArray()
    {
        ArrayNamingEnumeration<Object> list = new ArrayNamingEnumeration<Object>( null );
        assertFalse( list.hasMore() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch ( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests ArrayNamingEnumeration using an array with length = 0.
     */
    @Test
    public void testUsingEmptyArray()
    {
        ArrayNamingEnumeration<String> list = new ArrayNamingEnumeration<String>( ArrayUtils.EMPTY_STRING_ARRAY );
        assertFalse( list.hasMore() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch ( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests ArrayNamingEnumeration using an array with length = 1.
     */
    @Test
    public void testUsingSingleElementArray()
    {
        ArrayNamingEnumeration<String> list = new ArrayNamingEnumeration<String>( new String[]
            { "foo" } );
        assertTrue( list.hasMore() );
        assertEquals( "foo", list.next() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch ( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests ArrayNamingEnumeration using an array with length = 2.
     */
    @Test
    public void testUsingTwoElementArray()
    {
        ArrayNamingEnumeration<String> list = new ArrayNamingEnumeration<String>( new String[]
            { "foo", "bar" } );
        assertTrue( list.hasMore() );
        assertEquals( "foo", list.next() );
        assertTrue( list.hasMore() );
        assertEquals( "bar", list.next() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch ( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }
}
