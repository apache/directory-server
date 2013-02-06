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


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.annotations.CreateChngPwdServer;
import org.apache.directory.api.ldap.model.constants.SupportedSaslMechanisms;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.annotations.CreateConsumer;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.annotations.SaslMechanism;
import org.apache.directory.server.core.annotations.AnnotationUtils;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.ChangePasswordConfig;
import org.apache.directory.server.kerberos.KerberosConfig;
import org.apache.directory.server.kerberos.changepwd.ChangePasswordServer;
import org.apache.directory.server.kerberos.kdc.KdcServer;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.bind.MechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmMechanismHandler;
import org.apache.directory.server.ldap.handlers.bind.ntlm.NtlmProvider;
import org.apache.directory.server.ldap.replication.SyncReplConfiguration;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumer;
import org.apache.directory.server.ldap.replication.consumer.ReplicationConsumerImpl;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.directory.server.protocol.shared.transport.UdpTransport;
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
    private static void createTransports( LdapServer ldapServer, CreateTransport[] transportBuilders )
    {
        if ( transportBuilders.length != 0 )
        {
            for ( CreateTransport transportBuilder : transportBuilders )
            {
                String protocol = transportBuilder.protocol();
                int port = transportBuilder.port();
                int nbThreads = transportBuilder.nbThreads();
                int backlog = transportBuilder.backlog();
                String address = transportBuilder.address();

                if ( port <= 0 )
                {
                    try
                    {
                        ServerSocket ss = new ServerSocket( 0 );

                        port = ss.getLocalPort();

                        ss.close();
                    }
                    catch ( IOException ioe )
                    {
                        // Don't know what to do here...
                    }
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


    /**
     * Just gives an instance of {@link LdapServer} without starting it.
     * For getting a running LdapServer instance see {@link #createLdapServer(CreateLdapServer, DirectoryService)}
     * @see #createLdapServer(CreateLdapServer, DirectoryService)
     */
    public static LdapServer instantiateLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService )
    {
        if ( createLdapServer != null )
        {
            LdapServer ldapServer = new LdapServer();

            ldapServer.setServiceName( createLdapServer.name() );

            // Read the transports
            createTransports( ldapServer, createLdapServer.transports() );

            // Associate the DS to this LdapServer
            ldapServer.setDirectoryService( directoryService );

            ldapServer.setSaslHost( createLdapServer.saslHost() );

            ldapServer.setSaslPrincipal( createLdapServer.saslPrincipal() );

            if ( !Strings.isEmpty( createLdapServer.keyStore() ) )
            {
                ldapServer.setKeystoreFile( createLdapServer.keyStore() );
                ldapServer.setCertificatePassword( createLdapServer.certificatePassword() );
            }

            for ( Class<?> extOpClass : createLdapServer.extendedOpHandlers() )
            {
                try
                {
                    ExtendedOperationHandler extOpHandler = ( ExtendedOperationHandler ) extOpClass.newInstance();
                    ldapServer.addExtendedOperationHandler( extOpHandler );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( I18n.err( I18n.ERR_690, extOpClass.getName() ), e );
                }
            }

            for ( SaslMechanism saslMech : createLdapServer.saslMechanisms() )
            {
                try
                {
                    MechanismHandler handler = ( MechanismHandler ) saslMech.implClass().newInstance();
                    ldapServer.addSaslMechanismHandler( saslMech.name(), handler );
                }
                catch ( Exception e )
                {
                    throw new RuntimeException(
                        I18n.err( I18n.ERR_691, saslMech.name(), saslMech.implClass().getName() ), e );
                }
            }

            NtlmMechanismHandler ntlmHandler = ( NtlmMechanismHandler ) ldapServer.getSaslMechanismHandlers().get(
                SupportedSaslMechanisms.NTLM );

            if ( ntlmHandler != null )
            {
                Class<?> ntlmProviderClass = createLdapServer.ntlmProvider();
                // default value is a invalid Object.class
                if ( ( ntlmProviderClass != null ) && ( ntlmProviderClass != Object.class ) )
                {
                    try
                    {
                        ntlmHandler.setNtlmProvider( ( NtlmProvider ) ntlmProviderClass.newInstance() );
                    }
                    catch ( Exception e )
                    {
                        throw new RuntimeException( I18n.err( I18n.ERR_692 ), e );
                    }
                }
            }
            
            List<String> realms = new ArrayList<String>();
            for( String s : createLdapServer.saslRealms() )
            {
                realms.add( s );
            }
            
            ldapServer.setSaslRealms( realms );
            
            return ldapServer;
        }
        else
        {
            return null;
        }
    }


    /**
     * Returns an LdapServer instance and starts it before returning the instance, infering
     * the configuration from the Stack trace
     *  
     * @return a running LdapServer instance
     */
    public static LdapServer getLdapServer( DirectoryService directoryService ) throws ClassNotFoundException
    {
        Object instance = AnnotationUtils.getInstance( CreateLdapServer.class );
        LdapServer ldapServer = null;

        if ( instance != null )
        {
            CreateLdapServer createLdapServer = ( CreateLdapServer ) instance;

            ldapServer = createLdapServer( createLdapServer, directoryService );
        }

        return ldapServer;
    }


    /**
     * Create a replication consumer
     */
    private static ReplicationConsumer createConsumer( CreateConsumer createConsumer )
    {
        ReplicationConsumer consumer = new ReplicationConsumerImpl();

        SyncReplConfiguration config = new SyncReplConfiguration();
        config.setRemoteHost( createConsumer.remoteHost() );
        config.setRemotePort( createConsumer.remotePort() );
        config.setReplUserDn( createConsumer.replUserDn() );
        config.setReplUserPassword( Strings.getBytesUtf8( createConsumer.replUserPassword() ) );
        config.setUseTls( createConsumer.useTls() );
        config.setBaseDn( createConsumer.baseDn() );
        config.setRefreshInterval( createConsumer.refreshInterval() );

        consumer.setConfig( config );

        return consumer;
    }


    /**
     * creates an LdapServer and starts before returning the instance, infering
     * the configuration from the Stack trace
     *  
     * @return a running LdapServer instance
     */
    public static ReplicationConsumer createConsumer() throws ClassNotFoundException
    {
        Object instance = AnnotationUtils.getInstance( CreateConsumer.class );
        ReplicationConsumer consumer = null;

        if ( instance != null )
        {
            CreateConsumer createConsumer = ( CreateConsumer ) instance;

            consumer = createConsumer( createConsumer );
        }

        return consumer;
    }


    /**
     * creates an LdapServer and starts before returning the instance
     *  
     * @param createLdapServer the annotation containing the custom configuration
     * @param directoryService the directory service
     * @return a running LdapServer instance
     */
    private static LdapServer createLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService )
    {
        LdapServer ldapServer = instantiateLdapServer( createLdapServer, directoryService );

        if ( ldapServer == null )
        {
            return null;
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


    /**
     * Create a new instance of LdapServer
     *
     * @param description A description for the created LdapServer
     * @param directoryService The associated DirectoryService
     * @param startPort The port used by the server
     * @return An LdapServer instance 
     * @throws Exception If the server cannot be started
     */
    public static LdapServer createLdapServer( Description description, DirectoryService directoryService )
        throws Exception
    {
        CreateLdapServer createLdapServer = description.getAnnotation( CreateLdapServer.class );

        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService );
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

        KerberosConfig kdcConfig = new KerberosConfig();
        kdcConfig.setServicePrincipal( createKdcServer.kdcPrincipal() );
        kdcConfig.setPrimaryRealm( createKdcServer.primaryRealm() );
        kdcConfig.setMaximumTicketLifetime( createKdcServer.maxTicketLifetime() );
        kdcConfig.setMaximumRenewableLifetime( createKdcServer.maxRenewableLifetime() );
        
        KdcServer kdcServer = new KdcServer( kdcConfig );
        
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
                Transport t = createTransport( transportBuilder, startPort );
                startPort = t.getPort() + 1;
                kdcServer.addTransports( t );
            }
        }

        CreateChngPwdServer[] createChngPwdServers = createKdcServer.chngPwdServer();
        
        
        if( createChngPwdServers.length > 0 )
        {
            
            CreateChngPwdServer createChngPwdServer = createChngPwdServers[0];
            ChangePasswordConfig config = new ChangePasswordConfig( kdcConfig );
            config.setServicePrincipal( createChngPwdServer.srvPrincipal() );
            
            ChangePasswordServer chngPwdServer = new ChangePasswordServer( config );
            
            for( CreateTransport transportBuilder : createChngPwdServer.transports() )
            {
                Transport t = createTransport( transportBuilder, startPort );
                startPort = t.getPort() + 1;
                chngPwdServer.addTransports( t );
            }
            
            chngPwdServer.setDirectoryService( directoryService );
            
            kdcServer.setChangePwdServer( chngPwdServer );
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
    
    
    public static Transport createTransport( CreateTransport transportBuilder, int startPort )
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
            return tcp;
        }
        else if ( protocol.equalsIgnoreCase( "UDP" ) )
        {
            UdpTransport udp = new UdpTransport( address, port );
            return udp;
        }
        else
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_689, protocol ) );
        }
    }
    
    
    public static KdcServer getKdcServer( Description description, DirectoryService directoryService, int startPort ) throws Exception
    {
        CreateKdcServer createLdapServer = description.getAnnotation( CreateKdcServer.class );

        return createKdcServer( createLdapServer, directoryService, startPort );
    }

}
