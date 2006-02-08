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

package org.apache.directory.shared.asn1.der;


/**
 * DER Boolean object.
 */
public class DERBoolean extends DERObject
{
    private static final byte[] trueArray =
        { ( byte ) 0xff };

    private static final byte[] falseArray =
        { ( byte ) 0x00 };

    public static final DERBoolean TRUE = new DERBoolean( trueArray );

    public static final DERBoolean FALSE = new DERBoolean( falseArray );


    /**
     * Basic DERObject constructor.
     */
    public DERBoolean(byte[] value)
    {
        super( BOOLEAN, value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static DERBoolean valueOf( boolean value )
    {
        return ( value ? TRUE : FALSE );
    }


    /**
     * Lazy accessor
     * 
     * @return boolean value
     */
    public boolean isTrue()
    {
        return value[0] == ( byte ) 0xff;
    }
}
