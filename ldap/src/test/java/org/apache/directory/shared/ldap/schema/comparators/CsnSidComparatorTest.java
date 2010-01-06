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

import org.junit.Before;
import org.junit.Test;


/**
 * Test the CSN comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnSidComparatorTest
{
    private CsnSidComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new CsnSidComparator( null );
    }
    
    
    @Test
    public void testNullSids()
    {
        assertEquals( 0, comparator.compare( null, null ) );
        
        assertEquals( -1, comparator.compare( null, "000" ) );

        assertEquals( 1, comparator.compare( "000", null ) );
    }


    @Test
    public void testEqualsSids()
    {
        assertEquals( 0, comparator.compare( "000", "000" ) );
        assertEquals( 0, comparator.compare( "000", "0" ) );
        assertEquals( 0, comparator.compare( "fff", "fff" ) );
    }
    
    
    
    @Test
    public void testDifferentSids()
    {
        assertEquals( -1, comparator.compare( "123", "456" ) );
        assertEquals( 1, comparator.compare( "FFF", "000" ) );
        assertEquals( 1, comparator.compare( "FFF", "GGG" ) );
    }
}
