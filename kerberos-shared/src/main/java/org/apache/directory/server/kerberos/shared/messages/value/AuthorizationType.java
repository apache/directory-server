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
public final class AuthorizationType implements Comparable<AuthorizationType>
{
    /**
     * Constant for the "null" authorization type.
     */
    public static final AuthorizationType NULL = new AuthorizationType( 0, "null" );

    /**
     * Constant for the "if relevant" authorization type.
     */
    public static final AuthorizationType AD_IF_RELEVANT = new AuthorizationType( 1, "if relevant" );

    /**
     * Constant for the "intended for server" authorization type.
     */
    public static final AuthorizationType AD_INTENDED_FOR_SERVER = new AuthorizationType( 2, "intended for server" );

    /**
     * Constant for the  "intended for application class" authorization type.
     */
    public static final AuthorizationType AD_INTENDED_FOR_APPLICATION_CLASS = new AuthorizationType( 3,
        "intended for application class" );

    /**
     * Constant for the "kdc issued" authorization type.
     */
    public static final AuthorizationType AD_KDC_ISSUED = new AuthorizationType( 4, "kdc issued" );

    /**
     * Constant for the "and-or" authorization type.
     */
    public static final AuthorizationType AD_AND_OR = new AuthorizationType( 5, "and-or" );

    /**
     * Constant for the "mandatory ticket extensions" authorization type.
     */
    public static final AuthorizationType AD_MANDATORY_TICKET_EXTENSIONS = new AuthorizationType( 6,
        "mandatory ticket extensions" );

    /**
     * Constant for the "in ticket extensions" authorization type.
     */
    public static final AuthorizationType AD_IN_TICKET_EXTENSIONS = new AuthorizationType( 7, "in ticket extensions" );

    /**
     * Constant for the "mandatory for KDC" authorization type.
     */
    public static final AuthorizationType AD_MANDATORY_FOR_KDC = new AuthorizationType( 8, "mandatory for KDC" );

    /**
     * Constant for the "Initial verified CAS" authorization type.
     */
    public static final AuthorizationType AD_INITIAL_VERIFIED_CAS = new AuthorizationType( 9, "Initial verified CAS" );

    /**
     * Constant for the "OSF DCE" authorization type.
     */
    public static final AuthorizationType OSF_DCE = new AuthorizationType( 64, "OSF DCE" );

    /**
     * Constant for the "sesame" authorization type.
     */
    public static final AuthorizationType SESAME = new AuthorizationType( 65, "sesame" );

    /**
     * Constant for the "OSF DCE PKI CERTID" authorization type.
     */
    public static final AuthorizationType AD_OSF_DCE_PKI_CERTID = new AuthorizationType( 66, "OSF DCE PKI CERTID" );

    /**
     * Constant for the "WIN2K PAC" authorization type.
     */
    public static final AuthorizationType AD_WIN2K_PAC = new AuthorizationType( 128, "WIN2K PAC" );

    /**
     * Constant for the "encryption negotiation" authorization type.
     */
    public static final AuthorizationType AD_ETYPE_NEGOTIATION = new AuthorizationType( 129, "encryption negotiation" );

    /**
     * Array for building a List of VALUES.
     */
    private static final AuthorizationType[] values =
        { NULL, AD_IF_RELEVANT, AD_INTENDED_FOR_SERVER, AD_INTENDED_FOR_APPLICATION_CLASS, AD_KDC_ISSUED, AD_AND_OR,
            AD_MANDATORY_TICKET_EXTENSIONS, AD_IN_TICKET_EXTENSIONS, AD_MANDATORY_FOR_KDC, AD_INITIAL_VERIFIED_CAS,
            OSF_DCE, SESAME, AD_OSF_DCE_PKI_CERTID, AD_WIN2K_PAC, AD_ETYPE_NEGOTIATION };

    /**
     * A List of all the authorization type constants.
     */
    public static final List<AuthorizationType> VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

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


    public int compareTo( AuthorizationType that )
    {
        return ordinal - that.ordinal;
    }


    public String toString()
    {
        return name + " (" + ordinal + ")";
    }
}
