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
public enum MessageType implements EnumConverter<Byte>
{
    /** A query message. */
    QUERY(0),

    /** A response message. */
    RESPONSE(1);

    private static ReverseEnumMap<Byte, MessageType> map = new ReverseEnumMap<Byte, MessageType>( MessageType.class );

    private final byte value;


    private MessageType( int value )
    {
        this.value = ( byte ) value;
    }


    public Byte convert()
    {
        return this.value;
    }


    /**
     * Converts an ordinal value into a {@link MessageType}.
     *
     * @param value
     * @return The {@link MessageType}.
     */
    public static MessageType convert( byte value )
    {
        return map.get( value );
    }
}
