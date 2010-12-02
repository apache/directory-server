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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An specialization of the ASN.1 GeneralTime. The Kerberos time contains date and 
 * time up to the seconds, but with no fractional seconds. It's also always
 * expressed as UTC timeZone, thus the 'Z' at the end of its string representation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosTime implements Comparable<KerberosTime>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( KerberosTime.class );

    /** The GeneralizedDate pattern matching */
    private static final String GENERALIZED_TIME_PATTERN =
                    "^\\d{4}" // century + year : 0000 to 9999
                    + "(0[1-9]|1[0-2])" // month : 01 to 12
                    + "(0[1-9]|[12]\\d|3[01])" // day : 01 to 31
                    + "([01]\\d|2[0-3])" // hour : 00 to 23
                    + "([0-5]\\d)" // minute : 00 to 59
                    + "([0-5]\\d)Z"; // second and UTC TZ

    /** The date pattern. The regexp pattern is immutable, only one instance needed. */
    private static final Pattern DATE_PATTERN = Pattern.compile( GENERALIZED_TIME_PATTERN );

    /** The format for a KerberosTime */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );
    
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

    // Initialize the dateFormat with the UTC TZ
    static
    {
        sdf.setTimeZone( UTC );
    }

    
    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime()
    {
        kerberosTime = System.currentTimeMillis();
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
            // TODO : mnaage exception
        }
    }
    
    
    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime( long date )
    {
        Calendar calendar = Calendar.getInstance( UTC );
        calendar.setTimeInMillis( date );
        this.date = sdf.format( calendar.getTime() );
        kerberosTime = date;
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
        
        synchronized ( sdf )
        {
            date = sdf.parse( zuluTime );
        }
        
        return new KerberosTime( date );
    }

    
    /**
     * Sets the date if it's a valid KerberosTime
     * @param date The date to store
     */
    public void setDate( String date ) throws ParseException
    {
        boolean result = DATE_PATTERN.matcher( date ).find();

        if ( result )
        {
            this.date = date;

            synchronized ( sdf )
            {
                kerberosTime = sdf.parse( date ).getTime();
            }
            
            LOG.debug( "Syntax valid for '{}'", date );
        }
        else
        {
            LOG.debug( "Syntax invalid for '{}'", date );
            throw new IllegalArgumentException();
        }
    }
    
    
    /**
     * @return The date as a byte[]
     */
    public byte[] getBytes()
    {
        return StringTools.getBytesUtf8( date );
    }
    
    
    /**
     * @return The stored date
     */
    public String getDate()
    {
        return date;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return date;
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( date == null ) ? 0 : date.hashCode() );
        return result;
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
        
        if ( date == null )
        {
            if ( other.date != null )
            {
                return false;
            }
        }
        else if ( !date.equals( other.date ) )
        {
            return false;
        }
        
        return true;
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
     * checks if the current kerberos time is less than the given kerberos time
     * @param ktime the kerberos time against which the currnet kerberos time needs to be compared
     * @return true if current kerberos time is less than the given kerberos time, false otherwise
     */
    public boolean lessThan( KerberosTime ktime )
    {
        return kerberosTime < ktime.kerberosTime;
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

}
