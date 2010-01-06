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
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


/**
 * Test the TelephoneNumber comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class TelephoneNumberComparatorTest
{
    private TelephoneNumberComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new TelephoneNumberComparator( null );
    }
    
    
    @Test
    public void testNullTelephoneNumbers()
    {
        String tel1 = null;
        String tel2 = null;
        
        assertEquals( 0, comparator.compare( tel1, tel2 ) );
        
        tel2 = "abc";
        assertEquals( -1, comparator.compare( tel1, tel2 ) );

        String tel3 = null;
        assertEquals( 1, comparator.compare( tel2, tel3 ) );
    }


    @Test
    public void testEmptyTelephoneNumbers()
    {
        String tel1 = "";
        String tel2 = "";
        
        assertEquals( 0, comparator.compare( tel1, tel2 ) );
        
        tel2 = "abc";
        assertTrue( comparator.compare( tel1, tel2 ) < 0 );

        String tel3 = "";
        assertTrue( comparator.compare( tel2, tel3 ) > 0 );
    }
    
    
    @Test
    public void testSimpleTelephoneNumbers()
    {
        String tel1 = "01 02 03 04 05";
        String tel2 = "01 02 03 04 05";
        
        assertEquals( 0, comparator.compare( tel1, tel2 ) );
        
        tel2 = "0102030405";
        assertEquals( 0, comparator.compare( tel1, tel2 ) );
    }
    
    
    @Test
    public void testComplexTelephoneNumbers()
    {
        String tel1 = "  + 33 1 01-02-03-04-05  ";
        String tel2 = "+3310102030405";
        
        assertEquals( 0, comparator.compare( tel1, tel2 ) );
        
        tel1 = "1-801-555-1212";
        tel2 = "18015551212";

        assertEquals( 0, comparator.compare( tel1, tel2 ) );
        assertEquals( 0, comparator.compare( "1 801 555 1212", tel1 ) );
        assertEquals( 0, comparator.compare( "1 801 555 1212", tel2 ) );
    }
}
