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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Implementation of the time object for Kerberos
 */
public class KerberosTime implements Comparable
{
    public static final KerberosTime INFINITY = new KerberosTime( Long.MAX_VALUE );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );

    static
    {
        dateFormat.setTimeZone( UTC_TIME_ZONE );
    }

    private long kerberosTime;


    public KerberosTime()
    {
        kerberosTime = System.currentTimeMillis();
    }


    public KerberosTime(long time)
    {
        kerberosTime = time;
    }


    public KerberosTime(Date time)
    {
        kerberosTime = time.getTime();
    }

    
    public static KerberosTime getTime( String zuluTime ) throws ParseException
    {
        Date date = null;
        synchronized ( dateFormat )
        {
            date = dateFormat.parse( zuluTime );
        }
        return new KerberosTime( date );
    }

    
    public int compareTo( Object o )
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // this optimization is usually worthwhile, and can always be added
        if ( this == o )
        {
            return EQUAL;
        }

        // Performing explicit checks for nullity and type are made redundant by
        // the following cast, which will throw NullPointerException and
        // ClassCastException in these respective cases.
        final KerberosTime that = ( KerberosTime ) o;

        // primitive numbers follow this form
        if ( this.kerberosTime < that.kerberosTime )
        {
            return BEFORE;
        }

        if ( this.kerberosTime > that.kerberosTime )
        {
            return AFTER;
        }

        return EQUAL;
    }


    public long getTime()
    {
        return kerberosTime;
    }


    public Date toDate()
    {
        return new Date( kerberosTime );
    }


    public boolean isInClockSkew( long clockSkew )
    {
        return Math.abs( kerberosTime - System.currentTimeMillis() ) < clockSkew;
    }


    public boolean greaterThan( KerberosTime time )
    {
        return kerberosTime > time.kerberosTime;
    }


    public boolean lessThan( KerberosTime time )
    {
        return kerberosTime < time.kerberosTime;
    }


    public boolean equals( KerberosTime time )
    {
        return kerberosTime == time.kerberosTime;
    }


    public boolean isZero()
    {
        return kerberosTime == 0;
    }


    public String toString()
    {
        Date kerberosDate = new Date( kerberosTime );

        synchronized ( dateFormat )
        {
            return dateFormat.format( kerberosDate );
        }
    }
}
