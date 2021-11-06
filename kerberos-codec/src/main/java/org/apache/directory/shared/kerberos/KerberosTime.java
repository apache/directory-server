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


import java.text.ParseException;
import java.util.Date;

import org.apache.directory.api.util.GeneralizedTime;
import org.apache.directory.api.util.Strings;


/**
 * An specialization of the ASN.1 GeneralTime. The Kerberos time contains date and
 * time up to the seconds, but with no fractional seconds. It's also always
 * expressed as UTC timeZone, thus the 'Z' at the end of its string representation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTime implements Comparable<KerberosTime>, java.io.Serializable
{
    /** Serial version id */
    private static final long serialVersionUID = -7541256140193748103L;

    /** Constant for the {@link KerberosTime} "infinity." */
    public static final KerberosTime INFINITY = new KerberosTime( Long.MAX_VALUE );

    /** The number of milliseconds in a minute. */
    public static final int MINUTE = 60000;

    /** The number of milliseconds in a day. */
    public static final int DAY = MINUTE * 1440;

    /** The number of milliseconds in a week. */
    public static final int WEEK = MINUTE * 10080;
    
    /** Kerberos generalized time. */
    private GeneralizedTime generalizedTime;


    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime()
    {
        generalizedTime = new GeneralizedTime( ( System.currentTimeMillis() / 1000L ) * 1000L ); // drop the ms
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
        generalizedTime = new GeneralizedTime( date );
    }


    /**
     * Creates a new instance of KerberosTime.
     *
     * @param time
     */
    public KerberosTime( Date time )
    {
        generalizedTime = new GeneralizedTime( ( time.getTime() / 1000L ) * 1000L ); // drop the ms
    }


    /**
     * Returns the {@link KerberosTime} as a long.
     *
     * @return The {@link KerberosTime} as a long.
     */
    public long getTime()
    {
        return generalizedTime.getTime();
    }


    /**
     * Returns the {@link KerberosTime} as a {@link Date}.
     *
     * @return The {@link KerberosTime} as a {@link Date}.
     */
    public Date toDate()
    {
        return generalizedTime.getDate();
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
        return new KerberosTime( zuluTime );
    }


    /**
     * Sets the date if it's a valid KerberosTime
     * @param date The date to store
     */
    public synchronized void setDate( String date ) throws ParseException
    {
        generalizedTime = new GeneralizedTime( date );
    }


    /**
     * @return The date as a byte[]
     */
    public byte[] getBytes()
    {
        return Strings.getBytesUtf8( getDate() );
    }


    /**
     * @return The stored date
     */
    public String getDate()
    {
        return generalizedTime.toGeneralizedTime(  
            GeneralizedTime.Format.YEAR_MONTH_DAY_HOUR_MIN_SEC,
            GeneralizedTime.FractionDelimiter.DOT, 0,
            GeneralizedTime.TimeZoneFormat.Z
        );
    }


    @Override
    public int hashCode()
    {
        // leave out fraction (milliseconds)
        return ( int ) ( generalizedTime.getTime() / 1000L );
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof KerberosTime ) )
        {
            return true;
        }

        KerberosTime other = ( KerberosTime ) obj;

        // compare without fraction (milliseconds)
        return generalizedTime.getTime() / 1000L == other.generalizedTime.getTime() / 1000L;
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
        long delta = Math.abs( generalizedTime.getTime() - System.currentTimeMillis() );

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
        return generalizedTime.compareTo( that.generalizedTime );
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return getDate();
    }
}
