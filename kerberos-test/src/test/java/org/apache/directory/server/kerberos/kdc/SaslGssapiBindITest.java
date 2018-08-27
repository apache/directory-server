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
package org.apache.directory.server.kerberos.kdc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.ldap.client.api.Krb5LoginConfiguration;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An {@link AbstractServerTest} testing SASL GSSAPI authentication
 * and security layer negotiation.  These tests require both the LDAP
 * and the Kerberos protocol.  As with any "three-headed" Kerberos
 * scenario, there are 3 principals:  1 for the test user, 1 for the
 * Kerberos ticket-granting service (TGS), and 1 for the LDAP service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "SaslGssapiBindITest-class",
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
                        "objectClass: domain\n\n"),
                indexes =
                    {
                        @CreateIndex(attribute = "objectClass"),
                        @CreateIndex(attribute = "dc"),
                        @CreateIndex(attribute = "ou")
                })
    },
    additionalInterceptors =
        {
            KeyDerivationInterceptor.class
    })
@CreateLdapServer(
    transports =
        {
            @CreateTransport(protocol = "LDAP")
    },
    saslHost = "localhost",
    saslPrincipal = "ldap/localhost@EXAMPLE.COM",
    saslMechanisms =
        {
            @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
    })
@CreateKdcServer(
    transports =
        {
            @CreateTransport(protocol = "UDP", port = 6088),
            @CreateTransport(protocol = "TCP", port = 6088)
    })
@ApplyLdifs({
    "dn: ou=users,dc=example,dc=com",
    "objectClass: top",
    "objectClass: organizationalUnit",
    "ou: users"
})
public class SaslGssapiBindITest extends AbstractKerberosITest
{

    /**
     * Tests to make sure GSSAPI binds below the RootDSE work.
     */
    @Test
    public void testSaslGssapiBind() throws Exception
    {
        // Set up a partition for EXAMPLE.COM and add user and service principals to test authentication with.
        KerberosTestUtils.fixServicePrincipalName(
            "ldap/" + KerberosTestUtils.getHostName() + "@EXAMPLE.COM", null, getLdapServer() );
        ObtainTicketParameters parameters = new ObtainTicketParameters( TcpTransport.class,
            EncryptionType.AES128_CTS_HMAC_SHA1_96, ChecksumType.HMAC_SHA1_96_AES128 );
        setupEnv( parameters );

        kdcServer.getConfig().setPaEncTimestampRequired( false );
        // Use our custom configuration to avoid reliance on external config
        Configuration.setConfiguration( new Krb5LoginConfiguration() );
        // 1. Authenticate to Kerberos.
        LoginContext lc = null;
        try
        {
            lc = new LoginContext( SaslGssapiBindITest.class.getName(), new CallbackHandlerBean( "hnelson", "secret" ) );
            lc.login();
        }
        catch ( LoginException le )
        {
            // Bad username:  Client not found in Kerberos database
            // Bad password:  Integrity check on decrypted field failed
            fail( "Authentication failed:  " + le.getMessage() );
        }

        // 2. Perform JNDI work as authenticated Subject.
        Subject.doAs( lc.getSubject(), new PrivilegedAction<Void>()
        {
            public Void run()
            {
                //FIXME activate this code as soon as the GSSAPIMechanismHandler is fixed.
                //Currently GSSAPI authentication for the ldap server is broken
                try
                {
                    // Create the initial context
                    Hashtable<String, String> env = new Hashtable<String, String>();
                    env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
                    env.put( Context.PROVIDER_URL, "ldap://" + KerberosTestUtils.getHostName() + ":"
                        + getLdapServer().getPort() );

                    // Request the use of the "GSSAPI" SASL mechanism
                    // Authenticate by using already established Kerberos credentials
                    env.put( Context.SECURITY_AUTHENTICATION, "GSSAPI" );

                    // Request privacy protection
                    env.put( "javax.security.sasl.qop", "auth-conf" );

                    // Request mutual authentication
                    env.put( "javax.security.sasl.server.authentication", "true" );

                    // Request high-strength cryptographic protection
                    env.put( "javax.security.sasl.strength", "high" );

                    DirContext ctx = new InitialDirContext( env );

                    String[] attrIDs =
                        { "uid" };

                    Attributes attrs = ctx.getAttributes( "uid=hnelson,ou=users,dc=example,dc=com", attrIDs );

                    String uid = null;

                    if ( attrs.get( "uid" ) != null )
                    {
                        uid = ( String ) attrs.get( "uid" ).get();
                    }

                    assertEquals( "hnelson", uid );
                }
                catch ( NamingException e )
                {
                    fail( "Should not have caught exception:  " + e.getMessage() + e.getRootCause() );
                }

                return null;
            }
        } );

    }


    private class CallbackHandlerBean implements CallbackHandler
    {
        private String name;
        private String password;


        /**
         * Creates a new instance of CallbackHandlerBean.
         *
         * @param name
         * @param password
         */
        public CallbackHandlerBean( String name, String password )
        {
            this.name = name;
            this.password = password;
        }


        public void handle( Callback[] callbacks ) throws UnsupportedCallbackException, IOException
        {
            for ( int ii = 0; ii < callbacks.length; ii++ )
            {
                Callback callBack = callbacks[ii];

                // Handles username callback.
                if ( callBack instanceof NameCallback )
                {
                    NameCallback nameCallback = ( NameCallback ) callBack;
                    nameCallback.setName( name );
                    // Handles password callback.
                }
                else if ( callBack instanceof PasswordCallback )
                {
                    PasswordCallback passwordCallback = ( PasswordCallback ) callBack;
                    passwordCallback.setPassword( password.toCharArray() );
                }
                else
                {
                    throw new UnsupportedCallbackException( callBack, I18n.err( I18n.ERR_617 ) );
                }
            }
        }
    }

}
