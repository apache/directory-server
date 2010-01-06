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

import org.apache.directory.shared.ldap.csn.Csn;
import org.junit.Before;
import org.junit.Test;


/**
 * Test the CSN comparator
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnComparatorTest
{
    private CsnComparator comparator;
    
    @Before
    public void init()
    {
        comparator = new CsnComparator( null );
    }
    
    
    @Test
    public void testNullCSNs()
    {
        assertEquals( 0, comparator.compare( null, null ) );
        
        Csn csn2 = new Csn( System.currentTimeMillis(), 1, 1, 1 );
        assertEquals( -1, comparator.compare( null, csn2.toString() ) );

        assertEquals( 1, comparator.compare( csn2.toString(), null ) );
    }


    @Test
    public void testEqualsCSNs()
    {
        long t0 = System.currentTimeMillis();
        Csn csn1 = new Csn( t0, 0, 0, 0 );
        Csn csn2 = new Csn( t0, 0, 0, 0 );
        
        assertEquals( 0, comparator.compare( csn1.toString(), csn2.toString() ) );
    }
    
    
    @Test
    public void testDifferentTimeStampCSNs()
    {
        long t0 = System.currentTimeMillis();
        long t1 = System.currentTimeMillis() + 1000;
        Csn csn1 = new Csn( t0, 0, 0, 0 );
        Csn csn2 = new Csn( t1, 0, 0, 0 );
        
        assertEquals( -1, comparator.compare( csn1.toString(), csn2.toString() ) );
        assertEquals( 1, comparator.compare( csn2.toString(), csn1.toString() ) );
    }
    
    
    @Test
    public void testDifferentChangeCountCSNs()
    {
        long t0 = System.currentTimeMillis();
        Csn csn1 = new Csn( t0, 0, 0, 0 );
        Csn csn2 = new Csn( t0, 1, 0, 0 );
        
        assertEquals( -1, comparator.compare( csn1.toString(), csn2.toString() ) );
        assertEquals( 1, comparator.compare( csn2.toString(), csn1.toString() ) );
    }
    
    
    @Test
    public void testDifferentReplicaIdCSNs()
    {
        long t0 = System.currentTimeMillis();
        Csn csn1 = new Csn( t0, 0, 0, 0 );
        Csn csn2 = new Csn( t0, 0, 1, 0 );
        
        assertEquals( -1, comparator.compare( csn1.toString(), csn2.toString() ) );
        assertEquals( 1, comparator.compare( csn2.toString(), csn1.toString() ) );
    }
    
    
    @Test
    public void testDifferentOperationNumberCSNs()
    {
        long t0 = System.currentTimeMillis();
        Csn csn1 = new Csn( t0, 0, 0, 0 );
        Csn csn2 = new Csn( t0, 0, 0, 1 );
        
        assertEquals( -1, comparator.compare( csn1.toString(), csn2.toString() ) );
        assertEquals( 1, comparator.compare( csn2.toString(), csn1.toString() ) );
    }
}
