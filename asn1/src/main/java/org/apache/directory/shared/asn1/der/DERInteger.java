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

import java.math.BigInteger;

/**
 * DER Integer object.
 */
public class DERInteger extends DERObject
{
    /**
     * Basic DERObject constructor.
     */
    DERInteger( byte[] value )
    {
    	super( INTEGER, value );
    }
    
    /**
     * Static factory method, type-conversion operator.
     */
    public static DERInteger valueOf( int integer )
    {
        return new DERInteger( intToOctet( integer ) );
    }
    
    /**
     * Lazy accessor
     * @return integer value
     */
    public int intValue()
    {
        return octetToInt( value );
    }
    
    private static int octetToInt( byte[] bytes )
    {
    	return new BigInteger( bytes ).intValue();
    }
	
    private static byte[] intToOctet( int integer )
    {
        return BigInteger.valueOf( integer ).toByteArray();
    }
}

