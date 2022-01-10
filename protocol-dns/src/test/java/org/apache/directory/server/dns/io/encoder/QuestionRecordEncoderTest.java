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

package org.apache.directory.server.dns.io.encoder;


import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.directory.api.util.Strings;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Tests for the Question record encoder.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class QuestionRecordEncoderTest
{
    IoBuffer expectedData;

    QuestionRecordEncoder encoder;

    String name = "www.apache.org";
    String[] nameParts = name.split( "\\." );
    RecordType type = RecordType.A;
    RecordClass rClass = RecordClass.IN;

    QuestionRecord record = new QuestionRecord( name, type, rClass );


    @BeforeEach
    public void setUp()
    {
        encoder = new QuestionRecordEncoder();

        expectedData = IoBuffer.allocate( 128 );
        expectedData.put( ( byte ) nameParts[0].length() ); // 1
        expectedData.put( Strings.getBytesUtf8( nameParts[0] ) ); // + 3
        expectedData.put( ( byte ) nameParts[1].length() ); // + 1
        expectedData.put( Strings.getBytesUtf8( nameParts[1] ) ); // + 6
        expectedData.put( ( byte ) nameParts[2].length() ); // + 1
        expectedData.put( Strings.getBytesUtf8( nameParts[2] ) ); // + 3
        expectedData.put( ( byte ) 0x00 ); // + 1 = 16
        expectedData.putShort( type.convert() );
        expectedData.putShort( rClass.convert() );
    }


    @Test
    public void testEncode()
    {
        IoBuffer out = IoBuffer.allocate( 128 );
        encoder.put( out, record );
        assertEquals( expectedData, out );
    }
}
