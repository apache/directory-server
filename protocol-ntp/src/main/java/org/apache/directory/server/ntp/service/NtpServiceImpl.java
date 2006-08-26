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

package org.apache.directory.server.ntp.service;


import org.apache.directory.server.ntp.NtpService;
import org.apache.directory.server.ntp.messages.LeapIndicatorType;
import org.apache.directory.server.ntp.messages.ModeType;
import org.apache.directory.server.ntp.messages.NtpMessage;
import org.apache.directory.server.ntp.messages.NtpMessageModifier;
import org.apache.directory.server.ntp.messages.NtpTimeStamp;
import org.apache.directory.server.ntp.messages.ReferenceIdentifier;
import org.apache.directory.server.ntp.messages.StratumType;


public class NtpServiceImpl implements NtpService
{
    public NtpMessage getReplyFor( NtpMessage request )
    {
        NtpMessageModifier modifier = new NtpMessageModifier();

        modifier.setLeapIndicator( LeapIndicatorType.NO_WARNING );
        modifier.setVersionNumber( 4 );
        modifier.setMode( ModeType.SERVER );
        modifier.setStratum( StratumType.PRIMARY_REFERENCE );
        modifier.setPollInterval( ( byte ) 0x04 );
        modifier.setPrecision( ( byte ) 0xFA );
        modifier.setRootDelay( 0 );
        modifier.setRootDispersion( 0 );
        modifier.setReferenceIdentifier( ReferenceIdentifier.LOCL );

        NtpTimeStamp now = new NtpTimeStamp();

        modifier.setReferenceTimestamp( now );
        modifier.setOriginateTimestamp( request.getTransmitTimestamp() );
        modifier.setReceiveTimestamp( request.getReceiveTimestamp() );
        modifier.setTransmitTimestamp( now );

        return modifier.getNtpMessage();
    }
}
