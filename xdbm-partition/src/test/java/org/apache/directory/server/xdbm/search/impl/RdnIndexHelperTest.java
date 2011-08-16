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
package org.apache.directory.server.xdbm.search.impl;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.junit.Test;


/**
 * Tests {@link RdnIndexHelper}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RdnIndexHelperTest extends TestBase
{

    @Test
    public void test_isDirectDescendantOf_directDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertTrue( helper.isDirectDescendantOf( 0L, 1L ) );
        assertTrue( helper.isDirectDescendantOf( 1L, 3L ) );
        assertTrue( helper.isDirectDescendantOf( 3L, 7L ) );
        assertTrue( helper.isDirectDescendantOf( 7L, 9L ) );
    }


    @Test
    public void test_isDirectDescendantOf_indirectDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDirectDescendantOf( 0L, 3L ) );
        assertFalse( helper.isDirectDescendantOf( 0L, 7L ) );
        assertFalse( helper.isDirectDescendantOf( 0L, 9L ) );
        assertFalse( helper.isDirectDescendantOf( 1L, 7L ) );
        assertFalse( helper.isDirectDescendantOf( 1L, 9L ) );
        assertFalse( helper.isDirectDescendantOf( 3L, 9L ) );
    }


    @Test
    public void test_isDirectDescendantOf_same() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDirectDescendantOf( 0L, 0L ) );
        assertFalse( helper.isDirectDescendantOf( 1L, 1L ) );
    }


    @Test
    public void test_isDirectDescendantOf_noDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDirectDescendantOf( 3L, 4L ) );
        assertFalse( helper.isDirectDescendantOf( 10L, 11L ) );
    }


    @Test(expected = IllegalArgumentException.class)
    public void test_isDirectDescendantOf_nonExistingIds() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDirectDescendantOf( -99L, -999L ) );
    }


    @Test
    public void test_isDescendantOf_directDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertTrue( helper.isDescendantOf( 0L, 1L ) );
        assertTrue( helper.isDescendantOf( 1L, 3L ) );
        assertTrue( helper.isDescendantOf( 3L, 7L ) );
        assertTrue( helper.isDescendantOf( 7L, 9L ) );
    }


    @Test
    public void test_isDescendantOf_indirectDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertTrue( helper.isDescendantOf( 0L, 3L ) );
        assertTrue( helper.isDescendantOf( 0L, 7L ) );
        assertTrue( helper.isDescendantOf( 0L, 9L ) );
        assertTrue( helper.isDescendantOf( 1L, 7L ) );
        assertTrue( helper.isDescendantOf( 1L, 9L ) );
        assertTrue( helper.isDescendantOf( 3L, 9L ) );
    }


    @Test
    public void test_isDescendantOf_same() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDescendantOf( 0L, 0L ) );
        assertFalse( helper.isDescendantOf( 1L, 1L ) );
    }


    @Test
    public void test_isDescendantOf_noDescendants() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDescendantOf( 3L, 4L ) );
        assertFalse( helper.isDescendantOf( 10L, 11L ) );
    }


    @Test(expected = IllegalArgumentException.class)
    public void test_isDescendantOf_nonExistingIds() throws Exception
    {
        RdnIndexHelper<Entry, Long> helper = new RdnIndexHelper<Entry, Long>( store.getRdnIndex(), store.getRootId() );

        assertFalse( helper.isDescendantOf( -99L, -999L ) );
    }

}