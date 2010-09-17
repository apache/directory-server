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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The command line main for the server.  Warning this used to be a simple test
 * case so there really is not much here.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class UberjarMain
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( UberjarMain.class );


    /**
     * Takes a single argument, the path to the installation home, which contains 
     * the configuration to load with server startup settings.
     *
     * @param args the arguments
     */
    public static void main( String[] args ) throws Exception
    {
        if ( ( args != null ) && ( args.length == 2 ) )
        {
            // Creating ApacheDS service
            ApacheDsService service = new ApacheDsService();

            // Creating installation and instance layouts from the arguments
            InstallationLayout installationLayout = new InstallationLayout( args[0] );
            InstanceLayout instanceLayout = new InstanceLayout( args[1] );

            // Initializing the service
            try
            {
                service.init( installationLayout, instanceLayout );
            }
            catch ( Exception e )
            {
                LOG.error( "Failed to start the service.", e );
                System.exit( 1 );
            }

            // Starting the service
            service.start();
        }
        else
        {
            throw new IllegalArgumentException(
                "Program must be launched with 2 arguements (path the the installation directory "
                    + "and path to the instance directory." );
        }
    }
}
