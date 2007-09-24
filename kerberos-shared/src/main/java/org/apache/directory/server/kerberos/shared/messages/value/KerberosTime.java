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
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Implementation of the time object for Kerberos.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosTime implements Comparable<KerberosTime>
{
    /** The number of milliseconds in a minute. */
    public static final int MINUTE = 60000;

    /** The number of milliseconds in a day. */
    public static final int DAY = MINUTE * 1440;

    /** The number of milliseconds in a week. */
    public static final int WEEK = MINUTE * 10080;

    /** Constant for the {@link KerberosTime} "infinity." */
    public static final KerberosTime INFINITY = new KerberosTime( Long.MAX_VALUE );

    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "UTC" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );
    private static final Calendar CALENDAR = new GregorianCalendar( UTC_TIME_ZONE );

    static
    {
        dateFormat.setTimeZone( UTC_TIME_ZONE );
    }

    private long kerberosTime;


    /**
     * Creates a new instance of KerberosTime.
     */
    public KerberosTime()
    {
        kerberosTime = System.currentTimeMillis();
    }


    /**
     * Creates a new instance of KerberosTime.
     *
     * @param time
     */
    public KerberosTime( long time )
    {
        kerberosTime = time;
    }


    /**
     * Creates a new instance of KerberosTime.
     *
     * @param time
     */
    public KerberosTime( Date time )
    {
        kerberosTime = time.getTime();
    }


    /**
     * Returns the {@link KerberosTime} for a given zulu time.
     *
     * @param zuluTime
     * @return The {@link KerberosTime}.
     * @throws ParseException
     */
    /*public static KerberosTime getTime( String zuluTime ) throws ParseException
    {
        Date date = null;
        synchronized ( dateFormat )
        {
            date = dateFormat.parse( zuluTime );
        }
        return new KerberosTime( date );
    }*/
    
    private static int[] DAYS_PER_MONTH = new int[]{ 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    private static int FEBRUARY = 2;
    
    /**
     * Parse a KerberosTime
     * @param zuluTime The time to parse
     * @return A valid KzerberosTime, containing the Date
     * @exception ParseException If the time is not a KerberosTime
     */
    public static KerberosTime getTime( String zuluTime ) throws ParseException
    {
        Date date = null;
        
        if ( ( zuluTime == null ) || ( zuluTime.length() != 15 ) )
        {
            throw new ParseException( "Invalid KerberosTime : it should not be null", 0 );
        }
        
        char[] zTime = zuluTime.toCharArray();

        for ( int i = 0; i < zTime.length; i++ )
        {
            if ( !StringTools.isDigit( zTime[i] ) )
            {
                if ( ( i == 14 ) && ( ( zTime[i] == 'Z' ) || ( zTime[i] == 'z' ) ) )
                {
                    break;
                }
                else
                {
                    throw new ParseException( "Invalid KerberosTime", i );
                }
            }
        }
        
        // compute the numbers
        int year =  ( ( zTime[0] - '0' ) * 1000 ) +
                    ( ( zTime[1] - '0' ) * 100 ) +
                    ( ( zTime[2] - '0' ) * 10 ) +
                    ( zTime[3] - '0' );
        
        int month = ( ( zTime[4] - '0' ) * 10 ) +
                    ( zTime[5] - '0' );

        int day =   ( ( zTime[6] - '0' ) * 10 ) +
                    ( zTime[7] - '0' );

        int hour =  ( ( zTime[8] - '0' ) * 10 ) +
                    ( zTime[9] - '0' );

        int min =   ( ( zTime[10] - '0' ) * 10 ) +
                    ( zTime[11] - '0' );

        int sec =   ( ( zTime[12] - '0' ) * 10 ) +
                    ( zTime[13] - '0' );
        
        // Now test the values
        if ( ( month == 0 ) || ( month > 12 ) ) 
        {
            throw new ParseException( "Invalid KerberosTime : month is invalid", 5 );
        }

        if ( day == 0 )
        {
            throw new ParseException( "Invalid KerberosTime : day is invalid", 7 );
        }
        
        if ( month == FEBRUARY )
        {
            // Check for leap years
            if ( ( year & 0x0003 ) == 0 )
            {
                // evey 400 years, we have a leap year, otherwise it's not
                if ( year % 400 == 0 )
                {
                    if ( day > 29 )
                    {
                        throw new ParseException( "Invalid KerberosTime : day is invalid", 7 );
                    }
                }
                else
                {
                    if ( day > 28 )
                    {
                        throw new ParseException( "Invalid KerberosTime : day is invalid for february", 7 );
                    }
                }
            }
            else
            {
                if ( day > 28 )
                {
                    // We don't have a leap year, so we should have only 28 days
                    throw new ParseException( "Invalid KerberosTime : day is invalid for a non leap year", 7 );
                }
            }
        }
        else
        {
            if ( day > DAYS_PER_MONTH[month] )
            {
                throw new ParseException( "Invalid KerberosTime : day is invalid for this month", 7 );
            }
        }
        
        if ( ( hour > 23 ) || ( min > 59 ) || ( sec > 59 ) ) 
        {
            throw new ParseException( "Invalid KerberosTime : time is invalid", 9 );
       }
        
        synchronized( CALENDAR )
        {
            CALENDAR.clear();
            CALENDAR.set( year, month - 1, day, hour, min, sec );
            date = CALENDAR.getTime();
        }
        
        return new KerberosTime( date );
    }


    public int compareTo( KerberosTime that )
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // this optimization is usually worthwhile, and can always be added
        if ( this == that )
        {
            return EQUAL;
        }

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


    /**
     * Returns the {@link KerberosTime} as a long.
     *
     * @return The {@link KerberosTime} as a long.
     */
    public long getTime()
    {
        return kerberosTime;
    }


    /**
     * Returns the {@link KerberosTime} as a {@link Date}.
     *
     * @return The {@link KerberosTime} as a {@link Date}.
     */
    public Date toDate()
    {
        return new Date( kerberosTime );
    }


    /**
     * Returns whether this {@link KerberosTime} is within the given clockskew.
     *
     * @param clockSkew
     * @return true if this {@link KerberosTime} is within the given clockskew.
     */
    public boolean isInClockSkew( long clockSkew )
    {
        return Math.abs( kerberosTime - System.currentTimeMillis() ) < clockSkew;
    }


    /**
     * Returns whether this {@link KerberosTime} is greater than a given {@link KerberosTime}.
     *
     * @param time
     * @return true if this {@link KerberosTime} is greater than a given {@link KerberosTime}. 
     */
    public boolean greaterThan( KerberosTime time )
    {
        return kerberosTime > time.kerberosTime;
    }


    /**
     * Returns whether this {@link KerberosTime} is less than a given {@link KerberosTime}.
     *
     * @param time
     * @return true if this {@link KerberosTime} is less than a given {@link KerberosTime}. 
     */
    public boolean lessThan( KerberosTime time )
    {
        return kerberosTime < time.kerberosTime;
    }


    /**
     * Returns whether this {@link KerberosTime} is equal to another {@link KerberosTime}.
     *
     * @param time
     * @return true if the two {@link KerberosTime}s are equal.
     */
    public boolean equals( KerberosTime time )
    {
        return kerberosTime == time.kerberosTime;
    }


    /**
     * Returns whether this {@link KerberosTime} is zero.
     *
     * @return true if this {@link KerberosTime} is zero.
     */
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
