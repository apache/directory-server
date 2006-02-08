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

package org.apache.directory.server.ntp.messages;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Reference Identifier: This is a 32-bit bitstring identifying the
 * particular reference source. In the case of NTP Version 3 or Version
 * 4 stratum-0 (unspecified) or stratum-1 (primary) servers, this is a
 * four-character ASCII string, left justified and zero padded to 32
 * bits. In NTP Version 3 secondary servers, this is the 32-bit IPv4
 * address of the reference source. In NTP Version 4 secondary servers,
 * this is the low order 32 bits of the latest transmit timestamp of the
 * reference source. NTP primary (stratum 1) servers should set this
 * field to a code identifying the external reference source according
 * to the following list. If the external reference is one of those
 * listed, the associated code should be used. Codes for sources not
 * listed can be contrived as appropriate.
 */
public class ReferenceIdentifier implements Comparable
{
	/**
	 * Enumeration elements are constructed once upon class loading.
	 * Order of appearance here determines the order of compareTo.
	 */
	public static final ReferenceIdentifier INIT = new ReferenceIdentifier(0,  "INIT", "initializing");
	public static final ReferenceIdentifier LOCL = new ReferenceIdentifier(1,  "LOCL", "uncalibrated local clock");
	public static final ReferenceIdentifier PPS  = new ReferenceIdentifier(2,  "PPL", "pulse-per-second source");
	public static final ReferenceIdentifier ACTS = new ReferenceIdentifier(3,  "ACTS", "NIST dialup modem service");
	public static final ReferenceIdentifier USNO = new ReferenceIdentifier(4,  "USNO", "USNO modem service");
	public static final ReferenceIdentifier PTB  = new ReferenceIdentifier(5,  "PTB", "PTB (Germany) modem service");
	public static final ReferenceIdentifier TDF  = new ReferenceIdentifier(6,  "TDF", "Allouis (France) Radio 164 kHz");
	public static final ReferenceIdentifier DCF  = new ReferenceIdentifier(7,  "DCF", "Mainflingen (Germany) Radio 77.5 kHz");
	public static final ReferenceIdentifier MSF  = new ReferenceIdentifier(8,  "MSF", "Rugby (UK) Radio 60 kHz");
	public static final ReferenceIdentifier WWV  = new ReferenceIdentifier(9,  "WWV", "Ft. Collins (US) Radio 2.5, 5, 10, 15, 20 MHz");
	public static final ReferenceIdentifier WWVB = new ReferenceIdentifier(10, "WWVB", "Boulder (US) Radio 60 kHz");
	public static final ReferenceIdentifier WWVH = new ReferenceIdentifier(11, "WWVH", "Kaui Hawaii (US) Radio 2.5, 5, 10, 15 MHz");
	public static final ReferenceIdentifier CHU  = new ReferenceIdentifier(12, "CHU", "Ottawa (Canada) Radio 3330, 7335, 14670 kHz");
	public static final ReferenceIdentifier LORC = new ReferenceIdentifier(13, "LORC", "LORAN-C radionavigation system");
	public static final ReferenceIdentifier OMEG = new ReferenceIdentifier(14, "OMEG", "OMEGA radionavigation system");
	public static final ReferenceIdentifier GPS  = new ReferenceIdentifier(15, "GPS", "Global Positioning Service");
	public static final ReferenceIdentifier GOES = new ReferenceIdentifier(16, "GOES", "Geostationary Orbit Environment Satellite");
	public static final ReferenceIdentifier CDMA = new ReferenceIdentifier(17, "CDMA", "CDMA mobile cellular/PCS telephone system");

    /**
     * These two lines are all that's necessary to export a List of VALUES.
     */
    private static final ReferenceIdentifier[] values = { INIT, LOCL, PPS, ACTS, USNO, PTB, TDF, DCF, MSF, WWV, WWVB,
            WWVH, CHU, LORC, OMEG, GPS, GOES, CDMA };
    // VALUES needs to be located here, otherwise illegal forward reference
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    private final int ordinal;
    private final String name;
    private final String code;

    /**
     * Private constructor prevents construction outside of this class.
     */
    private ReferenceIdentifier( int ordinal, String code, String name )
    {
        this.ordinal = ordinal;
        this.code = code;
        this.name = name;
    }

    public static ReferenceIdentifier getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].ordinal == type )
            {
                return values[ ii ];
            }
        }

        return LOCL;
    }

    public static ReferenceIdentifier getTypeByName( String type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ ii ].code.equalsIgnoreCase( type ) )
            {
                return values[ ii ];
            }
        }

        return LOCL;
    }

    public int getOrdinal()
    {
        return ordinal;
    }

    public String getCode()
    {
        return code;
    }

    public int compareTo( Object that )
    {
        return ordinal - ( (ReferenceIdentifier) that ).ordinal;
    }

    public String toString()
    {
        return name;
    }
}
