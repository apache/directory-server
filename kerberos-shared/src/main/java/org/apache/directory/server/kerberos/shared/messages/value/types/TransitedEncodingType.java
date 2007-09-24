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
package org.apache.directory.server.kerberos.shared.messages.value.types;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public enum TransitedEncodingType
{
    /**
     * Constant for the "null" transited encoding type.
     */
    NULL( 0 ),

    /**
     * Constant for the "Domain X500 compress" transited encoding type.
     */
    DOMAIN_X500_COMPRESS( 1 );

    /**
     * Array for building a List of VALUES.
     */
    private static final TransitedEncodingType[] values =
        { NULL, DOMAIN_X500_COMPRESS };

    /**
     * A List of all the transited encoding type constants.
     */
    public static final List VALUES = Collections.unmodifiableList( Arrays.asList( values ) );

    /**
     * The value/code for the transited encoding type.
     */
    private final int ordinal;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private TransitedEncodingType( int ordinal )
    {
        this.ordinal = ordinal;
    }


    /**
     * Returns the transited encoding type when specified by its ordinal.
     *
     * @param type
     * @return The transited encoding type.
     */
    public static TransitedEncodingType getTypeByOrdinal( int type )
    {
    	switch ( type )
    	{
    		case 1 	: return DOMAIN_X500_COMPRESS;
    		default : return NULL;
    	}
    }


    /**
     * Returns the number associated with this transited encoding type.
     *
     * @return The transited encoding type ordinal.
     */
    public int getOrdinal()
    {
        return ordinal;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
    	switch ( this )
    	{
    		case DOMAIN_X500_COMPRESS :	return "Domain X500 compress (1)";
    		default : 					return "null (0)";
    	}
    }
}
