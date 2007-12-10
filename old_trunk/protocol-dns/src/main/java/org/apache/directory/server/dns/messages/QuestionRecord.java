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


import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;


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
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
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


    /**
     * Creates a new instance of QuestionRecord.
     *
     * @param domainName
     * @param recordType
     * @param recordClass
     */
    public QuestionRecord( String domainName, RecordType recordType, RecordClass recordClass )
    {
        this.domainName = domainName;
        this.recordType = recordType;
        this.recordClass = recordClass;
    }


    /**
     * The domain name of this query.
     * For example, www.example.com.
     * 
     * @return The domain name.
     */
    public String getDomainName()
    {
        return domainName;
    }


    /**
     * The type of the query.
     * For example, the type is A for address records.
     * 
     * @return The {@link RecordType}.
     */
    public RecordType getRecordType()
    {
        return recordType;
    }


    /**
     * The class for this query.
     * For example, the class is IN for the Internet.
     * 
     * @return The {@link RecordClass}.
     */
    public RecordClass getRecordClass()
    {
        return recordClass;
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
        if ( !( object instanceof QuestionRecord ) )
        {
            return false;
        }
        QuestionRecord rhs = ( QuestionRecord ) object;
        return new EqualsBuilder().append( this.domainName, rhs.domainName ).append( this.recordClass, rhs.recordClass )
            .append( this.recordType, rhs.recordType ).isEquals();
    }


    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        return new HashCodeBuilder( 1493545107, 315848479 ).append( this.domainName ).append( this.recordClass )
            .append( this.recordType ).toHashCode();
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return new ToStringBuilder( this ).appendSuper( super.toString() ).append( "domainName", this.domainName )
            .append( "recordClass", this.recordClass ).append( "recordType", this.recordType ).toString();
    }

}
