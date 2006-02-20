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
 * The question section is used to carry the "question" in most queries,
 * i.e., the parameters that define what is being asked.  The section
 * contains QDCOUNT (usually 1) entries, each of the following format:
 * 
 *                                     1  1  1  1  1  1
 *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                                               |
 *     /                     QNAME                     /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                     QTYPE                     |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                     QCLASS                    |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class QuestionRecord
{
    /**
     * A domain name represented as a sequence of labels, where
     * each label consists of a length octet followed by that
     * number of octets.  The domain name terminates with the
     * zero length octet for the null label of the root.  Note
     * that this field may be an odd number of octets; no
     * padding is used.
     */
    private String domainName;

    /**
     * A two octet code which specifies the type.
     */
    private RecordType recordType;

    /**
     * A two octet code that specifies the class.
     * For example, the CLASS field is IN for the Internet.
     */
    private RecordClass recordClass;


    public QuestionRecord(String domainName, RecordType recordType, RecordClass recordClass)
    {
        this.domainName = domainName;
        this.recordType = recordType;
        this.recordClass = recordClass;
    }


    /**
     * The domain name of this query.
     * For example, www.example.com.
     */
    public String getDomainName()
    {
        return domainName;
    }


    /**
     * The type of the query.
     * For example, the type is A for address records.
     */
    public RecordType getRecordType()
    {
        return recordType;
    }


    /**
     * The class for this query.
     * For example, the class is IN for the Internet.
     */
    public RecordClass getRecordClass()
    {
        return recordClass;
    }


    public String toString()
    {
        return getClass().getName() + " [ " + domainName + " ( " + recordClass + " " + recordType + " ) ]";
    }
}
