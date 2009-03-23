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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.impl.DefaultDirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import static org.apache.directory.server.integ.ServerIntegrationUtils.getWiredContext;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.io.decoder.EncryptionKeyDecoder;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An test case for testing the {@link KeyDerivationInterceptor}'s
 * ability to derive Kerberos symmetric keys based on userPassword and principal
 * name and to generate random keys when the special keyword "randomKey" is used.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( KeyDerivationServiceIT.Factory.class )
@ApplyLdifs( {
    // Entry #0
    "dn: dc=example,dc=com\n" +
    "dc: example\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n"
    }
)
public class KeyDerivationServiceIT 
{
    private static final String RDN = "uid=hnelson,ou=users,dc=example,dc=com";


    public static LdapService ldapService;

     
     public static class Factory implements LdapServerFactory
     {
         public LdapService newInstance() throws Exception
         {
             DirectoryService service = new DefaultDirectoryService();
             IntegrationUtils.doDelete( service.getWorkingDirectory() );
             service.getChangeLog().setEnabled( true );
             service.setAllowAnonymousAccess( false );
             service.setShutdownHookEnabled( false );

             Set<Partition> partitions = new HashSet<Partition>();
             JdbmPartition partition = new JdbmPartition();
             partition.setId( "example" );
             partition.setSuffix( "dc=example,dc=com" );

             Set<Index<?,ServerEntry>> indexedAttrs = new HashSet<Index<?,ServerEntry>>();
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "ou" ) );
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "dc" ) );
             indexedAttrs.add( new JdbmIndex<String,ServerEntry>( "objectClass" ) );
             partition.setIndexedAttributes( indexedAttrs );

             partitions.add( partition );
             service.setPartitions( partitions );

             List<Interceptor> list = service.getInterceptors();
             list.add( new KeyDerivationInterceptor() );
             service.setInterceptors( list );
             
             // change the working directory to something that is unique
             // on the system and somewhere either under target directory
             // or somewhere in a temp area of the machine.

             LdapService ldapService = new LdapService();
             ldapService.setDirectoryService( service );
             int port = AvailablePortFinder.getNextAvailable( 1024 );
             ldapService.setTcpTransport( new TcpTransport( port ) );
             ldapService.setAllowAnonymousAccess( false );
             ldapService.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

             // Setup SASL Mechanisms
             
             Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String,MechanismHandler>();
             mechanismHandlerMap.put( SupportedSaslMechanisms.PLAIN, new PlainMechanismHandler() );

             CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler );

             DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler );

             GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler );

             NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
             mechanismHandlerMap.put( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
             mechanismHandlerMap.put( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );

             ldapService.setSaslMechanismHandlers( mechanismHandlerMap );
             ldapService.setSaslHost( "localhost" );
             
             return ldapService;
         }
     }
     
     
    /**
     * Set up a partition for EXAMPLE.COM, add the Key Derivation interceptor, enable
     * the krb5kdc schema, and add a user principal to test authentication with.
     */
     @Before
    public void setUp() throws Exception
    {
        DirContext schemaRoot = ( DirContext ) getWiredContext( ldapService ).lookup( "ou=schema" );

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

        DirContext ctx = ( DirContext ) getWiredContext( ldapService ).lookup( "dc=example,dc=com" );
        Attributes attrs = getOrgUnitAttributes( "users" );
        DirContext users = ctx.createSubcontext( "ou=users", attrs );

        attrs = getPersonAttributes( "Nelson", "Horatio Nelson", "hnelson", "secret", "hnelson@EXAMPLE.COM" );
        users.createSubcontext( "uid=hnelson", attrs );
    }


    /**
     * Tests that the addition of an entry caused keys to be derived and added.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     */
     @Test
    public void testAddDerivedKeys() throws NamingException, IOException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getPort() );

        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_PRINCIPAL, "uid=hnelson,ou=users,dc=example,dc=com" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( "java.naming.ldap.attributes.binary", "krb5key" );

        DirContext ctx = new InitialDirContext( env );

        String[] attrIDs =
            { "uid", "userPassword", KerberosAttribute.KRB5_KEY_AT, KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT };

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

        // Could be 4 or 5 depending on whether AES-256 is enabled or not.
        assertTrue( "Number of keys", attributes.get( "krb5key" ).size() > 3 );

        byte[] testPasswordBytes =
            { ( byte ) 0x73, ( byte ) 0x65, ( byte ) 0x63, ( byte ) 0x72, ( byte ) 0x65, ( byte ) 0x74 };
        assertTrue( Arrays.equals( userPassword, testPasswordBytes ) );

        Attribute krb5key = attributes.get( KerberosAttribute.KRB5_KEY_AT );
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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 0, keyVersionNumber );
    }


    /**
     * Tests that the modification of an entry caused keys to be derived and modified.  The
     * modify request contains both the 'userPassword' and the 'krb5PrincipalName'.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     */
     @Test
    public void testModifyDerivedKeys() throws NamingException, IOException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getPort() );

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

        // Could be 4 or 5 depending on whether AES-256 is enabled or not.
        assertTrue( "Number of keys", attributes.get( "krb5key" ).size() > 3 );

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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 3, keyVersionNumber );
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
    public void testModifyDerivedKeysWithoutPrincipalName() throws NamingException, IOException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + ldapService.getPort() );

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

        // Could be 4 or 5 depending on whether AES-256 is enabled or not.
        assertTrue( "Number of keys", attributes.get( "krb5key" ).size() > 3 );

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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
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
            keyVersionNumber = Integer.valueOf( ( String ) attributes.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).get() );
        }

        assertEquals( "Key version number", 3, keyVersionNumber );
    }


    /**
     * Tests that the addition of an entry caused random keys to be derived and added.
     * 
     * @throws NamingException failure to perform LDAP operations
     * @throws IOException on network errors
     * @throws InvalidKeyException if the incorrect key results
     */
     @Test
    public void testAddRandomKeys() throws NamingException, IOException, InvalidKeyException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + ldapService.getPort() + "/ou=users,dc=example,dc=com" );
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
        IOException
    {
        Map<EncryptionType, EncryptionKey> map = new HashMap<EncryptionType, EncryptionKey>();

        for ( int ii = 0; ii < krb5key.size(); ii++ )
        {
            byte[] encryptionKeyBytes = ( byte[] ) krb5key.get( ii );
            EncryptionKey encryptionKey = EncryptionKeyDecoder.decode( encryptionKeyBytes );
            map.put( encryptionKey.getKeyType(), encryptionKey );
        }

        return map;
    }
}
