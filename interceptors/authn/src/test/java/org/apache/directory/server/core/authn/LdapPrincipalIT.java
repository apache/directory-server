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
package org.apache.directory.server.core.authn;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.LdapPrincipalSerializer;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * 
 * Test the LdapPrincipal class
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class LdapPrincipalIT
{
    /** The schema manager instance */
    private static SchemaManager schemaManager;

    @BeforeClass
    public static void setUp() throws Exception
    {
        schemaManager = new DefaultSchemaManager();
    }


    /**
     * Test the serialization of an empty LdapPrincipal
     */
    @Test
    public void testStaticSerializeEmptyLdapPrincipal() throws Exception
    {
        LdapPrincipal principal = new LdapPrincipal( schemaManager );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        LdapPrincipalSerializer.serialize( principal, out );
        out.flush();

        byte[] data = baos.toByteArray();
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        LdapPrincipal readPrincipal = LdapPrincipalSerializer.deserialize( schemaManager, in );
        assertEquals( principal.getAuthenticationLevel(), readPrincipal.getAuthenticationLevel() );
        assertEquals( principal.getName(), readPrincipal.getName() );
    }
    
    
    /**
     * Test the serialization of an empty LdapPrincipal
     */
    @Test
    public void testStaticSerializeLdapPrincipalWithSchemaManager() throws Exception
    {
        LdapPrincipal principal = new LdapPrincipal( schemaManager, new Dn( schemaManager, "uid=admin,ou=system" ), AuthenticationLevel.STRONG );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream( baos );

        LdapPrincipalSerializer.serialize( principal, out );
        out.flush();

        byte[] data = baos.toByteArray();
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream( data ) );

        LdapPrincipal readPrincipal = LdapPrincipalSerializer.deserialize( schemaManager, in );
        assertEquals( principal.getAuthenticationLevel(), readPrincipal.getAuthenticationLevel() );
        assertEquals( principal.getName(), readPrincipal.getName() );
    }
}
