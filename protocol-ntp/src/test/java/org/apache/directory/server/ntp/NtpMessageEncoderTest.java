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
package org.apache.directory.server.ntp;


import java.nio.ByteBuffer;

import junit.framework.TestCase;

import org.apache.directory.server.ntp.io.NtpMessageDecoder;
import org.apache.directory.server.ntp.io.NtpMessageEncoder;
import org.apache.directory.server.ntp.messages.LeapIndicatorType;
import org.apache.directory.server.ntp.messages.ModeType;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.apache.directory.server.ntp.messages.NtpMessageModifier;
import org.apache.directory.server.ntp.messages.NtpTimeStamp;
import org.apache.directory.server.ntp.messages.ReferenceIdentifier;
import org.apache.directory.server.ntp.messages.StratumType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class NtpMessageEncoderTest extends TestCase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( NtpMessageEncoderTest.class );


    /**
     * Tests the encoding of an NTP message.
     *
     * @throws Exception
     */
    public void testEncodeMessage() throws Exception
    {
        NtpMessageModifier modifier = new NtpMessageModifier();
        modifier.setLeapIndicator( LeapIndicatorType.NO_WARNING );
        modifier.setVersionNumber( 4 );
        modifier.setMode( ModeType.SERVER );
        modifier.setStratum( StratumType.PRIMARY_REFERENCE );
        modifier.setPollInterval( ( byte ) 0x06 ); // 6
        modifier.setPrecision( ( byte ) 0xFA ); // -6
        modifier.setRootDelay( 0 );
        modifier.setRootDispersion( 0 );
        modifier.setReferenceIdentifier( ReferenceIdentifier.LOCL );

        NtpTimeStamp now = new NtpTimeStamp();

        modifier.setReferenceTimestamp( now );
        modifier.setOriginateTimestamp( now );
        modifier.setReceiveTimestamp( now );
        modifier.setTransmitTimestamp( now );

        NtpMessage message = modifier.getNtpMessage();

        ByteBuffer replyByteBuffer = ByteBuffer.allocate( 1024 );

        NtpMessageEncoder encoder = new NtpMessageEncoder();
        encoder.encode( replyByteBuffer, message );

        print( message );

        NtpMessageDecoder decoder = new NtpMessageDecoder();
        NtpMessage reply = decoder.decode( replyByteBuffer );

        print( reply );
    }


    protected void print( NtpMessage request )
    {
        log.debug( String.valueOf( request.getLeapIndicator() ) );
        log.debug( String.valueOf( request.getVersionNumber() ) );
        log.debug( String.valueOf( request.getMode() ) );
        log.debug( String.valueOf( request.getStratum() ) );
        log.debug( String.valueOf( request.getPollInterval() ) );
        log.debug( String.valueOf( request.getPrecision() ) );
        log.debug( String.valueOf( request.getRootDelay() ) );
        log.debug( String.valueOf( request.getRootDispersion() ) );
        log.debug( String.valueOf( request.getReferenceIdentifier() ) );
        log.debug( String.valueOf( request.getReferenceTimestamp() ) );
        log.debug( String.valueOf( request.getOriginateTimestamp() ) );
        log.debug( String.valueOf( request.getReceiveTimestamp() ) );
        log.debug( String.valueOf( request.getTransmitTimestamp() ) );
    }
}
