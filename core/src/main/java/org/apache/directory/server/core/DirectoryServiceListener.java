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
package org.apache.directory.server.core;


import javax.naming.NamingException;


/**
 * An event handler that listens to the changes occurs to
 * {@link DirectoryService}.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public interface DirectoryServiceListener
{
    /**
     * Invoked before starting up {@link DirectoryService}.
     */
    void beforeStartup( DirectoryService service ) throws NamingException;


    /**
     * Invoked after starting up {@link DirectoryService}.
     */
    void afterStartup( DirectoryService service ) throws NamingException;


    /**
     * Invoked before shutting down {@link DirectoryService}.
     */
    void beforeShutdown( DirectoryService service ) throws NamingException;


    /**
     * Invoked after shutting down {@link DirectoryService}.
     */
    void afterShutdown( DirectoryService service ) throws NamingException;


    /**
     * Invoked before calling {@link DirectoryService#sync()}.
     */
    void beforeSync( DirectoryService service ) throws NamingException;


    /**
     * Invoked after calling {@link DirectoryService#sync()}.
     */
    void afterSync( DirectoryService service ) throws NamingException;
}
