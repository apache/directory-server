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
package org.apache.directory.server.core.entry;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.name.Dn;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Unit tests class ClonedServerEntry.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class ClonedServerEntryTest
{
    private static ClonedServerEntry clonedServerEntryA;
    private static ClonedServerEntry clonedServerEntryACopy;
    private static ClonedServerEntry clonedServerEntryB;
    private static ClonedServerEntry clonedServerEntryA1;
    private static ClonedServerEntry clonedServerEntryACopy1;
    private static ClonedServerEntry clonedServerEntryB1;
    private static ClonedServerEntry clonedServerEntryC1;

    /**
     * Initialize name instances
     */
    @BeforeClass
    public static void initNames() throws Exception
    {
        Entry eA = new DefaultEntry(new Dn( "dc=example,dc=com" ));
        Entry eB = new DefaultEntry(new Dn( "dc=example,dc=com" ));
        Entry eC = new DefaultEntry(new Dn( "dc=test,dc=org" ));

        clonedServerEntryA = new ClonedServerEntry();
        clonedServerEntryACopy = new ClonedServerEntry();
        clonedServerEntryB = new ClonedServerEntry();
        clonedServerEntryA1 = new ClonedServerEntry( eA );
        clonedServerEntryACopy1 = new ClonedServerEntry( eA );
        clonedServerEntryB1 = new ClonedServerEntry( eB );
        clonedServerEntryC1 = new ClonedServerEntry( eC );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( clonedServerEntryA.equals( null ) );
        assertFalse( clonedServerEntryA1.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryA1 );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryA );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryA1 );
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryA.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryA1.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA, clonedServerEntryACopy );
        assertEquals( clonedServerEntryACopy, clonedServerEntryB );
        assertEquals( clonedServerEntryA, clonedServerEntryB );
        assertEquals( clonedServerEntryA1, clonedServerEntryACopy1 );
        assertEquals( clonedServerEntryACopy1, clonedServerEntryB1 );
        assertEquals( clonedServerEntryA1, clonedServerEntryB1 );
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryACopy.hashCode() );
        assertEquals( clonedServerEntryACopy.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA.hashCode(), clonedServerEntryB.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryACopy1.hashCode() );
        assertEquals( clonedServerEntryACopy1.hashCode(), clonedServerEntryB1.hashCode() );
        assertEquals( clonedServerEntryA1.hashCode(), clonedServerEntryB1.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( clonedServerEntryA1.equals( clonedServerEntryC1 ) );
        assertFalse( clonedServerEntryC1.equals( clonedServerEntryA1 ) );
    }
}
