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

import org.apache.directory.shared.ldap.csn.CSN;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


/**
 * Test the CSN comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CSNComparatorTest
{
    private CSNComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new CSNComparator();
    }
    
    
    @Test
    public void testNullCSNs()
    {
        CSN csn1 = null;
        CSN csn2 = null;
        
        assertEquals( 0, comparator.compare( csn1, csn2 ) );
        
        csn2 = new CSN( System.currentTimeMillis(), 1, 1, 1 );
        assertEquals( -1, comparator.compare( csn1, csn2 ) );

        CSN csn3 = null;
        assertEquals( 1, comparator.compare( csn2, csn3 ) );
    }


    @Test
    public void testEqualsCSNs()
    {
        long t0 = System.currentTimeMillis();
        CSN csn1 = new CSN( t0, 0, 0, 0 );
        CSN csn2 = new CSN( t0, 0, 0, 0 );
        
        assertEquals( 0, comparator.compare( csn1, csn2 ) );
    }
    
    
    @Test
    public void testDifferentTimeStampCSNs()
    {
        long t0 = System.currentTimeMillis();
        long t1 = System.currentTimeMillis() + 1000;
        CSN csn1 = new CSN( t0, 0, 0, 0 );
        CSN csn2 = new CSN( t1, 0, 0, 0 );
        
        assertEquals( -1, comparator.compare( csn1, csn2 ) );
        assertEquals( 1, comparator.compare( csn2, csn1 ) );
    }
    
    
    @Test
    public void testDifferentChangeCountCSNs()
    {
        long t0 = System.currentTimeMillis();
        CSN csn1 = new CSN( t0, 0, 0, 0 );
        CSN csn2 = new CSN( t0, 1, 0, 0 );
        
        assertEquals( -1, comparator.compare( csn1, csn2 ) );
        assertEquals( 1, comparator.compare( csn2, csn1 ) );
    }
    
    
    @Test
    public void testDifferentReplicaIdCSNs()
    {
        long t0 = System.currentTimeMillis();
        CSN csn1 = new CSN( t0, 0, 0, 0 );
        CSN csn2 = new CSN( t0, 0, 1, 0 );
        
        assertEquals( -1, comparator.compare( csn1, csn2 ) );
        assertEquals( 1, comparator.compare( csn2, csn1 ) );
    }
    
    
    @Test
    public void testDifferentOperationNumberCSNs()
    {
        long t0 = System.currentTimeMillis();
        CSN csn1 = new CSN( t0, 0, 0, 0 );
        CSN csn2 = new CSN( t0, 0, 0, 1 );
        
        assertEquals( -1, comparator.compare( csn1, csn2 ) );
        assertEquals( 1, comparator.compare( csn2, csn1 ) );
    }
}
