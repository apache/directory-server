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


import java.io.File;

import org.apache.directory.daemon.InstallationLayout;


/**
 * The command line main for the server.  Warning this used to be a simple test
 * case so there really is not much here.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UberjarMain
{
    /**
     * Takes a single argument, the path to the installation home, which contains 
     * the configuration to load with server startup settings.
     *
     * @param args the arguments
     */
    public static void main( String[] args ) throws Exception
    {
        Service server = new Service();

        if ( args.length > 0 && new File( args[0] ).isDirectory() )
        {
            server.init( new InstallationLayout( args[0] ), null );
            server.start();
        }
        else if ( args.length > 0 && new File( args[0] ).isFile() )
        {
            server.init( null, args );
            server.start();
        }
        else
        {
            server.init( null, null );
            server.start();
        }
    }
}
