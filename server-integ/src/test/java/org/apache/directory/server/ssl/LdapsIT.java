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
package org.apache.directory.server.ssl;


import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.integ.IntegrationUtils;
import org.apache.directory.server.core.integ.Level;
import org.apache.directory.server.core.integ.annotations.CleanupLevel;
import org.apache.directory.server.core.integ.annotations.Factory;
import org.apache.directory.server.integ.LdapServerFactory;
import org.apache.directory.server.integ.SiRunner;
import org.apache.directory.server.newldap.LdapServer;
import org.apache.directory.server.newldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.SimpleMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.cramMD5.CramMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.digestMD5.DigestMd5MechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.gssapi.GssapiMechanismHandler;
import org.apache.directory.server.newldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.newldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.SocketAcceptor;
import org.apache.directory.server.ssl.SSLSocketFactory;
import org.apache.directory.shared.ldap.constants.SupportedSaslMechanisms;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


/**
 * Test case to verify DIREVE-216.  Starts up the server binds via SUN JNDI provider
 * to perform add modify operations on entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 642496 $
 */
@RunWith ( SiRunner.class ) 
@CleanupLevel ( Level.CLASS )
@Factory ( LdapsIT.Factory.class )
public class LdapsIT
{
    private static final String RDN = "cn=The Person";

    
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
            ldapServer.setEnableLdaps( true );
            ldapServer.setConfidentialityRequired( true );
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

            return ldapServer;
        }
    }
    
    
    /**
     * Create an entry for a person.
     */
    public DirContext getSecureConnection() throws Exception
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( "java.naming.provider.url", "ldap://localhost:" + ldapServer.getIpPort() + "/ou=system" );
        env.put( "java.naming.ldap.factory.socket", SSLSocketFactory.class.getName() );
        env.put( "java.naming.security.principal", "uid=admin,ou=system" );
        env.put( "java.naming.security.credentials", "secret" );
        env.put( "java.naming.security.authentication", "simple" );
        return new InitialDirContext( env );
    }


    /**
     * Just a little test to check if the connection is made successfully.
     * 
     * @throws NamingException cannot create person
     */
    @Test
    public void testLdapS() throws Exception
    {
        // Create a person
        Attributes attributes = new AttributesImpl( true );
        Attribute attribute = new AttributeImpl( "objectClass" );
        attribute.add( "top" );
        attribute.add( "person" );
        attributes.put( attribute );
        attributes.put( "cn", "The Person" );
        attributes.put( "sn", "Person" );
        attributes.put( "description", "this is a person" );
        DirContext ctx = getSecureConnection();
        DirContext person = ctx.createSubcontext( RDN, attributes );

        assertNotNull( person );
    }
}
