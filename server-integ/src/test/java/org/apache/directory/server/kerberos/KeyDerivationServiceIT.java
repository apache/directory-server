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


import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import javax.crypto.spec.DESKeySpec;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.util.Network;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.ldap.handlers.sasl.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.sasl.plain.PlainMechanismHandler;
import org.apache.directory.shared.kerberos.KerberosAttribute;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * An test case for testing the {@link KeyDerivationInterceptor}'s
 * ability to derive Kerberos symmetric keys based on userPassword and principal
 * name and to generate random keys when the special keyword "randomKey" is used.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "KeyDerivationServiceIT-class",
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
    saslMechanisms =
        {
            @SaslMechanism(name = SupportedSaslMechanisms.PLAIN, implClass = PlainMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.CRAM_MD5, implClass = CramMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.DIGEST_MD5, implClass = DigestMd5MechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSSAPI, implClass = GssapiMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.NTLM, implClass = NtlmMechanismHandler.class),
            @SaslMechanism(name = SupportedSaslMechanisms.GSS_SPNEGO, implClass = NtlmMechanismHandler.class)
    },
    extendedOpHandlers =
        {
            StoredProcedureExtendedOperationHandler.class
    })
public class KeyDerivationServiceIT extends AbstractLdapTestUnit
{
    private static final String RDN = "uid=hnelson,ou=users,dc=example,dc=com";


