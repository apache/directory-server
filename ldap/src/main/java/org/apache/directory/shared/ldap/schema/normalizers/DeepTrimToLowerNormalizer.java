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
package org.apache.directory.shared.ldap.schema.normalizers;


import java.io.IOException;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.PrepareString;


/**
 * Normalizer which trims down whitespace replacing multiple whitespace
 * characters on the edges and within the string with a single space character
 * thereby preserving tokenization order - while doing all this in the same pass
 * it lower cases all characters.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DeepTrimToLowerNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of DeepTrimToLowerNormalizer.
     * 
     * @param oid The MR OID to use with this Normalizer
     */
    public DeepTrimToLowerNormalizer( String oid )
    {
        super( oid );
    }


    /**
     * Creates a new instance of DeepTrimToLowerNormalizer where the OID is
     * set after instantiation.
     */
    public DeepTrimToLowerNormalizer()
    {
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> normalize( Value<?> value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }

        try
        {
            String normalized = PrepareString.normalize( value.getString(),
                PrepareString.StringType.CASE_IGNORE );
            
            return new ClientStringValue( normalized );
        }
        catch ( IOException ioe )
        {
            throw new NamingException( "Invalid value : " + value );
        }
    }


    /**
     * {@inheritDoc}
     */
    public String normalize( String value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }

        try
        {
            String normalized = PrepareString.normalize( value,
                PrepareString.StringType.CASE_IGNORE );
            
            return normalized;
        }
        catch ( IOException ioe )
        {
            throw new NamingException( "Invalid value : " + value );
        }
    }
}
