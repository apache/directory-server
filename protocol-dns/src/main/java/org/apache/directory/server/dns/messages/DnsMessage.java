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

package org.apache.directory.server.dns.messages;


import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


/**
 * All communications inside of the domain protocol are carried in a single
 * format called a message.  The top level format of message is divided
 * into 5 sections (some of which are empty in certain cases) shown below:
 *
 *     +---------------------+
 *     |        Header       |
 *     +---------------------+
 *     |       Question      | the question for the name server
 *     +---------------------+
 *     |        Answer       | ResourceRecords answering the question
 *     +---------------------+
 *     |      Authority      | ResourceRecords pointing toward an authority
 *     +---------------------+
 *     |      Additional     | ResourceRecords holding additional information
 *     +---------------------+
 */
public class DnsMessage
{
    /**
     * The header section is always present.  The header includes fields that
     * specify which of the remaining sections are present, and also specify
     * whether the message is a query or a response, a standard query or some
     * other opcode, etc.
     */
    private int transactionId;
    private MessageType messageType;
    private OpCode opCode;
    private boolean authoritativeAnswer;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private boolean reserved;
    private boolean acceptNonAuthenticatedData;

    private ResponseCode responseCode;

    private List<QuestionRecord> questionRecords;
    private List<ResourceRecord> answerRecords;
    private List<ResourceRecord> authorityRecords;
    private List<ResourceRecord> additionalRecords;


    public DnsMessage( int transactionId, MessageType messageType, OpCode opCode, boolean authoritativeAnswer,
        boolean truncated, boolean recursionDesired, boolean recursionAvailable, boolean reserved,
        boolean acceptNonAuthenticatedData, ResponseCode responseCode, List<QuestionRecord> question,
        List<ResourceRecord> answer, List<ResourceRecord> authority, List<ResourceRecord> additional )
    {
        this.transactionId = transactionId;
        this.messageType = messageType;
        this.opCode = opCode;
        this.authoritativeAnswer = authoritativeAnswer;
        this.truncated = truncated;
        this.recursionDesired = recursionDesired;
        this.recursionAvailable = recursionAvailable;
        this.reserved = reserved;
        this.acceptNonAuthenticatedData = acceptNonAuthenticatedData;
        this.responseCode = responseCode;
        this.questionRecords = question;
        this.answerRecords = answer;
        this.authorityRecords = authority;
        this.additionalRecords = additional;
    }


    /**
     * @return Returns the acceptNonAuthenticatedData.
     */
    public boolean isAcceptNonAuthenticatedData()
    {
        return acceptNonAuthenticatedData;
    }


    /**
     * @return Returns the additional.
     */
    public List<ResourceRecord> getAdditionalRecords()
    {
        return additionalRecords;
    }


    /**
     * @return Returns the answers.
     */
    public List<ResourceRecord> getAnswerRecords()
    {
        return answerRecords;
    }


    /**
     * @return Returns the authoritativeAnswer.
     */
    public boolean isAuthoritativeAnswer()
    {
        return authoritativeAnswer;
    }


    /**
     * @return Returns the authority.
     */
    public List<ResourceRecord> getAuthorityRecords()
    {
        return authorityRecords;
    }


    /**
     * @return Returns the messageType.
     */
    public MessageType getMessageType()
    {
        return messageType;
    }


    /**
     * @return Returns the opCode.
     */
    public OpCode getOpCode()
    {
        return opCode;
    }


    /**
     * @return Returns the question.
     */
    public List<QuestionRecord> getQuestionRecords()
    {
        return questionRecords;
    }


    /**
     * @return Returns the recursionAvailable.
     */
    public boolean isRecursionAvailable()
    {
        return recursionAvailable;
    }


    /**
     * @return Returns the recursionDesired.
     */
    public boolean isRecursionDesired()
    {
        return recursionDesired;
    }


    /**
     * @return Returns the reserved.
     */
    public boolean isReserved()
    {
        return reserved;
    }


    /**
     * @return Returns the responseCode.
     */
    public ResponseCode getResponseCode()
    {
        return responseCode;
    }


    /**
     * @return Returns the transactionId.
     */
    public int getTransactionId()
    {
        return transactionId;
    }


    /**
     * @return Returns the truncated.
     */
    public boolean isTruncated()
    {
        return truncated;
    }


    /**
     * @see java.lang.Object#equals(Object)
     */
    public boolean equals( Object object )
    {
        if ( object == this )
        {
            return true;
        }
        if ( !( object instanceof DnsMessage ) )
        {
            return false;
        }
        DnsMessage rhs = ( DnsMessage ) object;
        return new EqualsBuilder().append( this.transactionId, rhs.transactionId ).append( this.answerRecords,
            rhs.answerRecords ).append( this.opCode, rhs.opCode ).append( this.recursionAvailable,
            rhs.recursionAvailable ).append( this.messageType, rhs.messageType ).append( this.additionalRecords,
            rhs.additionalRecords ).append( this.truncated, rhs.truncated ).append( this.recursionDesired,
            rhs.recursionDesired ).append( this.responseCode, rhs.responseCode ).append( this.authorityRecords,
            rhs.authorityRecords ).append( this.authoritativeAnswer, rhs.authoritativeAnswer ).append( this.reserved,
            rhs.reserved ).append( this.acceptNonAuthenticatedData, rhs.acceptNonAuthenticatedData ).append(
            this.questionRecords, rhs.questionRecords ).isEquals();
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return new HashCodeBuilder( -1805208585, -276770303 ).append( this.transactionId ).append( this.answerRecords )
            .append( this.opCode ).append( this.recursionAvailable ).append( this.messageType ).append(
                this.additionalRecords ).append( this.truncated ).append( this.recursionDesired ).append(
                this.responseCode ).append( this.authorityRecords ).append( this.authoritativeAnswer ).append(
                this.reserved ).append( this.acceptNonAuthenticatedData ).append( this.questionRecords ).toHashCode();
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new ToStringBuilder( this ).appendSuper( super.toString() ).append( "transactionId", this.transactionId )
            .append( "opCode", this.opCode ).append( "truncated", this.truncated ).append( "messageType",
                this.messageType ).append( "recursionDesired", this.recursionDesired ).append( "additionalRecords",
                this.additionalRecords ).append( "responseCode", this.responseCode ).append( "authorityRecords",
                this.authorityRecords ).append( "acceptNonAuthenticatedData", this.acceptNonAuthenticatedData ).append(
                "recursionAvailable", this.recursionAvailable ).append( "answerRecords", this.answerRecords ).append(
                "questionRecords", this.questionRecords ).append( "authoritativeAnswer", this.authoritativeAnswer )
            .append( "reserved", this.reserved ).toString();
    }
}
