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
package org.apache.directory.server.core.integ;


import org.apache.directory.server.annotations.LdapServerBuilder;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.DSBuilder;
import org.apache.directory.server.ldap.LdapServer;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;


/**
 * A class to read and store the Suite's annotations. It's called when we start
 * running a Suite, and will call all the classes contained in the suite.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class FrameworkSuite extends Suite
{
    /** The DSBuilder for this suite, if any */
    private DSBuilder suiteDSBuilder;
    
    /** The LdapServerBuilder for this class, if any */
    private LdapServerBuilder suiteLdapServerBuilder;

    /** The DirectoryService for this suite, if any */
    private DirectoryService suiteService;
    
    /** The LdapServer for this class, if any */
    private LdapServer suiteLdapServer;

    
    /** The LDIFs entries for this suite */
    private ApplyLdifs suiteLdifs;

    /**
     * Creates a new instance of FrameworkSuite.
     */
    public FrameworkSuite( Class<?> clazz, RunnerBuilder builder ) throws InitializationError
    {
        super( clazz, builder );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void runChild( Runner runner, RunNotifier notifier )
    {
        suiteDSBuilder = getDescription().getAnnotation( DSBuilder.class );
        suiteLdifs = getDescription().getAnnotation( ApplyLdifs.class );
        suiteLdapServerBuilder = getDescription().getAnnotation( LdapServerBuilder.class );

        // Store the suite into the class we will run
        ( ( FrameworkRunner ) runner ).setSuite( this );
        
        // Now, call the class containing the tests
        super.runChild( runner, notifier );
    }


    /**
     * @return the suiteDSBuilder
     */
    public DSBuilder getSuiteDSBuilder()
    {
        return suiteDSBuilder;
    }


    /**
     * @return the suiteService
     */
    public DirectoryService getSuiteService()
    {
        return suiteService;
    }
    
    
    /**
     * @param suiteService the suiteService to set
     */
    public void setSuiteService( DirectoryService suiteService )
    {
        this.suiteService = suiteService;
    }
    
    
    /**
     * @return the suiteLdifs
     */
    public ApplyLdifs getSuiteLdifs()
    {
        return suiteLdifs;
    }


    /**
     * @return the suiteLdapServerBuilder
     */
    public LdapServerBuilder getSuiteLdapServerBuilder()
    {
        return suiteLdapServerBuilder;
    }


    /**
     * @return the suiteLdapServer
     */
    public LdapServer getSuiteLdapServer()
    {
        return suiteLdapServer;
    }
}
