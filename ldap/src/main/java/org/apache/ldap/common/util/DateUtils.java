/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.ldap.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Gets the generalized time using the "Z" form of the g-time-zone.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DateUtils
{
	private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone( "GMT" );
	
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyyMMddHHmmss'Z'" );
    
    static 
    {
    	dateFormat.setTimeZone( UTC_TIME_ZONE );
    }

    /**
     * Gets the generalized time using the "Z" form of the g-time-zone
     * described by [<a href=
     * "http://ietf.org/internet-drafts/draft-ietf-ldapbis-syntaxes-09.txt">
     * SYNTAXES</a>] section 3.3.13, included below:
     * <pre>
     *
     * 3.3.13.  Generalized Time
     *
     * A value of the Generalized Time syntax is a character string
     * representing a date and time.  The LDAP-specific encoding of a value
     * of this syntax is a restriction of the format defined in [ISO8601],
     * and is described by the following ABNF:
     *
     * century = 2(%x30-39) ; "00" to "99"
     * year    = 2(%x30-39) ; "00" to "99"
     * month   =   ( %x30 %x31-39 ) ; "01" (January) to "09"
     *           / ( %x31 %x30-32 ) ; "10" to "12"
     * day     =   ( %x30 %x31-39 )    ; "01" to "09"
     *           / ( %x31-32 %x30-39 ) ; "10" to "29"
     *           / ( %x33 %x30-31 )    ; "30" to "31"
     * hour    = ( %x30-31 %x30-39 ) / ( %x32 %x30-33 ) ; "00" to "23"
     * minute  = %x30-35 %x30-39                        ; "00" to "59"
     * second  =   ( %x30-35 %x30-39 )  ; "00" to "59"
     *           / ( %x36 %x30 )        ; "60" (a leap second)
     *
     * GeneralizedTime = century year month day hour
     *                      [ minute [ second ] ] [ fraction ]
     *                      g-time-zone
     * fraction        = ( DOT / COMMA ) 1*(%x30-39)
     * g-time-zone     = %x5A  ; "Z"
     *                   / g-differential
     * g-differential  = ( MINUS / PLUS ) hour [ minute ]
     * MINUS           = %x2D  ; minus sign ("-")
     *
     * The <DOT>, <COMMA> and <PLUS> rules are defined in [MODELS].
     *
     * The time value represents coordinated universal time (equivalent to
     * Greenwich Mean Time) if the "Z" form of <g-time-zone> is used,
     *
     * otherwise the value represents a local time in the time zone
     * indicated by <g-differential>.  In the latter case, coordinated
     * universal time can be calculated by subtracting the differential from
     * the local time.  The "Z" form of <g-time-zone> SHOULD be used in
     * preference to <g-differential>.
     *
     * Examples:
     *    199412161032Z
     *    199412160532-0500
     *
     * Both example values represent the same coordinated universal time:
     * 10:32 AM, December 16, 1994.
     *
     * The LDAP definition for the Generalized Time syntax is:
     *
     * ( 1.3.6.1.4.1.1466.115.121.1.24 DESC 'Generalized Time' )
     *
     * This syntax corresponds to the GeneralizedTime ASN.1 type from
     * [ASN.1], with the constraint that local time without a differential
     * SHALL NOT be used.
     * </pre>
     *
     * Gets the generalized time right now.
     *
     * @return the generalizedTime right now
     */ 
    public static String getGeneralizedTime()
    {
    	Date date = new Date();
    	
    	synchronized (dateFormat)
    	{
    		return dateFormat.format( date );
    	}
    } 
}

