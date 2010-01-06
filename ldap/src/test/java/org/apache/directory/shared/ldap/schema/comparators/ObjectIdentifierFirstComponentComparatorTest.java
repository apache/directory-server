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
package org.apache.directory.shared.ldap.schema.comparators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Before;
import org.junit.Test;


/**
 * Test the ObjectIdentifierFirstComponent comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectIdentifierFirstComponentComparatorTest
{
    private ObjectIdentifierFirstComponentComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new ObjectIdentifierFirstComponentComparator( null );
    }
    
    
    @Test
    public void testNullObjectIdentifiers()
    {
        assertEquals( 0, comparator.compare( null, null ) );
        
        String c2 = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        
        assertEquals( -1, comparator.compare( null, c2 ) );

        assertEquals( -1, comparator.compare( c2, null ) );
    }


    @Test
    public void testEqualsObjectIdentifiers()
    {
        String c1 = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        String c2 = "( 1.1 fqcn org.apache.directory.SimpleComparator bytecode ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        assertEquals( 0, comparator.compare( c1, c2 ) );

        String c3 = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== X-SCHEMA 'system' )";
        String c4 = "( 1.1 fqcn org.apache.directory.SimpleComparator bytecode ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        assertEquals( 0, comparator.compare( c3, c4 ) );
    }


    @Test
    public void testDifferentObjectIdentifiers()
    {
        String c1 = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        String c2 = "( 1.2 fqcn org.apache.directory.SimpleComparator bytecode ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        assertEquals( -1, comparator.compare( c1, c2 ) );

        String c3 = "( 1.1 FQCN org.apache.directory.SimpleComparator BYTECODE ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== X-SCHEMA 'system' )";
        String c4 = "( 1.1.1 fqcn org.apache.directory.SimpleComparator bytecode ABCDEFGHIJKLMNOPQRSTUVWXYZ+/abcdefghijklmnopqrstuvwxyz0123456789==== )";
        assertNotSame( 0, comparator.compare( c3, c4 ) );
    }
}
