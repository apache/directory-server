/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.ldap.client.template;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;


import java.util.List;


import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.AddResponse;
import org.apache.directory.api.ldap.model.message.DeleteResponse;
import org.apache.directory.api.ldap.model.message.ModifyRequest;
import org.apache.directory.api.ldap.model.message.ModifyResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.template.exception.PasswordException;
import org.apache.directory.server.annotations.CreateLdapConnectionPool;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.CreateLdapConnectionPoolRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;


/**
 * A test class for LdapConnectionTemplate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    }
    )
@CreateDS(name = "classDS",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry(
                    entryLdif =
                    "dn: dc=example, dc=com\n" +
                        "objectClass: domain\n" +
                        "objectClass: top\n" +
                        "dc: example\n\n"
                ),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                }
            )
    }
)
@ApplyLdifFiles( {
        "ldif/muppets.ldif"
    }
)
@CreateLdapConnectionPool(
        maxActive = 1,
        maxWait = 5000 )
public class LdapConnectionTemplateTest
{
    @ClassRule
    public static CreateLdapConnectionPoolRule classCreateLdapConnectionPoolRule =
            new CreateLdapConnectionPoolRule();
    
    @Rule
    public CreateLdapConnectionPoolRule createLdapConnectionPoolRule =
            new CreateLdapConnectionPoolRule( classCreateLdapConnectionPoolRule );
    
    private LdapConnectionTemplate ldapConnectionTemplate;
    

    @Before
    public void before() {
        ldapConnectionTemplate = createLdapConnectionPoolRule.getLdapConnectionTemplate();
    }


    @Test
    public void testAdd()
    {
        String uid = "newmuppet";
        Dn dn = ldapConnectionTemplate.newDn( "uid=" + uid + ",ou=people,dc=example,dc=com" );
        assertNotNull( dn );

        AddResponse response = ldapConnectionTemplate.add( dn,
            ldapConnectionTemplate.newAttribute( "objectClass", 
                "top", "person", "organizationalPerson", "inetOrgPerson" ),
            ldapConnectionTemplate.newAttribute( "cn", "New Muppet" ),
            ldapConnectionTemplate.newAttribute( "sn", "Muppet" ),
            ldapConnectionTemplate.newAttribute( "uid", "newmuppet" ) );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        String foundUid = ldapConnectionTemplate.lookup( dn,
            new EntryMapper<String>()
            {
                @Override
                public String map( Entry entry ) throws LdapException
                {
                    return entry.get( "uid" ).getString();
                }
            });
        assertNotNull( foundUid );
        assertEquals( uid, foundUid );
    }
    
    
    @Test
    public void testAuthenticate() 
    {
        try
        {
            PasswordWarning warning = ldapConnectionTemplate.authenticate( 
                "ou=people,dc=example,dc=com",
                "(mail=kermitthefrog@muppets.com)",
                SearchScope.ONELEVEL,
                "set4now".toCharArray() );
            assertNull( warning );
        }
        catch ( PasswordException e )
        {
            fail( "authenticate failed unexpectedly" );
        }

        try
        {
            PasswordWarning warning = ldapConnectionTemplate.authenticate( 
                ldapConnectionTemplate.newDn( "uid=kermitthefrog, ou=people, dc=example, dc=com" ),
                "set4now".toCharArray() );
            assertNull( warning );
        }
        catch ( PasswordException e )
        {
            fail( "authenticate failed unexpectedly" );
        }

        try
        {
            ldapConnectionTemplate.authenticate( 
                ldapConnectionTemplate.newDn( "uid=kermitthefrog, ou=people, dc=example, dc=com" ),
                "set4later".toCharArray() );
            fail( "authenticate failed unexpectedly" );
        }
        catch ( PasswordException e )
        {
            assertEquals( ResultCodeEnum.INVALID_CREDENTIALS, e.getResultCode() );
        }
    }
    

    @Test
    public void testDelete()
    {
        String uid = "kermitthefrog";
        Dn dn = ldapConnectionTemplate.newDn( "uid=" + uid + ", ou=people, dc=example, dc=com" );
        DeleteResponse response = ldapConnectionTemplate.delete( dn );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );

        String foundUid = ldapConnectionTemplate.lookup( dn,
            new EntryMapper<String>()
            {
                @Override
                public String map( Entry entry ) throws LdapException
                {
                    return entry.get( "uid" ).getString();
                }
            });
        assertNull( foundUid );
    }
    

    @Test
    public void testLookup() 
    {
        String uid = "kermitthefrog";
        Dn dn = ldapConnectionTemplate.newDn( "uid=" + uid + ", ou=people, dc=example, dc=com" );
        String foundUid = ldapConnectionTemplate.lookup( dn,
            new EntryMapper<String>()
            {
                @Override
                public String map( Entry entry ) throws LdapException
                {
                    return entry.get( "uid" ).getString();
                }
            });
        assertNotNull( foundUid );
        assertEquals( uid, foundUid );
    }


    @Test
    public void testModify()
    {
        String uid = "misspiggy";
        Dn dn = ldapConnectionTemplate.newDn( "uid=" + uid + ",ou=people,dc=example,dc=com" );
        assertNotNull( dn );

        ModifyResponse response = ldapConnectionTemplate.modify( dn,
            new RequestBuilder<ModifyRequest>()
            {
                @Override
                public void buildRequest( ModifyRequest request ) throws LdapException
                {
                    request.replace( "sn", "The Frog" );
                    request.replace( "cn", "Miss The Frog" );
                }
            } );
        assertEquals( ResultCodeEnum.SUCCESS, response.getLdapResult().getResultCode() );
        
        Muppet found = ldapConnectionTemplate.lookup( dn, Muppet.getEntryMapper() );
        assertNotNull( found );
        assertEquals( uid, found.getUid() );
        assertEquals( "The Frog", found.getSn() );
        assertEquals( "Miss The Frog", found.getCn() );
    }
    
    
    @Test
    public void testModifyPassword() 
    {
        Dn userDn = ldapConnectionTemplate.newDn( "uid=kermitthefrog, ou=people, dc=example, dc=com" );
        try
        {
            ldapConnectionTemplate.modifyPassword( 
                userDn, "set4later".toCharArray() );
            ldapConnectionTemplate.authenticate( userDn, "set4later".toCharArray() );
        }
        catch ( PasswordException e )
        {
            fail( "failed to change password" );
        }

        try
        {
            ldapConnectionTemplate.modifyPassword( 
                userDn,
                "set4now".toCharArray(),
                "set4notnow".toCharArray() );
            fail( "should not have been allowed to change password" );
        }
        catch ( PasswordException e )
        {
            // expected
        }

        try
        {
            ldapConnectionTemplate.modifyPassword( 
                userDn,
                "set4later".toCharArray(),
                "set4now".toCharArray(),
                false );
            ldapConnectionTemplate.authenticate( userDn, "set4now".toCharArray() );
        }
        catch ( PasswordException e )
        {
            fail( "failed to change password" );
        }
    }
    
    
    @Test
    public void testReauthenticate() throws PasswordException
    {
        for ( int i = 0; i < 100; i++ ) {
            ldapConnectionTemplate.authenticate( 
                    ldapConnectionTemplate.newDn( 
                            ServerDNConstants.ADMIN_SYSTEM_DN ),
                    "secret".toCharArray() );
        }
    }

    
    @Test
    public void testSearch() 
    {
        List<Muppet> muppets = ldapConnectionTemplate.search( 
            "ou=people,dc=example,dc=com", 
            "(objectClass=inetOrgPerson)", 
            SearchScope.ONELEVEL,
            Muppet.getEntryMapper() );
        assertNotNull( muppets );
        assertEquals( 6, muppets.size() );

        ldapConnectionTemplate.search( 
            ldapConnectionTemplate.newSearchRequest( 
                "ou=people,dc=example,dc=com", 
                "(objectClass=inetOrgPerson)", 
                SearchScope.ONELEVEL ),
            Muppet.getEntryMapper() );
        assertNotNull( muppets );
        assertEquals( 6, muppets.size() );
    }
    
    
    @Test
    public void testDIRAPI_202() {
        // test requested by https://issues.apache.org/jira/browse/DIRAPI-202
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost( "localhost" );
        config.setLdapPort( createLdapConnectionPoolRule.getLdapServer().getPort() );
        config.setName( "uid=admin,ou=system" );
        config.setCredentials( "secret" );

        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory( config );
        factory.setTimeOut( 30000 );

        // optional, values below are defaults
        GenericObjectPool.Config poolConfig = new GenericObjectPool.Config();
        poolConfig.lifo = true;
        poolConfig.maxActive = 8;
        poolConfig.maxIdle = 8;
        poolConfig.maxWait = -1L;
        poolConfig.minEvictableIdleTimeMillis = 1000L * 60L * 30L;
        poolConfig.minIdle = 0;
        poolConfig.numTestsPerEvictionRun = 3;
        poolConfig.softMinEvictableIdleTimeMillis = -1L;
        poolConfig.testOnBorrow = false;
        poolConfig.testOnReturn = false;
        poolConfig.testWhileIdle = false;
        poolConfig.timeBetweenEvictionRunsMillis = -1L;
        poolConfig.whenExhaustedAction = GenericObjectPool.WHEN_EXHAUSTED_BLOCK;

        LdapConnectionTemplate ldapConnectionTemplate = 
            new LdapConnectionTemplate( new LdapConnectionPool(
                new ValidatingPoolableLdapConnectionFactory( factory ), poolConfig ) );
        assertNotNull( ldapConnectionTemplate );

        List<Muppet> muppets = ldapConnectionTemplate.search( 
                "ou=people,dc=example,dc=com", 
                "(objectClass=inetOrgPerson)", 
                SearchScope.ONELEVEL,
                Muppet.getEntryMapper() );
            assertNotNull( muppets );
            assertEquals( 6, muppets.size() );

            ldapConnectionTemplate.search( 
                ldapConnectionTemplate.newSearchRequest( 
                    "ou=people,dc=example,dc=com", 
                    "(objectClass=inetOrgPerson)", 
                    SearchScope.ONELEVEL ),
                Muppet.getEntryMapper() );
            assertNotNull( muppets );
            assertEquals( 6, muppets.size() );
    }

    
    public static class Muppet 
    {
        private static final EntryMapper<Muppet> entryMapper = 
            new EntryMapper<LdapConnectionTemplateTest.Muppet>()
            {
                @Override
                public Muppet map( Entry entry ) throws LdapException
                {
                    return new Muppet()
                        .setCn( entry.get( "cn" ).getString() )
                        .setGivenName( entry.get( "givenName" ).getString() )
                        .setMail( entry.get( "mail" ).getString() )
                        .setSn( entry.get( "sn" ).getString() )
                        .setUid( entry.get( "uid" ).getString() );
                }
            };

        private String cn;
        private String givenName;
        private String mail;
        private String sn;
        private String uid;


        public String getCn()
        {
            return cn;
        }
        
        
        public static EntryMapper<Muppet> getEntryMapper() {
            return entryMapper;
        }

        
        public String getGivenName()
        {
            return givenName;
        }
        
        
        public String getMail()
        {
            return mail;
        }
        
        
        public String getSn()
        {
            return sn;
        }
        
        
        public String getUid()
        {
            return uid;
        }
        
        
        public Muppet setCn( String cn )
        {
            this.cn = cn;
            return this;
        }
        
        
        public Muppet setGivenName( String givenName )
        {
            this.givenName = givenName;
            return this;
        }
        
        
        public Muppet setMail( String mail )
        {
            this.mail = mail;
            return this;
        }
        
        
        public Muppet setSn( String sn )
        {
            this.sn = sn;
            return this;
        }
        
        
        public Muppet setUid( String uid )
        {
            this.uid = uid;
            return this;
        }
    }
}
