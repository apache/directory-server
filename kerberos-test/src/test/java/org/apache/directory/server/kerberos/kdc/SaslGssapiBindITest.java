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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.directory.ldap.client.api.Krb5LoginConfiguration;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.junit.After;
import org.junit.Before;
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
public class SaslGssapiBindITest extends AbstractLdapTestUnit
{
    private DirContext ctx;

    /** the context root for the schema */
    protected LdapContext schemaRoot;

    /** the context root for the system partition */
    protected LdapContext sysRoot;

    /** the context root for the rootDSE */
    protected CoreSession rootDse;


    /**
     * Creates a new instance of SaslGssapiBindTest and sets JAAS system properties.
     */
    public SaslGssapiBindITest()
    {
        String krbConfPath = getClass().getClassLoader().getResource( "krb5.conf" ).getFile();
        System.setProperty( "java.security.krb5.conf", krbConfPath );
        System.setProperty( "sun.security.krb5.debug", "false" );
    }


    /**
     * Set up a partition for EXAMPLE.COM and add user and service principals to
     * test authentication with.
     */
    @Before
    public void setUp() throws Exception
    {
        // On Windows 7 and Server 2008 the loopback address 127.0.0.1
        // isn't resolved to localhost by default. In that case we need
        // to use the IP address for the service principal.
        String hostName;
        try
        {
            InetAddress loopback = InetAddress.getByName( "127.0.0.1" );
            hostName = loopback.getHostName();
        }
        catch ( UnknownHostException e )
        {
            System.err.println( "Can't find loopback address '127.0.0.1', using hostname 'localhost'" );
            hostName = "localhost";
        }
        String servicePrincipal = "ldap/" + hostName + "@EXAMPLE.COM";
        getLdapServer().setSaslPrincipal( servicePrincipal );

        Attributes attrs;

        setContexts( "uid=admin,ou=system", "secret" );

        // -------------------------------------------------------------------
        // Enable the krb5kdc schema
        // -------------------------------------------------------------------

        // check if krb5kdc is disabled
        Attributes krb5kdcAttrs = schemaRoot.getAttributes( "cn=Krb5kdc" );
        boolean isKrb5KdcDisabled = false;
        if ( krb5kdcAttrs.get( "m-disabled" ) != null )
        {
            isKrb5KdcDisabled = ( ( String ) krb5kdcAttrs.get( "m-disabled" ).get() ).equalsIgnoreCase( "TRUE" );
        }

        // if krb5kdc is disabled then enable it
        if ( isKrb5KdcDisabled )
        {
            Attribute disabled = new BasicAttribute( "m-disabled" );
            ModificationItem[] mods = new ModificationItem[]
                { new ModificationItem( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=Krb5kdc", mods );
        }

        // Get a context, create the ou=users subcontext, then create the 3 principals.
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        ctx = new InitialDirContext( env );

        attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPrincipalAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret", "hnelson@EXAMPLE.COM" );
        users.createSubcontext( "uid=hnelson", attrs );

        attrs = getPrincipalAttributes( "Service", "KDC Service", "krbtgt", "secret", "krbtgt/EXAMPLE.COM@EXAMPLE.COM" );
        users.createSubcontext( "uid=krbtgt", attrs );

        attrs = getPrincipalAttributes( "Service", "LDAP Service", "ldap", "randall", servicePrincipal );
        users.createSubcontext( "uid=ldap", attrs );
    }


    /**
     * Convenience method for creating principals.
     *
     * @param cn           the commonName of the person
     * @param principal    the kerberos principal name for the person
     * @param sn           the surName of the person
     * @param uid          the unique identifier for the person
     * @param userPassword the credentials of the person
     * @return the attributes of the person principal
     */
    protected Attributes getPrincipalAttributes( String sn, String cn, String uid, String userPassword, String principal )
    {
        Attributes attrs = new BasicAttributes( true );
        Attribute ocls = new BasicAttribute( "objectClass" );
        ocls.add( "top" );
        ocls.add( "person" ); // sn $ cn
        ocls.add( "inetOrgPerson" ); // uid
        ocls.add( "krb5principal" );
        ocls.add( "krb5kdcentry" );
        attrs.put( ocls );
        attrs.put( "cn", cn );
        attrs.put( "sn", sn );
        attrs.put( "uid", uid );
        attrs.put( "userPassword", userPassword );
        attrs.put( "krb5PrincipalName", principal );
        attrs.put( "krb5KeyVersionNumber", "0" );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     *
     * @param ou the ou of the organizationalUnit
     * @return the attributes of the organizationalUnit
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


    /**
     * Tests to make sure GSSAPI binds below the RootDSE work.
     */
    @Test
    public void testSaslGssapiBind()
    {
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
        Subject.doAs( lc.getSubject(), new PrivilegedAction()
        {
            public Object run()
            {
                //FIXME activate this code as soon as the GSSAPIMechanismHandler is fixed.
                //Currently GSSAPI authentication for the ldap server is broken
                try
                {
                    // Create the initial context
                    Hashtable<String, String> env = new Hashtable<String, String>();
                    env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
                    env.put( Context.PROVIDER_URL, "ldap://localhost:" + getLdapServer().getPort() );

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

                    assertEquals( uid, "hnelson" );
                }
                catch ( NamingException e )
                {
                    fail( "Should not have caught exception:  " + e.getMessage() + e.getRootCause() );
                }

                return null;
            }
        } );

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


    // copied the below two methods from AbstractServerTest
    /**
     * Sets the contexts for this base class.  Values of user and password used to
     * set the respective JNDI properties.  These values can be overriden by the
     * overrides properties.
     *
     * @param user the username for authenticating as this user
     * @param passwd the password of the user
     * @throws NamingException if there is a failure of any kind
     */
    protected void setContexts( String user, String passwd ) throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( DirectoryService.JNDI_KEY, getService() );
        env.put( Context.SECURITY_PRINCIPAL, user );
        env.put( Context.SECURITY_CREDENTIALS, passwd );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        setContexts( env );
    }


    /**
     * Sets the contexts of this class taking into account the extras and overrides
     * properties.  
     *
     * @param env an environment to use while setting up the system root.
     * @throws NamingException if there is a failure of any kind
     */
    protected void setContexts( Hashtable<String, Object> env ) throws Exception
    {
        Hashtable<String, Object> envFinal = new Hashtable<String, Object>( env );
        envFinal.put( Context.PROVIDER_URL, ServerDNConstants.SYSTEM_DN );
        sysRoot = new InitialLdapContext( envFinal, null );

        envFinal.put( Context.PROVIDER_URL, "" );
        rootDse = getService().getAdminSession();

        envFinal.put( Context.PROVIDER_URL, SchemaConstants.OU_SCHEMA );
        schemaRoot = new InitialLdapContext( envFinal, null );
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
