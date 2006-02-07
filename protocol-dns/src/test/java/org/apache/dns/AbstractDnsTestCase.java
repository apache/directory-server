/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.dns;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.dns.messages.DnsMessage;
import org.apache.dns.messages.QuestionRecords;
import org.apache.dns.messages.ResourceRecords;
import org.apache.mina.common.ByteBuffer;

public abstract class AbstractDnsTestCase extends TestCase
{
    private static final int MINIMUM_DNS_DATAGRAM_SIZE = 576;

    protected void print( DnsMessage message )
    {
        System.out.println( message.getTransactionId() );
        System.out.println( message.getMessageType() );
        System.out.println( message.getOpCode() );
        System.out.println( message.isAuthoritativeAnswer() );
        System.out.println( message.isTruncated() );
        System.out.println( message.isRecursionDesired() );
        System.out.println( message.isRecursionAvailable() );
        System.out.println( message.getResponseCode() );

        QuestionRecords questions = message.getQuestionRecords();
        System.out.println( questions );

        ResourceRecords records = message.getAnswerRecords();
        System.out.println( records );

        records = message.getAuthorityRecords();
        System.out.println( records );

        records = message.getAdditionalRecords();
        System.out.println( records );
    }

    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
    {
        InputStream is = getClass().getResourceAsStream( file );

        byte[] bytes = new byte[ MINIMUM_DNS_DATAGRAM_SIZE ];

        int offset = 0;
        int numRead = 0;
        while ( offset < bytes.length && ( numRead = is.read( bytes, offset, bytes.length - offset ) ) >= 0 )
        {
            offset += numRead;
        }

        is.close();

        return ByteBuffer.wrap( bytes );
    }
}
