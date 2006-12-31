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
package org.apache.directory.server.core.unit;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import javax.naming.Context;
import javax.naming.directory.BasicAttributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.NamespaceTools;


/**
 * A base testcase is used to create test harnesses for running performance 
 * metrics on ApacheDS and other servers.  It provides a framework for running 
 * tests and capturing the results of individual operations against the 
 * directory.
 * <br>
 * This test case does a few things out of the box to make life easier for those
 * that want to run performance tests against an LDAP server.  These are listed
 * below:
 * <ul>
 *   <li>
 *      Uses the presence of a system property, 'external', to bypass the
 *      creation of an embedded ApacheDS instance.  If the external property
 *      is defined, a properties file by the name of the test case with the 
 *      .properties extension is searched for on the classpath.  If found this
 *      properties file is loaded and those parameters are used to connect to
 *      the LDAP server by feeding those properties into the InitialContext's
 *      environment argument.  If the properties file is not found, smart 
 *      defaults are used instead to connect to some external LDAP server.
 *      
 *      Uses an external.prepare.command system property to execute a command 
 *      between test cases.  Execution occurs before any test is run.  This 
 *      command should stop a server if it is running, clean out the database
 *      contents, and restart the server readying it for another run.
 *   </li>
 *
 *   <li>
 *      Automatically searches for an LDIF file with the same name as the current
 *      test case.  This file is loaded for each test case after loading a 
 *      common.ldif file if it is present on the classpath.  The test case can be 
 *      told to disable this LDIF file loading for both the common.ldif and the 
 *      <className>.ldif file.
 *   </li>
 *
 *   <li>
 *      The performance test reports statistics to the console.  Each operation 
 *      being perform is logged with timing information.  The time for each LDIF
 *      operation is also tracked.  The statistics are also compiled into a 
 *      output formated file.
 *   </li>
 *
 *   <li>
 *      After LDIF loads, test cases run.  These test cases can issue various 
 *      additional operations which get logged.
 *   </li>
 *
 * </ul>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbstractPerformanceTest extends AbstractTestCase
{
    private final Class subclass;
    private boolean isExternal = false;
    private String prepareCommand = null;
    private File outputDirectory = null;
    private PrintWriter statsOut = null;
    private long startTime = 0;
    private LdapContext testRoot;
    
    
    /**
     * Initializes the statistics log PrintWriter.
     * 
     * @param subclass 
     */
    protected AbstractPerformanceTest( Class subclass ) throws IOException
    {
        super( PartitionNexus.ADMIN_PRINCIPAL, "secret" );
        this.subclass = subclass;
        
        // Setup the statistics output writer
        outputDirectory = new File( System.getProperty( "outputDirectory", "." ) );
        File statsOutFile = new File( outputDirectory, subclass.getName() + ".stats" );
        statsOut = new PrintWriter( new FileWriter( statsOutFile ) );
        
        // Setup variables for handling external LDAP servers
        isExternal = System.getProperties().containsKey( "external" );
        if ( isExternal )
        {
            prepareCommand = System.getProperty( "external.prepare.command", null );
        }
    }
    
    
    protected void setUp() throws Exception
    {
        if ( ! isExternal )
        {
            // Add indices for ou, uid, and objectClass
            HashSet indexedAttributes = new HashSet();
            indexedAttributes.add( "ou" );
            indexedAttributes.add( "uid" );
            indexedAttributes.add( "objectClass" );
            
            // Build the root entry for the new partition
            BasicAttributes attributes = new BasicAttributes( "objectClass", "top", true );
            attributes.get( "objectClass" ).add( "organizationalUnit" );
            attributes.put( "ou", "test" );
            
            // Add apache.org paritition since all work will be done here
            MutablePartitionConfiguration partConfig = new MutablePartitionConfiguration();
            partConfig.setIndexedAttributes( indexedAttributes );
            partConfig.setName( "test" );
            partConfig.setSuffix( "ou=test" );
            partConfig.setContextEntry( attributes );
            
            configuration.setShutdownHookEnabled( false );
            configuration.setPartitionConfigurations( Collections.singleton( partConfig ) );
            
            doDelete( configuration.getWorkingDirectory() );
            setContextRoots( username, password, configuration );
            
            Hashtable env = new Hashtable( configuration.toJndiEnvironment() );
            env.put( Context.SECURITY_PRINCIPAL, username );
            env.put( Context.SECURITY_CREDENTIALS, password );
            env.put( Context.SECURITY_AUTHENTICATION, "simple" );
            env.put( Context.PROVIDER_URL, "ou=test" );
            env.put( Context.INITIAL_CONTEXT_FACTORY, 
                "org.apache.directory.server.core.jndi.CoreContextFactory" );
            testRoot = new InitialLdapContext( env, null );
        }
        else
        {
            // execute the external prepare command
            execute( prepareCommand );
            
            // load the JNDI properties if it exists otherwise use smart defaults
        }
        
        startTime = System.currentTimeMillis();
        statsOut.println( "=========================================================================" );
        statsOut.println( "[START]: " + subclass.getName() );
        statsOut.println( "=========================================================================" );
        statsOut.flush();
        loadLdifs();
    }
    
    
    protected void tearDown() throws Exception
    {
        statsOut.println( "=========================================================================" );
        statsOut.println( "[FINISH]: " + subclass.getName() );
        statsOut.println( "=========================================================================" );
        statsOut.flush();
        super.tearDown();
    }
    
    
    /**
     * Loads the commons.ldif file if present, then it loads the test 
     * specific LDIF file also if present.  
     */
    private void loadLdifs() throws Exception
    {
        InputStream in = subclass.getResourceAsStream( "/common.ldif" );
        if ( in != null )
        {
            in.close();
            loadLdif( "/common.ldif" );
        }

        String testLdif = subclass.getName() + ".ldif";
        in = subclass.getResourceAsStream( testLdif );
        if ( in != null )
        {
            in.close();
            loadLdif( testLdif );
        }
    }
    
    
    /**
     * Only supports add operations at this point.
     * 
     * @param entry the LDIF entry being applied
     */
    protected boolean applyEntry( Entry entry ) throws Exception
    {
        switch ( entry.getChangeType() )
        {
            case( Entry.ADD ):
                LdapDN ancestor = new LdapDN( testRoot.getNameInNamespace() );
                LdapDN descendant = new LdapDN( entry.getDn() );
                LdapDN relative = ( LdapDN ) NamespaceTools.getRelativeName( ancestor, descendant );
                testRoot.createSubcontext( relative, entry.getAttributes() );
                return true;
            default:
                return false;
        }
    }
    
    
    private void loadLdif( String ldifResource ) throws Exception
    {
        int count = 0;
        long start = 0;
        
        InputStream in = subclass.getResourceAsStream( ldifResource );
        if ( in != null )
        {
            LdifReader reader = new LdifReader( in );
            start = System.currentTimeMillis();
            log( "Started LDIF Import: " + ldifResource  );
            for ( Iterator ii = reader.iterator(); ii.hasNext(); /**/ )
            {
                long startEntry = System.currentTimeMillis();
                if ( applyEntry( ( Entry ) ii.next() ) )
                {
                    count++;
                }
                log( "added " + count + "-th entry in " + ( System.currentTimeMillis() - startEntry ) );
                statsOut.flush();
            }
        }
        
        StringBuffer buf = new StringBuffer();
        buf.append( "Completed LDIF Import: " );
        buf.append( count ).append( " entries in " );
        buf.append( System.currentTimeMillis() - start );
        buf.append( " milliseconds" );
        log( buf.toString() );
        statsOut.flush();
    }
    
    
    private void execute( String prepareCommand )
    {

    }

    
    private void log( String msg )
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "[" ).append( System.currentTimeMillis() - startTime ).append( "] - " );
        buf.append( msg );
        statsOut.println( buf );
    }
}


