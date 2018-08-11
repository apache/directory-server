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
package org.apache.directory.server.core.api;


import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
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
     * @param principal The LdapPrincipal instance to serialize
     * @param out The stream into which we will write the serialized instance
     * @throws IOException If the stream can't be written
     */
    public static void serialize( LdapPrincipal principal, ObjectOutput out ) throws IOException
    {
        // The Authentication level
        out.writeInt( principal.getAuthenticationLevel().getLevel() );

        // The principal's DN
        if ( principal.getDn() == null )
        {
            Dn.EMPTY_DN.writeExternal( out );
        }
        else
        {
            principal.getDn().writeExternal( out );
        }
    }


    /**
     * Deserializes a LdapPrincipal instance.
     * 
     * @param schemaManager The SchemaManager (can be null)
     * @param in The input stream from which the LdapPrincipal is read
     * @return a deserialized LdapPrincipal
     * @throws IOException If the stream can't be read
     */
    public static LdapPrincipal deserialize( SchemaManager schemaManager, ObjectInput in )
        throws IOException
    {
        // Read the authenyication level
        AuthenticationLevel authenticationLevel = AuthenticationLevel.getLevel( in.readInt() );

        // Read the principal's DN
        Dn dn = new Dn( schemaManager );

        try
        {
            dn.readExternal( in );
        }
        catch ( ClassNotFoundException cnfe )
        {
            IOException ioe = new IOException( cnfe.getMessage() );
            ioe.initCause( cnfe );
            throw ioe;
        }

        LdapPrincipal principal = new LdapPrincipal( schemaManager, dn, authenticationLevel );

        return principal;
    }
}
