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


import org.apache.directory.server.dns.util.EnumConverter;
import org.apache.directory.server.dns.util.ReverseEnumMap;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum ResponseCode implements EnumConverter<Byte>
{
    /** No error condition. */
    NO_ERROR(0),

    /** The name server was unable to interpret the query. */
    FORMAT_ERROR(1),

    /** The name server was unable to process this query due to a problem with the name server. */
    SERVER_FAILURE(2),

    /** The domain name referenced in the query does not exist. */
    NAME_ERROR(3),

    /** The name server does not support the requested kind of query. */
    NOT_IMPLEMENTED(4),

    /** The name server refuses to perform the specified operation for policy reasons. */
    REFUSED(5);

    private static ReverseEnumMap<Byte, ResponseCode> map = new ReverseEnumMap<Byte, ResponseCode>( ResponseCode.class );

    private final byte value;


    private ResponseCode( int value )
    {
        this.value = ( byte ) value;
    }


    public Byte convert()
    {
        return this.value;
    }


    public static ResponseCode convert( byte value )
    {
        return map.get( value );
    }
}
