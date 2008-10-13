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


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.ApplyLdifs;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.kerberos.PasswordPolicyInterceptor;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.ldap.LdapService;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.plain.PlainMechanismHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An {@link AbstractServerTest} testing the (@link {@link PasswordPolicyInterceptor}.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( PasswordPolicyServiceIT.Factory.class )
@ApplyLdifs( {
    // Entry #0
    "dn: dc=example,dc=com\n" +
    "dc: example\n" +
    "objectClass: top\n" +
    "objectClass: domain\n\n"
    }
)
public class PasswordPolicyServiceIT 
{
    private DirContext ctx;
    private DirContext users;


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
            list.add( new PasswordPolicyInterceptor() );
            service.setInterceptors( list );
            
            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapService ldapService = new LdapService();
            ldapService.setDirectoryService( service );
            ldapService.setSocketAcceptor( new SocketAcceptor( null ) );
            ldapService.setIpPort( AvailablePortFinder.getNextAvailable( 1024 ) );
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
     * Set up a partition for EXAMPLE.COM, add the {@link PasswordPolicyInterceptor}
     * interceptor, and create a users subcontext.
     */
    @Before
    public void setUp() throws Exception
    {
        Attributes attrs;
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + ldapService.getIpPort() + "/dc=example,dc=com" );
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
