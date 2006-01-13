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
package org.apache.ldap.common.message;


import java.util.NoSuchElementException;

import junit.framework.TestCase;
import org.apache.ldap.common.util.ArrayUtils;


/**
 * Tests the {@link ArrayNamingEnumeration} class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ArrayNamingEnumerationTest extends TestCase
{
    /**
     * Tests ArrayNamingEnumeration using an null array.
     */
    public void testUsingNullArray()
    {
        ArrayNamingEnumeration list = new ArrayNamingEnumeration( null );
        assertFalse( list.hasMore() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests ArrayNamingEnumeration using an array with length = 0.
     */
    public void testUsingEmptyArray()
    {
        ArrayNamingEnumeration list = new ArrayNamingEnumeration( ArrayUtils.EMPTY_STRING_ARRAY );
        assertFalse( list.hasMore() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }


    /**
     * Tests ArrayNamingEnumeration using an array with length = 1.
     */
    public void testUsingSingleElementArray()
    {
        ArrayNamingEnumeration list;
        list = new ArrayNamingEnumeration( new String[] { "foo" });
        assertTrue( list.hasMore() );
        assertEquals( "foo", list.next() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }




    /**
     * Tests ArrayNamingEnumeration using an array with length = 2.
     */
    public void testUsingTwoElementArray()
    {
        ArrayNamingEnumeration list;
        list = new ArrayNamingEnumeration( new String[] { "foo", "bar" });
        assertTrue( list.hasMore() );
        assertEquals( "foo", list.next() );
        assertTrue( list.hasMore() );
        assertEquals( "bar", list.next() );

        try
        {
            list.next();
            fail( "should blow exception before getting here" );
        }
        catch( NoSuchElementException e )
        {
            assertNotNull( e );
        }
    }
}
