/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.template.ConnectionCallback;
import org.apache.directory.ldap.client.template.LdapConnectionTemplate;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Tests the CreateLdapConnectionPoolRule.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateLdapServer(
        transports = {
                @CreateTransport(protocol = "LDAP")
        })
@CreateDS(name = "classDS",
        enableChangeLog = true,
        partitions = {
                @CreatePartition(
                        name = "example",
                        suffix = "dc=example,dc=com",
                        contextEntry = @ContextEntry(
                                entryLdif =
                                "dn: dc=example,dc=com\n" +
                                "objectClass: domain\n" +
                                "objectClass: top\n" +
                                "dc: example\n\n"
                        ),
                        indexes = {
                                @CreateIndex(attribute = "objectClass"),
                                @CreateIndex(attribute = "dc"),
                                @CreateIndex(attribute = "ou")
                })
        })
@ApplyLdifs({
                "dn: cn=class,ou=system",
                "objectClass: person",
                "cn: class",
                "sn: sn_class"
        })
@CreateLdapConnectionPool
public class TestCreateLdapConnectionPoolRule
{
    private static Logger LOG = LoggerFactory.getLogger( TestCreateLdapConnectionPoolRule.class );
    @ClassRule
    public static CreateLdapConnectionPoolRule classCreateLdapConnectionPoolRule = 
        new CreateLdapConnectionPoolRule();
    
    @Rule
    public CreateLdapConnectionPoolRule createLdapConnectionPoolRule = 
        new CreateLdapConnectionPoolRule( classCreateLdapConnectionPoolRule );
    
    
    @Test
    public void testLdapConnectionTemplate() 
    {
        LOG.trace( "checking ldap connection template" );
        LdapConnectionTemplate ldapConnectionTemplate =
            createLdapConnectionPoolRule.getLdapConnectionTemplate();
        assertNotNull( ldapConnectionTemplate );
        
        ldapConnectionTemplate.execute( 
            new ConnectionCallback<Object>() 
            {
                @Override
                public Object doWithConnection( LdapConnection connection ) throws LdapException
                {
                    assertNotNull( connection );
                    return null;
                }
            });
    }


    @Test
    public void testClassLdapConnectionPool()
    {
        assertEquals( createLdapConnectionPoolRule.getLdapConnectionPool(), 
            classCreateLdapConnectionPoolRule.getLdapConnectionPool() );
    
        LdapConnection ldapConnection = null;
        try
        {
            ldapConnection = createLdapConnectionPoolRule.getLdapConnectionPool()
                .getConnection();
    
            Dn dn = new Dn( "cn=class,ou=system" );
            Entry entry = ldapConnection.lookup( dn );
            assertNotNull( entry );
            assertEquals( "class", entry.get( "cn" ).get().getValue() );
        }
        catch ( LdapException e )
        {
            fail( e.getMessage() );
        }
        finally
        {
            if ( ldapConnection != null )
            {
                try
                {
                    createLdapConnectionPoolRule.getLdapConnectionPool()
                        .releaseConnection( ldapConnection );
                }
                catch ( LdapException e )
                {
                    // Who cares!
                }
            }
        }
    }
}