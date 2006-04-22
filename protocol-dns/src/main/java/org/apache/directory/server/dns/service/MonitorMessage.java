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
package org.apache.directory.server.dns.service;


import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.MessageType;
import org.apache.directory.server.dns.messages.OpCode;
import org.apache.directory.server.dns.messages.QuestionRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.mina.handler.chain.IoHandlerCommand;


public abstract class MonitorMessage implements IoHandlerCommand
{
    protected String monitorMessage( DnsMessage message, String direction )
    {
        MessageType messageType = message.getMessageType();
        OpCode opCode = message.getOpCode();
        ResponseCode responseCode = message.getResponseCode();
        short transactionId = message.getTransactionId();

        StringBuffer sb = new StringBuffer();
        sb.append( "Monitoring " + direction + ":" );
        sb.append( "\n\t" + "messageType                " + messageType );
        sb.append( "\n\t" + "opCode                     " + opCode );
        sb.append( "\n\t" + "responseCode               " + responseCode );
        sb.append( "\n\t" + "transactionId              " + transactionId );

        sb.append( "\n\t" + "authoritativeAnswer        " + message.isAuthoritativeAnswer() );
        sb.append( "\n\t" + "truncated                  " + message.isTruncated() );
        sb.append( "\n\t" + "recursionDesired           " + message.isRecursionDesired() );
        sb.append( "\n\t" + "recursionAvailable         " + message.isRecursionAvailable() );
        sb.append( "\n\t" + "reserved                   " + message.isReserved() );
        sb.append( "\n\t" + "acceptNonAuthenticatedData " + message.isAcceptNonAuthenticatedData() );

        QuestionRecords questions = message.getQuestionRecords();

        sb.append( "\n\t" + "questions:                 " + questions );

        return sb.toString();
    }
}
