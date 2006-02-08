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
 * DER UTF8String object.
 */
public class DERUTF8String extends DERString
{
    /**
     * Basic DERObject constructor.
     */
    public DERUTF8String( byte[] value )
    {
    	super( UTF8_STRING, value );
    }
    
    /**
     * Static factory method, type-conversion operator.
     */
    public static DERUTF8String valueOf( String string )
    {
        return new DERUTF8String( stringToByteArray( string ) );
    }
}

