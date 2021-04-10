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
package org.apache.directory.server.ntp.messages;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Unit tests class NtpTimeStamp.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NtpTimeStampTest
{
    NtpTimeStamp ntpTimeStampA;
    NtpTimeStamp ntpTimeStampA1;
    NtpTimeStamp ntpTimeStampACopy1;
    NtpTimeStamp ntpTimeStampB1;
    NtpTimeStamp ntpTimeStampC1;


    /**
     * Initialize name instances
     */
    @BeforeEach
    public void initNames() throws Exception
    {
        ntpTimeStampA = new NtpTimeStamp();
        Date date = new Date( 1L );
        ntpTimeStampA1 = new NtpTimeStamp( date );
        ntpTimeStampACopy1 = new NtpTimeStamp( date );
        ntpTimeStampB1 = new NtpTimeStamp( date );
        ntpTimeStampC1 = new NtpTimeStamp( new Date( 2L ) );
    }


    @Test
    public void testEqualsNull() throws Exception
    {
        assertFalse( ntpTimeStampA.equals( null ) );
        assertFalse( ntpTimeStampA1.equals( null ) );
    }


    @Test
    public void testEqualsReflexive() throws Exception
    {
        assertEquals( ntpTimeStampA, ntpTimeStampA );
        assertEquals( ntpTimeStampA1, ntpTimeStampA1 );
    }


    @Test
    public void testHashCodeReflexive() throws Exception
    {
        assertEquals( ntpTimeStampA.hashCode(), ntpTimeStampA.hashCode() );
        assertEquals( ntpTimeStampA1.hashCode(), ntpTimeStampA1.hashCode() );
    }


    @Test
    public void testEqualsSymmetric() throws Exception
    {
        assertEquals( ntpTimeStampA1, ntpTimeStampACopy1 );
        assertEquals( ntpTimeStampACopy1, ntpTimeStampA1 );
    }


    @Test
    public void testHashCodeSymmetric() throws Exception
    {
        assertEquals( ntpTimeStampA1.hashCode(), ntpTimeStampACopy1.hashCode() );
        assertEquals( ntpTimeStampACopy1.hashCode(), ntpTimeStampA1.hashCode() );
    }


    @Test
    public void testEqualsTransitive() throws Exception
    {
        assertEquals( ntpTimeStampA1, ntpTimeStampACopy1 );
        assertEquals( ntpTimeStampACopy1, ntpTimeStampB1 );
        assertEquals( ntpTimeStampA1, ntpTimeStampB1 );
    }


    @Test
    public void testHashCodeTransitive() throws Exception
    {
        assertEquals( ntpTimeStampA1.hashCode(), ntpTimeStampACopy1.hashCode() );
        assertEquals( ntpTimeStampACopy1.hashCode(), ntpTimeStampB1.hashCode() );
        assertEquals( ntpTimeStampA1.hashCode(), ntpTimeStampB1.hashCode() );
    }


    @Test
    public void testNotEqualDiffValue() throws Exception
    {
        assertFalse( ntpTimeStampA1.equals( ntpTimeStampC1 ) );
        assertFalse( ntpTimeStampC1.equals( ntpTimeStampA1 ) );
    }
}
