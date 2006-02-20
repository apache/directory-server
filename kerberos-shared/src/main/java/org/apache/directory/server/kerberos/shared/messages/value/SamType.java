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
package org.apache.directory.server.kerberos.shared.messages.value;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Type safe enumeration of Single-use Authentication Mechanism types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class SamType implements Comparable
{
    /*
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */

    /** safe SAM type enum for Enigma Logic */
    public static final SamType PA_SAM_TYPE_ENIGMA = new SamType( 1, "Enigma Logic" );

    /** safe SAM type enum for Digital Pathways */
    public static final SamType PA_SAM_TYPE_DIGI_PATH = new SamType( 2, "Digital Pathways" );

    /** safe SAM type enum for S/key where KDC has key 0 */
    public static final SamType PA_SAM_TYPE_SKEY_K0 = new SamType( 3, "S/key where KDC has key 0" );

    /** safe SAM type enum for Traditional S/Key */
    public static final SamType PA_SAM_TYPE_SKEY = new SamType( 4, "Traditional S/Key" );

    /** safe SAM type enum for Security Dynamics */
    public static final SamType PA_SAM_TYPE_SECURID = new SamType( 5, "Security Dynamics" );

    /** safe SAM type enum for CRYPTOCard */
    public static final SamType PA_SAM_TYPE_CRYPTOCARD = new SamType( 6, "CRYPTOCard" );

    /** safe SAM type enum for Apache Software Foundation */
    public static final SamType PA_SAM_TYPE_APACHE = new SamType( 7, "Apache Software Foundation" );

    /** Array for building a List of VALUES. */
    private static final SamType[] values =
        { PA_SAM_TYPE_ENIGMA, PA_SAM_TYPE_DIGI_PATH, PA_SAM_TYPE_SKEY_K0, PA_SAM_TYPE_SKEY, PA_SAM_TYPE_SECURID,
            PA_SAM_TYPE_CRYPTOCARD, PA_SAM_TYPE_APACHE };

    /** a list of all the sam type constants */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /** the name of the sam type */
    private final String name;

    /** the value/code for the sam type */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private SamType(int ordinal, String name)
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the name of the SamType.
     *
     * @return the name of the SAM type
     */
    public String toString()
    {
        return name;
    }


    /**
     * Compares this type to another object hopefully one that is of the same
     * type.
     *
     * @param that the object to compare this SamType to
     * @return ordinal - ( ( SamType ) that ).ordinal;
     */
    public int compareTo( Object that )
    {
        return ordinal - ( ( SamType ) that ).ordinal;
    }


    /**
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static SamType getTypeByOrdinal( int ordinal )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == ordinal )
            {
                return values[ii];
            }
        }

        return PA_SAM_TYPE_APACHE;
    }


    /**
     * Gets the ordinal value associated with this SAM type.
     *
     * @return the ordinal value associated with this SAM type
     */
    public int getOrdinal()
    {
        return ordinal;
    }
}
