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
package org.apache.directory.server.dns;


import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.QuestionRecords;
import org.apache.directory.server.dns.messages.ResourceRecords;
import org.apache.mina.common.ByteBuffer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractDnsTestCase extends TestCase
{
    private final Logger log;
    private static final int MINIMUM_DNS_DATAGRAM_SIZE = 576;


    public AbstractDnsTestCase()
    {
        log = LoggerFactory.getLogger( AbstractDnsTestCase.class );
    }


    public AbstractDnsTestCase(Class subclass)
    {
        log = LoggerFactory.getLogger( subclass );
    }


    protected void print( DnsMessage message )
    {
        log.debug( String.valueOf( message.getTransactionId() ) );
        log.debug( String.valueOf( message.getMessageType() ) );
        log.debug( String.valueOf( message.getOpCode() ) );
        log.debug( String.valueOf( message.isAuthoritativeAnswer() ) );
        log.debug( String.valueOf( message.isTruncated() ) );
        log.debug( String.valueOf( message.isRecursionDesired() ) );
        log.debug( String.valueOf( message.isRecursionAvailable() ) );
        log.debug( String.valueOf( message.getResponseCode() ) );

        QuestionRecords questions = message.getQuestionRecords();
        log.debug( String.valueOf( questions ) );

        ResourceRecords records = message.getAnswerRecords();
        log.debug( String.valueOf( records ) );

        records = message.getAuthorityRecords();
        log.debug( String.valueOf( records ) );

        records = message.getAdditionalRecords();
        log.debug( String.valueOf( records ) );
    }


    protected ByteBuffer getByteBufferFromFile( String file ) throws IOException
    {
        InputStream is = getClass().getResourceAsStream( file );

        byte[] bytes = new byte[MINIMUM_DNS_DATAGRAM_SIZE];

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
