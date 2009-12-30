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

import java.lang.reflect.Method;

import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.protocol.shared.transport.Transport;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.runner.Description;

/**
 * 
 * TODO ServerAnnotationProcessor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ServerAnnotationProcessor
{
    private static void createTransports( LdapServer ldapServer, CreateTransport[] transportBuilders )
    {
        if ( transportBuilders.length != 0 )
        {
            int createdPort = 1024;
            
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
    
    
    private static LdapServer createLdapServer( CreateLdapServer createLdapServer, DirectoryService directoryService )
    {
        if ( createLdapServer != null )
        {
            LdapServer ldapServer = new LdapServer();
            
            ldapServer.setServiceName( createLdapServer.name() );
            
            // Read the transports
            createTransports( ldapServer, createLdapServer.transports() );
            
            // Associate the DS to this LdapServer
            ldapServer.setDirectoryService( directoryService );
            
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

    
    public static LdapServer getLdapServer( DirectoryService directoryService ) throws Exception
    {
        CreateLdapServer createLdapServer = null;
        
        // Get the caller by inspecting the stackTrace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // Get the enclosing class
        Class<?> classCaller = Class.forName( stackTrace[2].getClassName() );
        
        // Get the current method
        String methodCaller = stackTrace[2].getMethodName();
        
        // Check if we have any annotation associated with the method
        Method[] methods = classCaller.getMethods();
        
        for ( Method method : methods )
        {
            if ( methodCaller.equals( method.getName() ) )
            {
                createLdapServer = method.getAnnotation( CreateLdapServer.class );
                
                if ( createLdapServer != null )
                {
                    break;
                }
            }
        }

        // No : look at the class level
        if ( createLdapServer == null )
        {
            createLdapServer = classCaller.getAnnotation( CreateLdapServer.class );
        }
        
        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService );
    }


    public static LdapServer getLdapServer( Description description, DirectoryService directoryService ) throws Exception
    {
        CreateLdapServer createLdapServer = description.getAnnotation( CreateLdapServer.class );

        // Ok, we have found a CreateLdapServer annotation. Process it now.
        return createLdapServer( createLdapServer, directoryService );
    }
}
