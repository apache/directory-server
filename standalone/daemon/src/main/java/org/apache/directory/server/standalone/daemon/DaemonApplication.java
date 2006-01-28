/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.standalone.daemon;


/**
 * Interface used by DaemonApplications.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DaemonApplication
{
    /**
     * Threads should be created, along with sockets.  
     * 
     * @param layout the application's installation home layout 
     * @param args the shifted arguments after the installation home path and 
     * the command arguments are removed
     */
    void init( InstallationLayout layout, String[] args ) throws Exception;
    
    /**
     * Start threads and bind sockets here.
     */
    void start();
    
    /**
     * Stop threads and close sockets opened in start() here.
     * 
     * @param args shifted arguments without installation path or stop command
     * @throws Exception 
     */
    void stop( String[] args ) throws Exception;
    
    /**
     * The application should destroy resources created in init() here.
     */
    void destroy();
}
