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


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class AuthorizationType implements Comparable
{
    /**
     * Constant for the "null" authorization type.
     */
    public static final AuthorizationType NULL = new AuthorizationType( 0, "null" );

    /**
     * Constant for the "if relevant" authorization type.
     */
    public static final AuthorizationType IF_RELEVANT = new AuthorizationType( 1, "if relevant" );

    /**
     * Constant for the "intended for server" authorization type.
     */
    public static final AuthorizationType INTENDED_FOR_SERVER = new AuthorizationType( 2, "intended for server" );

    /**
     * Constant for the  "intended for application class" authorization type.
     */
    public static final AuthorizationType INTENDED_FOR_APPLICATION_CLASS = new AuthorizationType( 3,
        "intended for application class" );

    /**
     * Constant for the "kdc issued" authorization type.
     */
    public static final AuthorizationType KDC_ISSUED = new AuthorizationType( 4, "kdc issued" );

    /**
     * Constant for the "or" authorization type.
     */
    public static final AuthorizationType OR = new AuthorizationType( 5, "or" );

    /**
     * Constant for the "mandatory ticket extensions" authorization type.
     */
    public static final AuthorizationType MANDATORY_TICKET_EXTENSIONS = new AuthorizationType( 6,
        "mandatory ticket extensions" );

    /**
     * Constant for the "in ticket extensions" authorization type.
     */
    public static final AuthorizationType IN_TICKET_EXTENSIONS = new AuthorizationType( 7, "in ticket extensions" );

    /**
     * Constant for the "OSF DCE" authorization type.
     */
    public static final AuthorizationType OSF_DCE = new AuthorizationType( 64, "OSF DCE" );

    /**
     * Constant for the "sesame" authorization type.
     */
    public static final AuthorizationType SESAME = new AuthorizationType( 65, "sesame" );

    /**
     * Array for building a List of VALUES.
     */
    private static final AuthorizationType[] values =
        { NULL, IF_RELEVANT, INTENDED_FOR_SERVER, INTENDED_FOR_APPLICATION_CLASS, KDC_ISSUED, OR,
            MANDATORY_TICKET_EXTENSIONS, IN_TICKET_EXTENSIONS, OSF_DCE, SESAME };

    /**
     * A List of all the authorization type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The name of the authorization type.
     */
    private final String name;

    /**
     * The value/code for the authorization type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private AuthorizationType( int ordinal, String name )
    {
        this.ordinal = ordinal;
        this.name = name;
    }


    /**
     * Returns the authorization type when specified by its ordinal.
     *
     * @param type
     * @return The authorization type.
     */
    public static AuthorizationType getTypeByOrdinal( int type )
    {
        for ( int ii = 0; ii < values.length; ii++ )
        {
            if ( values[ii].ordinal == type )
                return values[ii];
        }

        return NULL;
    }


    /**
     * Returns the number associated with this authorization type.
     *
     * @return The authorization type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }


    public int compareTo( Object that )
    {
        return ordinal - ( ( AuthorizationType ) that ).ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
