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
package org.apache.directory.server.kerberos;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Hashtable;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An {@link AbstractServerTest} testing the (@link {@link PasswordPolicyInterceptor}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith ( FrameworkRunner.class ) 
@CreateDS( allowAnonAccess=true, name="PasswordPolicyServiceIT-class",
    partitions =
        {
            @CreatePartition(
                name = "example",
                suffix = "dc=example,dc=com",
                contextEntry = @ContextEntry( 
                    entryLdif =
                        "dn: dc=example,dc=com\n" +
                        "dc: example\n" +
                        "objectClass: top\n" +
                        "objectClass: domain\n\n" ),
                indexes = 
                {
                    @CreateIndex( attribute = "objectClass" ),
                    @CreateIndex( attribute = "dc" ),
                    @CreateIndex( attribute = "ou" )
                } )
        })
@CreateLdapServer ( 
    transports = 
    {
        @CreateTransport( protocol = "LDAP" )
    },
    saslHost="localhost",
    saslMechanisms = 
    {
        @SaslMechanism( name=SupportedSaslMechanisms.PLAIN, implClass=PlainMechanismHandler.class ),
        @SaslMechanism( name=SupportedSaslMechanisms.CRAM_MD5, implClass=CramMd5MechanismHandler.class),
        @SaslMechanism( name= SupportedSaslMechanisms.DIGEST_MD5, implClass=DigestMd5MechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSSAPI, implClass=GssapiMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.NTLM, implClass=NtlmMechanismHandler.class),
        @SaslMechanism( name=SupportedSaslMechanisms.GSS_SPNEGO, implClass=NtlmMechanismHandler.class)
    },
    extendedOpHandlers = 
    {
        StoredProcedureExtendedOperationHandler.class
    })
@Ignore( "This test case is no loger useful cause we removed PasswordPolicyInterceptor, instead look at PasswordPolicyTest" )
//WARN: this test class will be removed soon
public class PasswordPolicyServiceIT extends AbstractLdapTestUnit
{
    private DirContext ctx;
    private DirContext users;
    
    
    /**
     * Set up a partition for EXAMPLE.COM
     */
    @Before
    public void setUp() throws Exception
    {
        Attributes attrs;
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + getLdapServer().getPort() + "/dc=example,dc=com" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialDirContext( env );

        attrs = getOrgUnitAttributes( "users" );
        users = ctx.createSubcontext( "ou=users", attrs );
    }

    
    /**
     * Tests that passwords that are too short are properly rejected. 
     */
    @Test
    public void testLength()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "HN1" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix are properly rejected. 
     */
    @Test
    public void testCharacterMix()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords that contain substrings of the username are properly rejected. 
     */
    @Test
    public void testContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "A1nelson" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that are too
     * short are properly rejected. 
     */
    @Test
    public void testCharacterMixAndLength()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "hi" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertFalse( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords that are too short and that contain substrings of
     * the username are properly rejected.
     */
    @Test
    public void testLengthAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Bush", "William Bush", "wbush", "bush1" );
        try
        {
            users.createSubcontext( "uid=wbush", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertFalse( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that contain substrings of
     * the username are properly rejected.
     */
    @Test
    public void testCharacterMixAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "hnelson" );
        try
        {
            users.createSubcontext( "uid=hnelson", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertFalse( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tests that passwords with insufficient character mix and that are too
     * short and that contain substrings of the username are properly rejected.
     */
    @Test
    public void testCharacterMixAndLengthAndContainsUsername()
    {
        Attributes attrs = getPersonAttributes( "Bush", "William Bush", "wbush", "bush" );
        try
        {
            users.createSubcontext( "uid=wbush", attrs );
            fail( "Shouldn't have gotten here." );
        }
        catch ( NamingException ne )
        {
            assertTrue( ne.getMessage().contains( "length too short" ) );
            assertTrue( ne.getMessage().contains( "insufficient character mix" ) );
            assertTrue( ne.getMessage().contains( "contains portions of username" ) );
        }
    }


    /**
     * Tear down.
     */
    @After
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
    }


    /**
     * Convenience method for creating a person.
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     */
    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }
}
