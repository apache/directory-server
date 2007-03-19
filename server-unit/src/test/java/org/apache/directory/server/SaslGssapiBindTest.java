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
package org.apache.directory.server;


import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.kerberos.kdc.KdcConfiguration;
import org.apache.directory.server.kerberos.shared.jaas.CallbackHandlerBean;
import org.apache.directory.server.kerberos.shared.jaas.Krb5LoginConfiguration;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.ldap.LdapConfiguration;
import org.apache.directory.server.unit.AbstractServerTest;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.mina.util.AvailablePortFinder;


/**
 * An {@link AbstractServerTest} testing SASL GSSAPI authentication
 * and security layer negotiation.  These tests require both the LDAP
 * and the Kerberos protocol.  As with any "three-headed" Kerberos
 * scenario, there are 3 principals:  1 for the test user, 1 for the
 * Kerberos ticket-granting service (TGS), and 1 for the LDAP service.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SaslGssapiBindTest extends AbstractServerTest
{
    private DirContext ctx = null;


    /**
     * Creates a new instance of SaslGssapiBindTest and sets JAAS system properties
     * for the KDC and realm, so we don't have to rely on external configuration.
     */
    public SaslGssapiBindTest()
    {
        System.setProperty( "java.security.krb5.realm", "EXAMPLE.COM" );
        System.setProperty( "java.security.krb5.kdc", "localhost" );
    }


    /**
     * Set up a partition for EXAMPLE.COM and add user and service principals to
     * test authentication with.
     */
    public void setUp() throws Exception
    {
        configuration.setAllowAnonymousAccess( false );

        LdapConfiguration ldapConfig = configuration.getLdapConfiguration();
        ldapConfig.setSaslHost( "localhost" );
        ldapConfig.setSaslPrincipal( "ldap/localhost@EXAMPLE.COM" );

        KdcConfiguration kdcConfig = configuration.getKdcConfiguration();
        kdcConfig.setEnabled( true );
        kdcConfig.setSearchBaseDn( "ou=users,dc=example,dc=com" );
        kdcConfig.setSecurityAuthentication( "simple" );
        kdcConfig.setSecurityCredentials( "secret" );
        kdcConfig.setSecurityPrincipal( "uid=admin,ou=system" );

        Attributes attrs;
        Set<PartitionConfiguration> pcfgs = new HashSet<PartitionConfiguration>();

        MutablePartitionConfiguration pcfg;

        // Add partition 'example'
        pcfg = new MutablePartitionConfiguration();
        pcfg.setName( "example" );
        pcfg.setSuffix( "dc=example,dc=com" );

        Set<Object> indexedAttrs = new HashSet<Object>();
        indexedAttrs.add( "ou" );
        indexedAttrs.add( "dc" );
        indexedAttrs.add( "objectClass" );
        pcfg.setIndexedAttributes( indexedAttrs );

        attrs = new AttributesImpl( true );
        Attribute attr = new AttributeImpl( "objectClass" );
        attr.add( "top" );
        attr.add( "domain" );
        attrs.put( attr );
        attr = new AttributeImpl( "dc" );
        attr.add( "example" );
        attrs.put( attr );
        pcfg.setContextEntry( attrs );

        pcfgs.add( pcfg );
        configuration.setPartitionConfigurations( pcfgs );

        doDelete( configuration.getWorkingDirectory() );
        port = AvailablePortFinder.getNextAvailable( 1024 );
        ldapConfig.setIpPort( port );
        configuration.setShutdownHookEnabled( false );
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
            Attribute disabled = new AttributeImpl( "m-disabled" );
            ModificationItemImpl[] mods = new ModificationItemImpl[]
                { new ModificationItemImpl( DirContext.REMOVE_ATTRIBUTE, disabled ) };
            schemaRoot.modifyAttributes( "cn=Krb5kdc", mods );
        }

        // Get a context, create the ou=users subcontext, then create the 3 principals.
        Hashtable<String, String> env = new Hashtable<String, String>();
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

        attrs = getPrincipalAttributes( "Service", "LDAP Service", "ldap", "randall", "ldap/localhost@EXAMPLE.COM" );
        users.createSubcontext( "uid=ldap", attrs );
    }


    /**
     * Convenience method for creating principals.
     */
    protected Attributes getPrincipalAttributes( String sn, String cn, String uid, String userPassword, String principal )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
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

        KerberosPrincipal servicePrincipal = new KerberosPrincipal( principal );
        char[] password = new String( userPassword ).toCharArray();
        KerberosKey serviceKey = new KerberosKey( servicePrincipal, password, "DES" );

        attrs.put( KerberosAttribute.PRINCIPAL, servicePrincipal.getName() );
        attrs.put( KerberosAttribute.VERSION, Integer.toString( serviceKey.getVersionNumber() ) );
        attrs.put( KerberosAttribute.KEY, serviceKey.getEncoded() );
        attrs.put( KerberosAttribute.TYPE, Integer.toString( serviceKey.getKeyType() ) );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     */
    protected Attributes getOrgUnitAttributes( String ou )
    {
        Attributes attrs = new AttributesImpl();
        Attribute ocls = new AttributeImpl( "objectClass" );
        ocls.add( "top" );
        ocls.add( "organizationalUnit" );
        attrs.put( ocls );
        attrs.put( "ou", ou );

        return attrs;
    }


    /**
     * Tests to make sure GSSAPI binds below the RootDSE work.
     */
    public void testSaslGssapiBind()
    {
        // Use our custom configuration to avoid reliance on external config
        Configuration.setConfiguration( new Krb5LoginConfiguration() );

        // 1. Authenticate to Kerberos.
        LoginContext lc = null;
        try
        {
            lc = new LoginContext( SaslGssapiBindTest.class.getName(), new CallbackHandlerBean( "hnelson", "secret" ) );
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
                try
                {
                    // Create the initial context
                    Hashtable<String, String> env = new Hashtable<String, String>();
                    env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
                    env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );

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
                    fail( "Should not have caught exception:  " + e.getMessage() );
                }

                return null;
            }
        } );
    }


    /**
     * Tear down.
     */
    public void tearDown() throws Exception
    {
        ctx.close();
        ctx = null;
        super.tearDown();
    }
}
