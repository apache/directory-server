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
package org.apache.directory.server.operations.extended;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.ExtendedOperationHandler;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.newldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;
import org.apache.directory.shared.ldap.sp.JavaStoredProcUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( StoredProcedureIT.Factory.class )
public class StoredProcedureIT
{
    private LdapContext ctx;
    private LdapContext spCtx;
    private Map<String, OidNormalizer> oids;

    
    public static LdapServer ldapServer;

    
    public static class Factory implements LdapServerFactory
    {
        public LdapServer newInstance() throws Exception
        {
            DirectoryService service = new DefaultDirectoryService();
            IntegrationUtils.doDelete( service.getWorkingDirectory() );
            service.getChangeLog().setEnabled( true );
            service.setShutdownHookEnabled( false );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            LdapServer ldapServer = new LdapServer();
            ldapServer.setDirectoryService( service );
            ldapServer.setSocketAcceptor( new SocketAcceptor( null ) );
            ldapServer.setIpPort( AvailablePortFinder.getNextAvailable( 1024 ) );
            ldapServer.setEnabled( true );
            ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            // Setup SASL Mechanisms
            
            Map<String, MechanismHandler> mechanismHandlerMap = new HashMap<String,MechanismHandler>();
            mechanismHandlerMap.put( SupportedSaslMechanisms.PLAIN, new SimpleMechanismHandler() );

            CramMd5MechanismHandler cramMd5MechanismHandler = new CramMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.CRAM_MD5, cramMd5MechanismHandler );

            DigestMd5MechanismHandler digestMd5MechanismHandler = new DigestMd5MechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.DIGEST_MD5, digestMd5MechanismHandler );

            GssapiMechanismHandler gssapiMechanismHandler = new GssapiMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSSAPI, gssapiMechanismHandler );

            NtlmMechanismHandler ntlmMechanismHandler = new NtlmMechanismHandler();
            mechanismHandlerMap.put( SupportedSaslMechanisms.NTLM, ntlmMechanismHandler );
            mechanismHandlerMap.put( SupportedSaslMechanisms.GSS_SPNEGO, ntlmMechanismHandler );

            ldapServer.setSaslMechanismHandlers( mechanismHandlerMap );

            Set<ExtendedOperationHandler> handlers = new HashSet<ExtendedOperationHandler>( ldapServer.getExtendedOperationHandlers() );
            handlers.add( new StoredProcedureExtendedOperationHandler() );
            ldapServer.setExtendedOperationHandlers( handlers );

            return ldapServer;
        }
    }
    
    
    @Before 
    public void setUp() throws Exception
    {
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + ldapServer.getIpPort() + "/ou=system" );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        ctx = new InitialLdapContext( env, null );
        
        Attributes spContainer = new AttributesImpl( "objectClass", "top", true );
        spContainer.get( "objectClass" ).add( "organizationalUnit" );
        spContainer.put( "ou", "Stored Procedures" );
        spCtx = ( LdapContext ) ctx.createSubcontext( "ou=Stored Procedures", spContainer );
        assertNotNull( spCtx );
        
        // Initialize OIDs maps for normalization
        oids = new HashMap<String, OidNormalizer>();

        oids.put( "ou", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "organizationalUnitName", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
        oids.put( "2.5.4.11", new OidNormalizer( "ou", new DeepTrimToLowerNormalizer() ) );
    }

    
    @Test
    public void testExecuteProcedureWithReturnValue() throws Exception
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHello";
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        Object response = JavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[] { } );
        assertEquals( "Hello World!", response );
    }
    

    @Test
    public void testExecuteProcedureWithParametersAndReturnValue() throws Exception
    {
        String procedureName = HelloWorldProcedure.class.getName() + ":sayHelloTo";
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, HelloWorldProcedure.class );
        Object response = JavaStoredProcUtils.callStoredProcedure( ctx, procedureName, new Object[] { "Ersin" } );
        assertEquals( "Hello Ersin!", response );
    }
    
    
    /*
    @Test public void testSPDeleteSubtree() throws Exception
    {
        String ldif =
            "version: 1\n" +
            "\n" +
            "dn: ou=People,ou=system\n" +
            "ou: People\n" +
            "objectclass: organizationalUnit\n" +
            "objectclass: top\n" +
            "\n" + 
            "dn: cn=John,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: John\n" +
            "cn: John\n" +
            "\n" +
            "dn: cn=Jane,ou=People,ou=system\n" +
            "objectclass: person\n" +
            "objectclass: top\n" +
            "sn: Jane\n" +
            "cn: Jane\n";
        
        injectEntries( ldif );
        
        JavaStoredProcUtils.loadStoredProcedureClass( spCtx, DITUtilitiesSP.class );
        
        LdapDN people = new LdapDN( "ou=People" );
        people = LdapDN.normalize(  people, oids );
        
        String spName = DITUtilitiesSP.class.getName() + ":deleteSubtree";
        Object[] params = new Object[] { new LdapContextParameter( "ou=system" ),
                                         people };
        
        
        JavaStoredProcUtils.callStoredProcedure( ctx, spName, params );
        
        try
        {
            sysRoot.lookup( "cn=Jane,ou=People" );
            sysRoot.lookup( "cn=John,ou=People" );
            sysRoot.lookup( "ou=People" );
            fail( "We should not have come here." );
        }
        catch ( NameNotFoundException e )
        {
            // Expected
        }
    }
    */
}