    private void checkKeyNumber( Attributes attributes )
    {
        Attribute krb5key = attributes.get( "krb5key" );

        String vendor = System.getProperty( "java.vm.vendor" );

        if ( vendor.equalsIgnoreCase( "IBM Corporation" ) )
        {
            // Will be 2 or 3 on IBM JRE whether AES-256 is enabled or not
            assertTrue( "Number of keys", krb5key.size() > 1 );
        }
        else if ( vendor.equalsIgnoreCase( "Sun Microsystems Inc." ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on SUN JRE
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else if ( vendor.equalsIgnoreCase( "BEA Systems, Inc." ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on BEA JRockit
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else if ( vendor.equalsIgnoreCase( "Oracle Corporation" ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on Oracle JRockit
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else if ( vendor.equalsIgnoreCase( "Apple Inc." ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on Apple JVM
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else if ( vendor.equalsIgnoreCase( "Apple Inc." ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on Apple JVM
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else if ( vendor.equalsIgnoreCase( "\"Apple Computer, Inc.\"" ) )
        {
            // Could be 4 or 5 depending on whether AES-256 is enabled or not, on Apple JVM
            assertTrue( "Number of keys", krb5key.size() > 3 );
        }
        else
        {
            fail( "Unkown JVM" );
        }
    }


    /**
     * Set up a partition for EXAMPLE.COM, add the Key Derivation interceptor, enable
     * the krb5kdc schema, and add a user principal to test authentication with.
     */
    @Before
    public void setUp() throws Exception
    {
        DirContext schemaRoot = ( DirContext ) getWiredContext( getLdapServer() ).lookup( "ou=schema" );

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

        DirContext ctx = ( DirContext ) getWiredContext( getLdapServer() ).lookup( "dc=example,dc=com" );
        Attributes attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret", "hnelson@EXAMPLE.COM" );
        users.createSubcontext( "uid=hnelson", attrs );
        ctx.close();
    }


    /**
     * Tests that the addition of an entry caused keys to be derived and added.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     */
    @Test
    public void testAddDerivedKeys() throws NamingException, KerberosException, UnknownHostException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, Network.ldapLoopbackUrl( getLdapServer().getPort() )  );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( "java.naming.ldap.attributes.binary", "krb5key" );

        DirContext ctx = new InitialDirContext( env );

        String[] attrIDs =
            { "uid", "userPassword", "krb5Key", "krb5KeyVersionNumber" };

        Attributes attributes = ctx.getAttributes( RDN, attrIDs );

        String uid = null;

        if ( attributes.get( "uid" ) != null )
        {
            uid = ( String ) attributes.get( "uid" ).get();
        }

        assertEquals( uid, "hnelson" );

        byte[] userPassword = null;

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        checkKeyNumber( attributes );

        byte[] testPasswordBytes =
            { ( byte ) 0x73, ( byte ) 0x65, ( byte ) 0x63, ( byte ) 0x72, ( byte ) 0x65, ( byte ) 0x74 };
        assertTrue( Arrays.equals( userPassword, testPasswordBytes ) );

        Attribute krb5key = attributes.get( "krb5key" );

        Map<EncryptionType, EncryptionKey> map = reconstituteKeyMap( krb5key );
        EncryptionKey encryptionKey = map.get( EncryptionType.DES_CBC_MD5 );

        byte[] testKeyBytes =
            { ( byte ) 0xF4, ( byte ) 0xA7, ( byte ) 0x13, ( byte ) 0x64, ( byte ) 0x8A, ( byte ) 0x61, ( byte ) 0xCE,
                ( byte ) 0x5B };

        assertTrue( Arrays.equals( encryptionKey.getKeyValue(), testKeyBytes ) );
        assertEquals( EncryptionType.DES_CBC_MD5, encryptionKey.getKeyType() );

        int keyVersionNumber = -1;

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 0, keyVersionNumber );
        ctx.close();
    }


    /**
     * Tests that the modification of an entry caused keys to be derived and modified.  The
     * modify request contains both the 'userPassword' and the 'krb5PrincipalName'.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     */
    @Test
    public void testModifyDerivedKeys() throws NamingException, KerberosException, UnknownHostException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, Network.ldapLoopbackUrl( getLdapServer().getPort() ) );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( "java.naming.ldap.attributes.binary", "krb5key" );

        DirContext ctx = new InitialDirContext( env );

        String newPrincipalName = "hnelson@EXAMPLE.COM";
        String newUserPassword = "secretsecret";

        // Modify password.
        Attributes attributes = new BasicAttributes( true );
        Attribute attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );
        attr = new BasicAttribute( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, newPrincipalName );
        attributes.put( attr );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        byte[] userPassword = null;

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        checkKeyNumber( attributes );

        byte[] testBytes =
            { 0x73, 0x65, 0x63, 0x72, 0x65, 0x74, 0x73, 0x65, 0x63, 0x72, 0x65, 0x74 };
        assertTrue( Arrays.equals( userPassword, testBytes ) );

        Attribute krb5key = attributes.get( "krb5key" );
        Map<EncryptionType, EncryptionKey> map = reconstituteKeyMap( krb5key );
        EncryptionKey encryptionKey = map.get( EncryptionType.DES_CBC_MD5 );

        byte[] testKeyBytes =
            { ( byte ) 0x16, ( byte ) 0x4A, ( byte ) 0x6D, ( byte ) 0x89, ( byte ) 0x5D, ( byte ) 0x76, ( byte ) 0x0E,
                ( byte ) 0x23 };

        assertTrue( Arrays.equals( encryptionKey.getKeyValue(), testKeyBytes ) );
        assertEquals( EncryptionType.DES_CBC_MD5, encryptionKey.getKeyType() );

        int keyVersionNumber = -1;

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 1, keyVersionNumber );

        newUserPassword = "secretsecretsecret";

        // Modify password.
        attributes = new BasicAttributes( true );
        attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );
        attr = new BasicAttribute( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, newPrincipalName );
        attributes.put( attr );

        person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        assertEquals( "password length", 18, userPassword.length );

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 2, keyVersionNumber );

        newUserPassword = "secretsecretsecretsecret";

        // Modify password.
        attributes = new BasicAttributes( true );
        attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );
        attr = new BasicAttribute( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, newPrincipalName );
        attributes.put( attr );

        person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        assertEquals( "password length", 24, userPassword.length );

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 3, keyVersionNumber );
        ctx.close();
    }


    /**
     * Tests that the modification of an entry caused keys to be derived and modified.  The
     * modify request contains only the 'userPassword'.  The 'krb5PrincipalName' is to be
     * obtained from the initial add of the user principal entry.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     */
    @Test
    public void testModifyDerivedKeysWithoutPrincipalName() throws NamingException, KerberosException,
        UnknownHostException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, Network.ldapLoopbackUrl( getLdapServer().getPort() ) );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( "java.naming.ldap.attributes.binary", "krb5key" );

        DirContext ctx = new InitialDirContext( env );

        String newUserPassword = "secretsecret";

        // Modify password.
        Attributes attributes = new BasicAttributes( true );
        Attribute attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );

        DirContext person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        byte[] userPassword = null;

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        checkKeyNumber( attributes );

        byte[] testBytes =
            { 0x73, 0x65, 0x63, 0x72, 0x65, 0x74, 0x73, 0x65, 0x63, 0x72, 0x65, 0x74 };
        assertTrue( Arrays.equals( userPassword, testBytes ) );

        Attribute krb5key = attributes.get( "krb5key" );
        Map<EncryptionType, EncryptionKey> map = reconstituteKeyMap( krb5key );
        EncryptionKey encryptionKey = map.get( EncryptionType.DES_CBC_MD5 );

        byte[] testKeyBytes =
            { ( byte ) 0x16, ( byte ) 0x4A, ( byte ) 0x6D, ( byte ) 0x89, ( byte ) 0x5D, ( byte ) 0x76, ( byte ) 0x0E,
                ( byte ) 0x23 };

        assertTrue( Arrays.equals( encryptionKey.getKeyValue(), testKeyBytes ) );
        assertEquals( EncryptionType.DES_CBC_MD5, encryptionKey.getKeyType() );

        int keyVersionNumber = -1;

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 1, keyVersionNumber );

        newUserPassword = "secretsecretsecret";

        // Modify password.
        attributes = new BasicAttributes( true );
        attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );

        person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        assertEquals( "password length", 18, userPassword.length );

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 2, keyVersionNumber );

        newUserPassword = "secretsecretsecretsecret";

        // Modify password.
        attributes = new BasicAttributes( true );
        attr = new BasicAttribute( "userPassword", newUserPassword );
        attributes.put( attr );

        person = ( DirContext ) ctx.lookup( RDN );
        person.modifyAttributes( "", DirContext.REPLACE_ATTRIBUTE, attributes );

        // Read again from directory.
        person = ( DirContext ) ctx.lookup( RDN );

        attributes = person.getAttributes( "" );

        if ( attributes.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) attributes.get( "userPassword" ).get();
        }

        assertEquals( "password length", 24, userPassword.length );

        if ( attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ) != null )
        {
            keyVersionNumber = Integer.valueOf( ( String ) attributes
                .get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 3, keyVersionNumber );
        ctx.close();
    }


    /**
     * Tests that the addition of an entry caused random keys to be derived and added.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     * @throws InvalidKeyException if the incorrect key results
     */
    @Test
    public void testAddRandomKeys() throws NamingException, KerberosException, InvalidKeyException,
        UnknownHostException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url",
            Network.ldapLoopbackUrl( getLdapServer().getPort() ) + "/ou=users,dc=example,dc=com" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        env.put( "java.naming.ldap.attributes.binary", "krb5key" );
        DirContext ctx = new InitialDirContext( env );

        Attributes attrs = getPersonAttributes( "Quist", "Thomas Quist", "tquist", "randomKey", "tquist@EXAMPLE.COM" );
        ctx.createSubcontext( "uid=tquist", attrs );

        attrs = getPersonAttributes( "Fryer", "John Fryer", "jfryer", "randomKey", "jfryer@EXAMPLE.COM" );
        ctx.createSubcontext( "uid=jfryer", attrs );

        String[] attrIDs =
            { "uid", "userPassword", "krb5Key" };

        Attributes tquistAttrs = ctx.getAttributes( "uid=tquist", attrIDs );
        Attributes jfryerAttrs = ctx.getAttributes( "uid=jfryer", attrIDs );

        String uid = null;
        byte[] userPassword = null;

        if ( tquistAttrs.get( "uid" ) != null )
        {
            uid = ( String ) tquistAttrs.get( "uid" ).get();
        }

        assertEquals( "tquist", uid );

        if ( tquistAttrs.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) tquistAttrs.get( "userPassword" ).get();
        }

        // Bytes for "randomKey."
        byte[] testPasswordBytes =
            { ( byte ) 0x72, ( byte ) 0x61, ( byte ) 0x6E, ( byte ) 0x64, ( byte ) 0x6F, ( byte ) 0x6D, ( byte ) 0x4B,
                ( byte ) 0x65, ( byte ) 0x79 };
        assertTrue( Arrays.equals( testPasswordBytes, userPassword ) );

        if ( jfryerAttrs.get( "uid" ) != null )
        {
            uid = ( String ) jfryerAttrs.get( "uid" ).get();
        }

        assertEquals( "jfryer", uid );

        if ( jfryerAttrs.get( "userPassword" ) != null )
        {
            userPassword = ( byte[] ) jfryerAttrs.get( "userPassword" ).get();
        }

        assertTrue( Arrays.equals( testPasswordBytes, userPassword ) );

        byte[] testKeyBytes =
            { ( byte ) 0xF4, ( byte ) 0xA7, ( byte ) 0x13, ( byte ) 0x64, ( byte ) 0x8A, ( byte ) 0x61, ( byte ) 0xCE,
                ( byte ) 0x5B };

        Attribute krb5key = tquistAttrs.get( "krb5key" );
        Map<EncryptionType, EncryptionKey> map = reconstituteKeyMap( krb5key );
        EncryptionKey encryptionKey = map.get( EncryptionType.DES_CBC_MD5 );
        byte[] tquistKey = encryptionKey.getKeyValue();

        assertEquals( EncryptionType.DES_CBC_MD5, encryptionKey.getKeyType() );

        krb5key = jfryerAttrs.get( "krb5key" );
        map = reconstituteKeyMap( krb5key );
        encryptionKey = map.get( EncryptionType.DES_CBC_MD5 );
        byte[] jfryerKey = encryptionKey.getKeyValue();

        assertEquals( EncryptionType.DES_CBC_MD5, encryptionKey.getKeyType() );

        assertEquals( "Key length", 8, tquistKey.length );
        assertEquals( "Key length", 8, jfryerKey.length );

        assertFalse( Arrays.equals( testKeyBytes, tquistKey ) );
        assertFalse( Arrays.equals( testKeyBytes, jfryerKey ) );
        assertFalse( Arrays.equals( jfryerKey, tquistKey ) );

        byte[] tquistDerivedKey =
            { ( byte ) 0xFD, ( byte ) 0x7F, ( byte ) 0x6B, ( byte ) 0x83, ( byte ) 0xA4, ( byte ) 0x76, ( byte ) 0xC1,
                ( byte ) 0xEA };
        byte[] jfryerDerivedKey =
            { ( byte ) 0xA4, ( byte ) 0x10, ( byte ) 0x3B, ( byte ) 0x49, ( byte ) 0xCE, ( byte ) 0x0B, ( byte ) 0xB5,
                ( byte ) 0x07 };

        assertFalse( Arrays.equals( tquistDerivedKey, tquistKey ) );
        assertFalse( Arrays.equals( jfryerDerivedKey, jfryerKey ) );

        assertTrue( DESKeySpec.isParityAdjusted( tquistKey, 0 ) );
        assertTrue( DESKeySpec.isParityAdjusted( jfryerKey, 0 ) );
        ctx.close();
    }


    /**
     * Convenience method for creating a person.
     *
     * @param cn the commonName of the person
     * @param sn the surName of the person
     * @param uid the unique id of the person
     * @param userPassword the password of the person
     * @param principal the kerberos principal name for the person
     * @return the attributes of the person entry
     */
    protected Attributes getPersonAttributes( String sn, String cn, String uid, String userPassword, String principal )
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
        attrs.put( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principal );
        attrs.put( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, "0" );

        return attrs;
    }


    /**
     * Convenience method for creating an organizational unit.
     *
     * @param ou the organizational unit to create
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


    private Map<EncryptionType, EncryptionKey> reconstituteKeyMap( Attribute krb5key ) throws NamingException,
        KerberosException
    {
        Map<EncryptionType, EncryptionKey> map = new HashMap<EncryptionType, EncryptionKey>();

        for ( int ii = 0; ii < krb5key.size(); ii++ )
        {
            byte[] encryptionKeyBytes = ( byte[] ) krb5key.get( ii );
            EncryptionKey encryptionKey = KerberosDecoder.decodeEncryptionKey( encryptionKeyBytes );
            map.put( encryptionKey.getKeyType(), encryptionKey );
        }

        return map;
    }
}
