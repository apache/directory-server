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
package org.apache.directory.server.core.jndi;


import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.DirectoryService;


/**
 * A simplistic implementation of {@link AbstractContextFactory}.
 * This class simply extends {@link AbstractContextFactory} and leaves all
 * abstract event listener methods as empty.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CoreContextFactory extends AbstractContextFactory implements InitialContextFactory
{
    /**
     * Creates a new instance.
     */
    public CoreContextFactory()
    {
    }


    /**
     * Does nothing by default.
     */
    public void beforeStartup( DirectoryService service ) throws NamingException
    {
    }


    /**
     * Does nothing by default.
     */
    public void afterStartup( DirectoryService service ) throws NamingException
    {
    }


    /**
     * Does nothing by default.
     */
    public void beforeShutdown( DirectoryService service ) throws NamingException
    {
    }


    /**
     * Does nothing by default.
     */
    public void afterShutdown( DirectoryService service ) throws NamingException
    {
    }


    /**
     * Does nothing by default.
     */
    public void beforeSync( DirectoryService service ) throws NamingException
    {
    }


    /**
     * Does nothing by default.
     */
    public void afterSync( DirectoryService service ) throws NamingException
    {
    }
}
