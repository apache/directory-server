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

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
public class KerberosTime
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
    
    /** The KerberosTime */
    private String date;
    
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
    }

    
    /**
     * Creates a new instance of a KerberosTime object
     * 
     * @param date the KerberosTime to store
     */
    public KerberosTime( String date )
    {
        setDate( date );
    }
    
    
    /**
     * Creates a new instance of a KerberosTime object
     */
    public KerberosTime( long date )
    {
        Calendar calendar = Calendar.getInstance( UTC );
        calendar.setTimeInMillis( date );
        this.date = sdf.format( calendar.getTime() );
    }
    
    
    /**
     * Sets the date if it's a valid KerberosTime
     * @param date The date to store
     */
    public void setDate( String date )
    {
        boolean result = DATE_PATTERN.matcher( date ).find();

        if ( result )
        {
            this.date = date;
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
}
