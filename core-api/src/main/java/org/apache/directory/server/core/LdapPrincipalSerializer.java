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
package org.apache.directory.server.core;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.name.DnSerializer;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class which serialize and deserialize a LdapPrincipal.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class LdapPrincipalSerializer
{
    /** The LoggerFactory used by this class */
    protected static final Logger LOG = LoggerFactory.getLogger( LdapPrincipalSerializer.class );

    /**
     * Private constructor.
     */
    private LdapPrincipalSerializer()
    {
    }

    
    /**
     * Serializes a LdapPrincipal instance.
     * 
     * @param principal The LsapPrincipal instance to serialize
     * @param out The stream into which we will write teh serialized instance
     * @throws IOException If the stream can't be written
     */
    public static void serialize( LdapPrincipal principal, ObjectOutput out ) throws IOException
    {
        // The Authentication level
        out.writeInt( principal.getAuthenticationLevel().getLevel() );
        
        // The principal's DN
        if ( principal.getDN() == null )
        {
            DnSerializer.serialize( Dn.EMPTY_DN, out );
        }
        else
        {
            DnSerializer.serialize( principal.getDN(), out );
        }
    }
    
    
    /**
     * Deserializes a LdapPrincipal instance.
     * 
     * @param schemaManager The SchemaManager (can be null)
     * @param in The input stream from which the Rdn is read
     * @return a deserialized Rdn
     * @throws IOException If the stream can't be read
     */
    public static LdapPrincipal deserialize( SchemaManager schemaManager, ObjectInput in )
        throws IOException, LdapInvalidDnException
    {
        // Read the authenyication level
        AuthenticationLevel authenticationLevel = AuthenticationLevel.getLevel( in.readInt() );
        
        // Read the principal's DN
        Dn dn = DnSerializer.deserialize( schemaManager, in );
        
        LdapPrincipal principal = new LdapPrincipal( schemaManager, dn, authenticationLevel );
        
        return principal;
    }
}
