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

package org.apache.directory.server.dns.messages;

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
    private short transactionId;
    private MessageType messageType;
    private OpCode opCode;
    private boolean authoritativeAnswer;
    private boolean truncated;
    private boolean recursionDesired;
    private boolean recursionAvailable;
    private boolean reserved;
    private boolean acceptNonAuthenticatedData;

    private ResponseCode responseCode;

    private QuestionRecords questionRecords;
    private ResourceRecords answerRecords;
    private ResourceRecords authorityRecords;
    private ResourceRecords additionalRecords;

    public DnsMessage( short transactionId, MessageType messageType, OpCode opCode, boolean authoritativeAnswer,
            boolean truncated, boolean recursionDesired, boolean recursionAvailable, boolean reserved,
            boolean acceptNonAuthenticatedData, ResponseCode responseCode, QuestionRecords question,
            ResourceRecords answer, ResourceRecords authority, ResourceRecords additional )
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
    public ResourceRecords getAdditionalRecords()
    {
        return additionalRecords;
    }

    /**
     * @return Returns the answers.
     */
    public ResourceRecords getAnswerRecords()
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
    public ResourceRecords getAuthorityRecords()
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
    public QuestionRecords getQuestionRecords()
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
    public short getTransactionId()
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

    public String toString()
    {
        return getClass().getName() + "[ transactionId = " + transactionId + " ]";
    }
}
