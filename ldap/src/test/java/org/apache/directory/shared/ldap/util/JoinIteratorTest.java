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


import java.util.Collections;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.util.JoinIterator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;


/**
 * Document this class.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JoinIteratorTest
{
    @Test
    public void testNullArgument()
    {
        try
        {
            new JoinIterator( null );
            fail( "Should not be able to create a JoinIterator with null args" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
    }


    @Test
    public void testSingleArgument()
    {
        Iterator<?>[] iterators = new Iterator<?>[]
            { Collections.singleton( "foo" ).iterator() };

        try
        {
            new JoinIterator( iterators );
            fail( "Should not be able to create a JoinIterator with a single Iterator" );
        }
        catch ( IllegalArgumentException e )
        {
            assertNotNull( e );
        }
    }


    @Test
    public void testTwoArguments()
    {
        Iterator<?>[] iterators = new Iterator<?>[]
            { Collections.singleton( "foo" ).iterator(), Collections.singleton( "bar" ).iterator() };

        JoinIterator iterator = new JoinIterator( iterators );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "foo", iterator.next() );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "bar", iterator.next() );
        assertFalse( "iterator should NOT have an element", iterator.hasNext() );
    }


    @Test
    public void testSeveralArguments()
    {
        List<String> multivalued = new ArrayList<String>();
        multivalued.add( "foo1" );
        multivalued.add( "foo2" );

        Iterator<?>[] iterators = new Iterator<?>[]
            { Collections.singleton( "foo0" ).iterator(), multivalued.iterator(),
                Collections.singleton( "bar0" ).iterator(), Collections.singleton( "bar1" ).iterator() };

        JoinIterator iterator = new JoinIterator( iterators );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "foo0", iterator.next() );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "foo1", iterator.next() );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "foo2", iterator.next() );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "bar0", iterator.next() );
        assertTrue( "iterator should have an element", iterator.hasNext() );
        assertEquals( "bar1", iterator.next() );
        assertFalse( "iterator should NOT have an element", iterator.hasNext() );
    }
}
