/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmProvider;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
import org.apache.directory.shared.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.runner.Description;

/**
 * 
 * Annotation processor for creating LDAP and Kerberos servers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ServerAnnotationProcessor
{
    private static void createTransports( LdapServer ldapServer, CreateTransport[] transportBuilders, int startPort )
    {
        if ( transportBuilders.length != 0 )
        {
            int createdPort = startPort;
            
            for ( CreateTransport transportBuilder : transportBuilders )
            {
                String protocol = transportBuilder.protocol();
                int port = transportBuilder.port();
                int nbThreads = transportBuilder.nbThreads();
                int backlog = transportBuilder.backlog();
                String address = transportBuilder.address();
                
                if ( port == -1 )
                {
                    port = AvailablePortFinder.getNextAvailable( createdPort );
                    createdPort = port + 1;
                }
                
                if ( protocol.equalsIgnoreCase( "LDAP" ) )
                {
                    Transport ldap = new TcpTransport( address, port, nbThreads, backlog );
                    ldapServer.addTransports( ldap );
                }
                else if ( protocol.equalsIgnoreCase( "LDAPS" ) )
                {
                    Transport ldaps = new TcpTransport( address, port, nbThreads, backlog );
                    ldaps.setEnableSSL( true );
                    ldapServer.addTransports( ldaps );
                }
                else
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_689, protocol ) );
                }
            }
        }
        else
        {
            // Create default LDAP and LDAPS transports
            int port = AvailablePortFinder.getNextAvailable( 1024 );
            Transport ldap = new TcpTransport( port );
            ldapServer.addTransports( ldap );
            
            port = AvailablePortFinder.getNextAvailable( port );
            Transport ldaps = new TcpTransport( port );
            ldaps.setEnableSSL( true );
            ldapServer.addTransports( ldaps );
        }
    }
    
    
    private static LdapServer createLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService, int startPort )
    {
        if ( createLdapServer != null )
        {
            LdapServer ldapServer = new LdapServer();
            
            ldapServer.setServiceName( createLdapServer.name() );
            
            // Read the transports
            createTransports( ldapServer, createLdapServer.transports(), startPort );
            
            // Associate the DS to this LdapServer
            ldapServer.setDirectoryService( directoryService );

            ldapServer.setSaslHost( createLdapServer.saslHost() );
            
            ldapServer.setSaslPrincipal( createLdapServer.saslPrincipal() );
            
            for( Class<?> extOpClass : createLdapServer.extendedOpHandlers() )
            {
                try
                {
                    ExtendedOperationHandler extOpHandler = ( ExtendedOperationHandler ) extOpClass.newInstance();
                    ldapServer.addExtendedOperationHandler( extOpHandler );
                }
                catch( Exception e )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_690, extOpClass.getName() ), e );
                }
            }
            
            for( SaslMechanism saslMech : createLdapServer.saslMechanisms() )
            {
                try
                {
                    MechanismHandler handler = ( MechanismHandler ) saslMech.implClass().newInstance();
                    ldapServer.addSaslMechanismHandler( saslMech.name(), handler );
                }
                catch( Exception e )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_691, saslMech.name(), saslMech.implClass().getName() ), e );
                }
            }
            
            NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get( SupportedSaslMechanisms.NTLM );
            if( ntlmHandler != null )
            {
                Class<?> ntlmProviderClass = createLdapServer.ntlmProvider();
                // default value is a invalid Object.class
                if( ( ntlmProviderClass != null ) && ( ntlmProviderClass != Object.class ) )
                {
                    try
                    {
                        ntlmHandler.setNtlmProvider( ( NtlmProvider ) ntlmProviderClass.newInstance() );
                    }
                    catch( Exception e )
                    {
                        throw new RuntimeException( I18n.err( I18n.ERR_692 ), e );
                    }
                }
            }
        
            // Launch the server
            try
            {
                ldapServer.start();
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
            
            return ldapServer;
        }
        else
        {
            return null;
        }
    }


    /**
     * Create a new instance of LdapServer
     *
     * @param directoryService The associated DirectoryService
     * @param startPort The port used by the server
     * @return An LdapServer instance 
     * @throws Exception If the server cannot be started
     */
    public static LdapServer createLdapServer( DirectoryService directoryService, int startPort ) throws Exception
    {
        CreateLdapServer createLdapServer = ( CreateLdapServer ) getAnnotation( CreateLdapServer.class );
        
        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService, startPort );
    }


    /**
     * Create a new instance of LdapServer
     *
     * @param description A description for the created LdapServer
     * @param directoryService The associated DirectoryService
     * @param startPort The port used by the server
     * @return An LdapServer instance 
     * @throws Exception If the server cannot be started
     */
    public static LdapServer createLdapServer( Description description, DirectoryService directoryService, int startPort )
        throws Exception
    {
        CreateLdapServer createLdapServer = description.getAnnotation( CreateLdapServer.class );

        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService, startPort );
    }


    @SuppressWarnings("unchecked")
    private static Annotation getAnnotation( Class annotationClass ) throws Exception
    {
        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        // In Java5 the 0th stacktrace element is: java.lang.Thread.dumpThreads(Native Method)
        int index = stackTrace[0].getMethodName().equals( "dumpThreads" ) ? 4 : 3;

        // Get the enclosing class
        Class<?> classCaller = Class.forName( stackTrace[index].getClassName() );

        // Get the current method
        String methodCaller = stackTrace[index].getMethodName();

        // Check if we have any annotation associated with the method
        Method[] methods = classCaller.getMethods();
        
        for ( Method method : methods )
        {
            if ( methodCaller.equals( method.getName() ) )
            {
                Annotation annotation = method.getAnnotation( annotationClass );
                
                if ( annotation != null )
                {
                    return annotation;
                }
            }
        }
        
        // No : look at the class level
        return classCaller.getAnnotation( annotationClass );
    }
    
    
    public static KdcServer getKdcServer( DirectoryService directoryService, int startPort ) throws Exception
    {
        CreateKdcServer createKdcServer = ( CreateKdcServer ) getAnnotation( CreateKdcServer.class );

        return createKdcServer( createKdcServer, directoryService, startPort );
    }

    
    private static KdcServer createKdcServer( CreateKdcServer createKdcServer, DirectoryService directoryService, int startPort )
    {
        if( createKdcServer == null )
        {
            return null;
        }
        
        KdcServer kdcServer = new KdcServer();
        kdcServer.setServiceName( createKdcServer.name() );
        kdcServer.setKdcPrincipal( createKdcServer.kdcPrincipal() );
        kdcServer.setPrimaryRealm( createKdcServer.primaryRealm() );
        kdcServer.setMaximumTicketLifetime( createKdcServer.maxTicketLifetime() );
        kdcServer.setMaximumRenewableLifetime( createKdcServer.maxRenewableLifetime() );
        
        CreateTransport[] transportBuilders = createKdcServer.transports();
        
        if( transportBuilders == null )
        {
            // create only UDP transport if none specified
            UdpTransport defaultTransport = new UdpTransport( AvailablePortFinder.getNextAvailable( startPort ) );
            kdcServer.addTransports( defaultTransport );
        }
        else if( transportBuilders.length > 0 )
        {
            for( CreateTransport transportBuilder : transportBuilders )
            {
                String protocol = transportBuilder.protocol();
                int port = transportBuilder.port();
                int nbThreads = transportBuilder.nbThreads();
                int backlog = transportBuilder.backlog();
                String address = transportBuilder.address();

                if ( port == -1 )
                {
                    port = AvailablePortFinder.getNextAvailable( startPort );
                    startPort = port + 1;
                }
                
                if ( protocol.equalsIgnoreCase( "TCP" ) )
                {
                    Transport tcp = new TcpTransport( address, port, nbThreads, backlog );
                    kdcServer.addTransports( tcp );
                }
                else if ( protocol.equalsIgnoreCase( "UDP" ) )
                {
                    UdpTransport udp = new UdpTransport( address, port );
                    kdcServer.addTransports( udp );
                }
                else
                {
                    throw new IllegalArgumentException( I18n.err( I18n.ERR_689, protocol ) );
                }
            }
        }
        
        kdcServer.setDirectoryService( directoryService );
        
        // Launch the server
        try
        {
            kdcServer.start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
        
        return kdcServer;
    }
    
    
    public static KdcServer getKdcServer( Description description, DirectoryService directoryService, int startPort ) throws Exception
    {
        CreateKdcServer createLdapServer = description.getAnnotation( CreateKdcServer.class );

        return createKdcServer( createLdapServer, directoryService, startPort );
    }

}
