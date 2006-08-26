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


import java.util.Map;


/**
 * The answer, authority, and additional sections all share the same
 * format: a variable number of resource records, where the number of
 * records is specified in the corresponding count field in the header.
 * Each resource record has the following format:
 *                                     1  1  1  1  1  1
 *       0  1  2  3  4  5  6  7  8  9  0  1  2  3  4  5
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                                               |
 *     /                                               /
 *     /                      NAME                     /
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                      TYPE                     |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                     CLASS                     |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                      TTL                      |
 *     |                                               |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 *     |                   RDLENGTH                    |
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--|
 *     /                     RDATA                     /
 *     /                                               /
 *     +--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+--+
 */
public class ResourceRecordImpl implements ResourceRecord
{
    /**
     * An owner name, i.e., the name of the node to which this
     * resource record pertains.
     */
    private String domainName;

    /**
     * Two octets containing one of the resource record TYPE codes.
     */
    private RecordType recordType;

    /**
     * Two octets containing one of the resource record CLASS codes.
     * For example, the CLASS field is IN for the Internet.
     */
    private RecordClass recordClass;

    /**
     * A 32 bit signed integer that specifies the time interval
     * that the resource record may be cached before the source
     * of the information should again be consulted.  Zero
     * values are interpreted to mean that the resource record can only be
     * used for the transaction in progress, and should not be
     * cached.  For example, SOA records are always distributed
     * with a zero TTL to prohibit caching.  Zero values can
     * also be used for extremely volatile data.
     */
    private int timeToLive;

    /**
     * A variable length string of octets that describes the
     * resource.  The format of this information varies
     * according to the TYPE and CLASS of the resource record.
     */
    private Map attributes;


    public ResourceRecordImpl( String domainName, RecordType recordType, RecordClass recordClass, int timeToLive,
        Map attributes )
    {
        this.domainName = domainName;
        this.recordType = recordType;
        this.recordClass = recordClass;
        this.timeToLive = timeToLive;
        this.attributes = attributes;
    }


    /**
     * @return Returns the domainName.
     */
    public String getDomainName()
    {
        return domainName;
    }


    /**
     * @return Returns the recordType.
     */
    public RecordType getRecordType()
    {
        return recordType;
    }


    /**
     * @return Returns the recordClass.
     */
    public RecordClass getRecordClass()
    {
        return recordClass;
    }


    /**
     * @return Returns the timeToLive.
     */
    public int getTimeToLive()
    {
        return timeToLive;
    }


    /**
     * @return Returns the value for the id.
     */
    public String get( String id )
    {
        return ( String ) attributes.get( id.toLowerCase() );
    }


    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }

        if ( !( o instanceof ResourceRecord ) )
        {
            return false;
        }

        ResourceRecordImpl that = ( ResourceRecordImpl ) o;

        return ( this.domainName.equals( that.domainName ) ) && ( this.recordType == that.recordType )
            && ( this.recordClass == that.recordClass );
    }


    public int hashCode()
    {
        return domainName.hashCode() + recordType.hashCode() + recordClass.hashCode();
    }


    public String toString()
    {
        return getClass().getName() + " [ " + domainName + " ( " + recordType + " " + recordClass + " " + timeToLive
            + " " + attributes.size() + " ) ]";
    }
}
