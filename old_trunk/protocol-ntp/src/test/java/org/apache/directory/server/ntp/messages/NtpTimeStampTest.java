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


import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import junit.framework.TestCase;


/**
 * Some unit tests for NtpTimeStamp.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: $, $Date:  $
 */
public class NtpTimeStampTest extends TestCase
{
    private static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat( "MM/dd/yyyy HH:mm:ss" );


    /**
     * equals() should return false for NtpTimeStamps of date objects with different values.
     */
    public void testEqualsForDifferent() throws ParseException
    {

        Date date = DATE_FORMAT.parse( "04/12/1971 11:23:14" );
        Date otherDate = DATE_FORMAT.parse( "12/24/2002 15:11:11" );

        assertFalse( date == otherDate );
        assertFalse( date.equals( otherDate ) );

        NtpTimeStamp timeStamp = new NtpTimeStamp( date );
        NtpTimeStamp otherTimeStamp = new NtpTimeStamp( otherDate );

        assertFalse( "equals()", timeStamp.equals( otherTimeStamp ) );
    }


    /**
     * equals() should return true for NtpTimeStamps of date objects with same value.
     */
    public void testEqualsForSame() throws ParseException
    {

        Date date = DATE_FORMAT.parse( "05/30/1978 04:11:02" );
        Date sameDate = DATE_FORMAT.parse( "05/30/1978 04:11:02" );

        assertFalse( date == sameDate );
        assertTrue( date.equals( sameDate ) );

        NtpTimeStamp timeStamp = new NtpTimeStamp( date );
        NtpTimeStamp otherTimeStamp = new NtpTimeStamp( sameDate );

        assertTrue( "equals()", timeStamp.equals( otherTimeStamp ) );

    }


    /**
     * A roundtrip of wrapping a date into a NtpTimeStamp object A, writing it to a buffer, and creating 
     * another NtpTimeStamp B from this buffer, should lead to objects A and B with the same content.
     */
    public void testWriteToBuffer() throws ParseException
    {
        Date date = DATE_FORMAT.parse( "04/12/1971 11:23:14" );

        NtpTimeStamp timeStampA = new NtpTimeStamp( date );

        ByteBuffer buffer = ByteBuffer.allocate( 1024 );
        timeStampA.writeTo( buffer );
        buffer.rewind();

        NtpTimeStamp timeStampB = new NtpTimeStamp( buffer );

        assertTrue( timeStampA.equals( timeStampB ) );
    }
}
