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
    public static KerberosTime getTime( String zuluTime ) throws ParseException
    {
        Date date = null;
        synchronized ( dateFormat )
        {
            date = dateFormat.parse( zuluTime );
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
