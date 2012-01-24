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
package org.apache.directory.shared.kerberos.codec.types;


/**
 * Type safe enumeration of Single-use Authentication Mechanism types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum SamType
{
    /*
     * Enumeration elements are constructed once upon class loading.
     * Order of appearance here determines the order of compareTo.
     */

    /** safe SAM type enum for Enigma Logic */
    PA_SAM_TYPE_ENIGMA(1, "Enigma Logic"),

    /** safe SAM type enum for Digital Pathways */
    PA_SAM_TYPE_DIGI_PATH(2, "Digital Pathways"),

    /** safe SAM type enum for S/key where KDC has key 0 */
    PA_SAM_TYPE_SKEY_K0(3, "S/key where KDC has key 0"),

    /** safe SAM type enum for Traditional S/Key */
    PA_SAM_TYPE_SKEY(4, "Traditional S/Key"),

    /** safe SAM type enum for Security Dynamics */
    PA_SAM_TYPE_SECURID(5, "Security Dynamics"),

    /** safe SAM type enum for CRYPTOCard */
    PA_SAM_TYPE_CRYPTOCARD(6, "CRYPTOCard"),

    /** safe SAM type enum for Apache Software Foundation */
    PA_SAM_TYPE_APACHE(7, "Apache Software Foundation");

    /** the name of the sam type */
    private String name;

    /** the value/code for the sam type */
    private int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private SamType( int ordinal, String name )
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
     * Gets the ordinal by its ordinal value.
     *
     * @param ordinal the ordinal value of the ordinal
     * @return the type corresponding to the ordinal value
     */
    public static SamType getTypeByOrdinal( int ordinal )
    {
        for ( SamType st : SamType.values() )
        {
            if ( ordinal == st.getOrdinal() )
            {
                return st;
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
