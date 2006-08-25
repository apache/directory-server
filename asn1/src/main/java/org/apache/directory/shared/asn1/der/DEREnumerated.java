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

package org.apache.directory.shared.asn1.der;


/**
 * DER Enumerated object.
 */
public class DEREnumerated extends DERObject
{
    /**
     * Basic DERObject constructor.
     */
    public DEREnumerated(byte[] value)
    {
        super( ENUMERATED, value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DEREnumerated valueOf( int integer )
    {
        return new DEREnumerated( intToOctet( integer ) );
    }


    /**
     * Lazy accessor
     * 
     * @return integer value
     */
    public int intValue()
    {
        return octetToInt( value );
    }


    private static int octetToInt( byte[] bytes )
    {
        int result = 0;

        for ( int ii = 0; ii < Math.min( 4, bytes.length ); ii++ )
        {
            result += bytes[ii] * ( 16 ^ ii );
        }
        return result;
    }


    private static byte[] intToOctet( int integer )
    {
        byte[] result = new byte[4];

        for ( int ii = 0, shift = 24; ii < 4; ii++, shift -= 8 )
        {
            result[ii] = ( byte ) ( 0xFF & ( integer >> shift ) );
        }
        return result;
    }
}
