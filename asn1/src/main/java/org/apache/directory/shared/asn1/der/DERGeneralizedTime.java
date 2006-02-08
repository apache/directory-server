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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * DER Generalized time object.
 */
public class DERGeneralizedTime extends DERString
{
	private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );
    
	static
	{
		dateFormat.setTimeZone( UTC_TIME_ZONE );
	}
	
    /**
     * Basic DERObject constructor.
     */
    DERGeneralizedTime( byte[] value )
    {
    	super( GENERALIZED_TIME, value );
    }
    
    /**
     * Static factory method, type-conversion operator.
     */
    public static DERGeneralizedTime valueOf( Date date )
    {
        String dateString = null;
        
        synchronized (dateFormat)
        {
        	dateString = dateFormat.format( date );
        }
        
        byte[] bytes = stringToByteArray( dateString );

        return new DERGeneralizedTime( bytes );
    }
    
    /**
     * Lazy accessor
     * @return Date representation of this DER Generalized Time
     * @throws ParseException
     */
    public Date getDate()
    	throws ParseException
    {
        String string = byteArrayToString( value );
        
        
        synchronized (dateFormat)
        {
        	return dateFormat.parse( string );
        }
    }
}

