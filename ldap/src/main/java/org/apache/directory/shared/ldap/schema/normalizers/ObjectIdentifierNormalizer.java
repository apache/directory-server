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


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * A normalizer for the objectIdentifierMatch matching rule.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectIdentifierNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of ObjectIdentifierNormalizer.
     */
    public ObjectIdentifierNormalizer()
    {
        super( SchemaConstants.OBJECT_IDENTIFIER_MATCH_MR_OID );
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

        String str = value.getString().trim();

        if ( str.length() == 0 )
        {
            return new ClientStringValue( "" );
        }
        else if ( Character.isDigit( str.charAt( 0 ) ) )
        {
            // We do this test to avoid a lowerCasing which cost time
            return new ClientStringValue( str );
        }
        else
        {
            return new ClientStringValue( str.toLowerCase() );
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

        String str = value.trim();

        if ( str.length() == 0 )
        {
            return "";
        }
        else if ( Character.isDigit( str.charAt( 0 ) ) )
        {
            // We do this test to avoid a lowerCasing which cost time
            return str;
        }
        else
        {
            return str.toLowerCase();
        }
    }
}
