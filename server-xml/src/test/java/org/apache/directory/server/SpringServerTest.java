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
package org.apache.directory.server;

import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

import javax.naming.Context;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.net.URL;
import java.util.Hashtable;


/**
 * @version $Rev$ $Date$
 */
public class SpringServerTest
{
    private String providerURL = "dc=example,dc=com";

    @Test
    public void testSpringServerStartup() throws Exception {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL configURL = classLoader.getResource( "server.xml" );

        File configF = new File( configURL.toURI() );
        ApplicationContext factory = new FileSystemXmlApplicationContext( configF.toURL().toString() );
        ApacheDS apacheDS = ( ApacheDS ) factory.getBean( "apacheDS" );
        //noinspection unchecked
        Hashtable<String,Object> env = ( Hashtable ) factory.getBean( "environment" );
        env.put( ApacheDS.JNDI_KEY, apacheDS );
        env.put( Context.PROVIDER_URL, providerURL );
        env.put( Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.getName() );

        File workingDirFile = new File( configF.getParentFile(), "work" );
        apacheDS.getDirectoryService().setWorkingDirectory( workingDirFile );
        new InitialDirContext( env );
    }
}
