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
public enum RecordClass implements EnumConverter<Short>
{
    /** Internet */
    IN(1),

    /** CSNET class */
    CS(2),

    /** CHAOS class */
    CH(3),

    /** Hesiod [Dyer 87] */
    HS(4),

    /** Special value used in dynamic update messages */
    NONE(254),

    /** Any class */
    ANY(255);

    private static ReverseEnumMap<Short, RecordClass> map = new ReverseEnumMap<Short, RecordClass>( RecordClass.class );

    private final short value;


    private RecordClass( int value )
    {
        this.value = ( byte ) value;
    }


    public Short convert()
    {
        return this.value;
    }


    public static RecordClass convert( short value )
    {
        return map.get( value );
    }
}
