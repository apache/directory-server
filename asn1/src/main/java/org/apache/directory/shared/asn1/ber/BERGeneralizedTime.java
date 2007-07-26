/*
 * Copyright (c) 2000 - 2006 The Legion Of The Bouncy Castle (http://www.bouncycastle.org)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE
 * FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 * 
 */

package org.apache.directory.shared.asn1.ber;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * DER Generalized time object.
 */
public class BERGeneralizedTime extends BERString
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
    BERGeneralizedTime(byte[] value)
    {
        super( value );
    }


    /**
     * Static factory method, type-conversion operator.
     */
    public static BERGeneralizedTime valueOf( Date date )
    {
        String dateString = null;

        synchronized ( dateFormat )
        {
            dateString = dateFormat.format( date );
        }

        byte[] bytes = stringToByteArray( dateString );

        return new BERGeneralizedTime( bytes );
    }


    /**
     * Lazy accessor
     * 
     * @return Date representation of this BER Generalized Time
     * @throws ParseException
     */
    public long getDate() throws ParseException
    {
        long date = 0;
        int pos = 0;
        

        
        return date;
    }
}
