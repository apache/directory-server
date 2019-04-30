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
 * The list of possible value for the TransitedEncoding type
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum TransitedEncodingType
{
    /** Constant for the "null" transited encoding type. */
    NULL(0),

    /** Constant for the "Domain X500 compress" transited encoding type. */
    DOMAIN_X500_COMPRESS(1);

    /**
     * The value/code for the transited encoding type.
     */
    private final int value;


    /**
     * Private constructor prevents construction outside of this class.
     */
    private TransitedEncodingType( int value )
    {
        this.value = value;
    }


    /**
     * Returns the transited encoding type when specified by its value.
     *
     * @param type The type we are looking for
     * @return The transited encoding type.
     */
    public static TransitedEncodingType getTypeByOrdinal( int type )
    {
        if ( type == 1 )
        {
            return DOMAIN_X500_COMPRESS;
        }
        else
        {
            return NULL;
        }
    }


    /**
     * Returns the number associated with this transited encoding type.
     *
     * @return The transited encoding type value.
     */
    public int getValue()
    {
        return value;
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString()
    {
        if ( this == DOMAIN_X500_COMPRESS )
        {
            return "Domain X500 compress (1)";
        }
        else
        {
                return "null (0)";
        }
    }
}
