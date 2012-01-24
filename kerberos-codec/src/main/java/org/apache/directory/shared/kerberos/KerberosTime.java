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
package org.apache.directory.shared.kerberos;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.apache.directory.shared.util.DateUtils;
import org.apache.directory.shared.util.Strings;


/**
 * An specialization of the ASN.1 GeneralTime. The Kerberos time contains date and
 * time up to the seconds, but with no fractional seconds. It's also always
 * expressed as UTC timeZone, thus the 'Z' at the end of its string representation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTime implements Comparable<KerberosTime>, Serializable
{
    private static final long serialVersionUID = 1L;

    /** The UTC timeZone */
    private static final TimeZone UTC = TimeZone.getTimeZone( "UTC" );

    /** The KerberosTime as a String*/
    private String date;

    /** The kerberosTime, as a long */
    private long kerberosTime;

    /** Constant for the {@link KerberosTime} "infinity." */
    public static final KerberosTime INFINITY = new KerberosTime( Long.MAX_VALUE );

    /** The number of milliseconds in a minute. */
    public static final int MINUTE = 60000;

    /** The number of milliseconds in a day. */
    public static final int DAY = MINUTE * 1440;

    /** The number of milliseconds in a week. */
    public static final int WEEK = MINUTE * 10080;


    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime()
    {
        kerberosTime = ( System.currentTimeMillis() / 1000L ) * 1000L; // drop the ms
        convertInternal( kerberosTime );
    }


    /**
     * Creates a new instance of a KerberosTime object
     * 
     * @param date the KerberosTime to store
     */
    public KerberosTime( String date )
    {
        try
        {
            setDate( date );
        }
        catch ( ParseException pe )
        {
            throw new IllegalArgumentException( "Bad time : " + date );
        }
    }


    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime( long date )
    {
        convertInternal( date );
    }


    /**
     * Creates a new instance of KerberosTime.
     *
     * @param time
     */
    public KerberosTime( Date time )
    {
        kerberosTime = ( time.getTime() / 1000L ) * 1000L; // drop the ms
        convertInternal( kerberosTime );
    }


    /**
     * converts the given milliseconds time to seconds and
     * also formats the time to the generalized form
     * 
     * @param date the time in milliseconds
     */
    private void convertInternal( long date )
    {
        Calendar calendar = Calendar.getInstance( UTC );
        calendar.setTimeInMillis( date );

        synchronized ( DateUtils.DATE_FORMAT )
        {
            this.date = DateUtils.DATE_FORMAT.format( calendar.getTime() );
        }

        kerberosTime = ( calendar.getTimeInMillis() / 1000L ) * 1000L; // drop the ms
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
     * Returns the {@link KerberosTime} for a given zulu time.
     *
     * @param zuluTime
     * @return The {@link KerberosTime}.
     * @throws ParseException
     */
    public static KerberosTime getTime( String zuluTime ) throws ParseException
    {
        Date date = null;

        synchronized ( DateUtils.DATE_FORMAT )
        {
            date = DateUtils.DATE_FORMAT.parse( zuluTime );
        }

        return new KerberosTime( date );
    }


    /**
     * Sets the date if it's a valid KerberosTime
     * @param date The date to store
     */
    public void setDate( String date ) throws ParseException
    {
        synchronized ( DateUtils.DATE_FORMAT )
        {
            kerberosTime = DateUtils.DATE_FORMAT.parse( date ).getTime();
        }

        convertInternal( kerberosTime );
    }


    /**
     * @return The date as a byte[]
     */
    public byte[] getBytes()
    {
        return Strings.getBytesUtf8( date );
    }


    /**
     * @return The stored date
     */
    public String getDate()
    {
        return date;
    }


    @Override
    public int hashCode()
    {
        return ( int ) kerberosTime;
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( obj == null )
        {
            return true;
        }

        KerberosTime other = ( KerberosTime ) obj;

        return kerberosTime == other.kerberosTime;
    }


    /**
     * Returns whether this {@link KerberosTime} is within the given clockskew.
     *
     * @param clockSkew
     * @return true if this {@link KerberosTime} is within the given clockskew.
     */
    public boolean isInClockSkew( long clockSkew )
    {
        // The KerberosTime does not have milliseconds
        long delta = Math.abs( kerberosTime - System.currentTimeMillis() );

        return delta < clockSkew;
    }


    /**
     * compares current kerberos time with the given kerberos time
     * @param that the kerberos time against which the current kerberos time is compared
     * @return 0 if both times are equal,<br>
     *         -1 if current time is less than the given time and<br>
     *         1 if the given time is greater than the current time
     */
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
     * checks if the current kerberos time is less or equal than the given kerberos time
     * @param ktime the kerberos time against which the current kerberos time needs to be compared
     * @return true if current kerberos time is less or equal than the given kerberos time, false otherwise
     */
    public boolean lessThan( KerberosTime ktime )
    {
        return kerberosTime <= ktime.kerberosTime;
    }


    /**
     * checks if the current kerberos time is greater than the given kerberos time
     * @param ktime the kerberos time against which the currnet kerberos time needs to be compared
     * @return true if current kerberos time is greater than the given kerberos time, false otherwise
     */
    public boolean greaterThan( KerberosTime ktime )
    {
        return kerberosTime > ktime.kerberosTime;
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


    /**
     * Write a serialized version of this instance.
     */
    private void writeObject( ObjectOutputStream out ) throws IOException
    {
        out.writeUTF( date );
    }


    /**
     * Read a KerberosTime from a stream
     */
    private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException
    {
        String date = in.readUTF();

        try
        {
            setDate( date );
        }
        catch ( ParseException pe )
        {
            kerberosTime = ( System.currentTimeMillis() / 1000L ) * 1000L; // drop the ms
            convertInternal( kerberosTime );
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return date;
    }
}
