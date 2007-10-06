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


import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;


/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractTestCase extends TestCase
{
    public static final Logger LOG = LoggerFactory.getLogger( AbstractTestCase.class );

    public static final String LDIF = 
    	"dn: uid=akarasulu,ou=users,ou=system\n" + 
    	"cn: Alex Karasulu\n" +
        "sn: Karasulu\n" + 
        "givenname: Alex\n" + 
        "objectclass: top\n" + 
        "objectclass: person\n" +
        "objectclass: organizationalPerson\n" + 
        "objectclass: inetOrgPerson\n" + 
        "ou: Engineering\n" + 
        "ou: People\n" +
        "l: Bogusville\n" + 
        "uid: akarasulu\n" + 
        "mail: akarasulu@apache.org\n" +
        "telephonenumber: +1 408 555 4798\n" + 
        "facsimiletelephonenumber: +1 408 555 9751\n" + 
        "roomnumber: 4612\n" +
        "userpassword: test\n";

    protected final String username;

    protected final String password;

    /** the context root for the system partition */
    protected LdapContext sysRoot;

    /** the context root for the schema partition */
    protected LdapContext schemaRoot;
    
    /** the rootDSE context for the server */
    protected LdapContext rootDSE;

    /** flag whether to delete database files for each test or not */
    protected boolean doDelete = true;

    /** A testEntries of entries as Attributes to add to the DIT for testing */
    protected List<Entry> testEntries = new ArrayList<Entry>();

    /** An optional LDIF file path if set and present is read to add more test entries */
    private String ldifPath;

    /** Load resources relative to this class */
    private Class loadClass;

    private Hashtable<String,Object> overrides = new Hashtable<String,Object>();

    protected Registries registries;

    protected DirectoryService service;


    protected AbstractTestCase( String username, String password )
    {
        if ( username == null || password == null )
        {
            throw new NullPointerException();
        }

        this.username = username;
        this.password = password;
        this.service = new DefaultDirectoryService();
    }


    /**
     * Sets the LDIF path as a relative resource path to use with the
     * loadClass parameter to load the resource.
     *
     * @param ldifPath the relative resource path to the LDIF file
     * @param loadClass the class used to load the LDIF as a resource stream
     */
    protected void setLdifPath( String ldifPath, Class loadClass )
    {
        this.loadClass = loadClass;
        this.ldifPath = ldifPath;
    }


    /**
     * Sets the LDIF path to use.  If the path is relative to this class then it
     * is first tested
     *
     * @param ldifPath the path to the LDIF file
     */
    protected void setLdifPath( String ldifPath )
    {
        this.ldifPath = ldifPath;
    }


    /**
     * Get's the initial context factory for the provider's ou=system context
     * root.
     *
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // -------------------------------------------------------------------
        // Add a single test entry
        // -------------------------------------------------------------------

        LdifReader reader = new LdifReader();
    	List entries = reader.parseLdif( LDIF );
        Entry entry = ( Entry ) entries.get(0);
        testEntries.add( entry );

        // -------------------------------------------------------------------
        // Add more from an optional LDIF file if they exist
        // -------------------------------------------------------------------

        InputStream in = null;
        if ( loadClass != null && ldifPath == null )
        {
            in = loadClass.getResourceAsStream( getName() + ".ldif" );
        }
        else if ( loadClass == null && ldifPath != null )
        {
            File ldifFile = new File( ldifPath );
            if ( ldifFile.exists() )
            {
                //noinspection UnusedAssignment
                in = new FileInputStream( ldifPath );
            }
            else
            {
                //noinspection UnusedAssignment
                in = getClass().getResourceAsStream( ldifPath );
            }
            throw new FileNotFoundException( ldifPath );
        }
        else if ( loadClass != null )
        {
            in = loadClass.getResourceAsStream( ldifPath );
        }

        if ( in != null )
        {
            Iterator<Entry> list = new LdifReader( in );
            
            while ( list.hasNext() )
            {
                entry = list.next();
                testEntries.add( entry );
            }
        }

        // -------------------------------------------------------------------
        // Add key for extra entries to the testEntries of extras
        // -------------------------------------------------------------------

        service.setTestEntries( testEntries );
        service.setShutdownHookEnabled( false );
        doDelete( service.getWorkingDirectory() );
        service.startup();
        setContextRoots( username, password );
        registries = service.getRegistries();
    }

    
    /**
     * Restarts the server without loading data when it has been shutdown.
     * @throws NamingException if the restart fails
     */
    protected void restart() throws NamingException
    {
        if ( service == null )
        {
            service = new DefaultDirectoryService();
        }
        service.setShutdownHookEnabled( false );
        service.startup();
        setContextRoots( username, password );
    }
    

    /**
     * Deletes the working directory.
     *
     * @param wkdir the working directory to delete
     * @throws IOException if the working directory cannot be deleted
     */
    protected void doDelete( File wkdir ) throws IOException
    {
        if ( doDelete )
        {
            if ( wkdir.exists() )
            {
                try
                {
                    FileUtils.deleteDirectory( wkdir );
                }
                catch ( IOException e )
                {
                    LOG.error( "Failed to delete the working directory.", e );
                }
            }
            if ( wkdir.exists() )
            {
                throw new IOException( "Failed to delete: " + wkdir );
            }
        }
    }


    /**
     * Sets and returns the system root.  Values of user and password used to
     * set the respective JNDI properties.  These values can be overriden by the
     * overrides properties.
     *
     * @param user the username for authenticating as this user
     * @param passwd the password of the user
     * @throws NamingException if there is a failure of any kind
     */
    protected void setContextRoots( String user, String passwd ) throws NamingException
    {
        Hashtable<String,Object> env = new Hashtable<String,Object>();
        env.put(  DirectoryService.JNDI_KEY, service );
        env.put( Context.SECURITY_PRINCIPAL, user );
        env.put( Context.SECURITY_CREDENTIALS, passwd );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        setContextRoots( env );
    }


    /**
     * Sets the system root taking into account the extras and overrides
     * properties.  In between these it sets the properties for the working
     * directory, the provider URL and the JNDI InitialContexFactory to use.
     *
     * @param env an environment to use while setting up the system root.
     * @throws NamingException if there is a failure of any kind
     */
    protected void setContextRoots( Hashtable<String,Object> env ) throws NamingException
    {
        Hashtable<String,Object> envFinal = new Hashtable<String,Object>( env );
        if ( !envFinal.containsKey( Context.PROVIDER_URL ) )
        {
            envFinal.put( Context.PROVIDER_URL, "ou=system" );
        }

        envFinal.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        envFinal.putAll( overrides );

        // We have to initiate the first run as an admin at least.
        Hashtable<String,Object> adminEnv = new Hashtable<String,Object>( envFinal );
        adminEnv.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        adminEnv.put( Context.SECURITY_CREDENTIALS, "secret" );
        adminEnv.put( Context.SECURITY_AUTHENTICATION, "simple" );
        adminEnv.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        adminEnv.put( DirectoryService.JNDI_KEY, service );
        new InitialLdapContext( adminEnv, null );

        // OK, now let's get an appropriate context.
        sysRoot = new InitialLdapContext( envFinal, null );

        envFinal.put( Context.PROVIDER_URL, "ou=schema" );
        schemaRoot = new InitialLdapContext( envFinal, null );
        
        envFinal.put( Context.PROVIDER_URL, "" );
        rootDSE = new InitialLdapContext( envFinal, null );
    }


    /**
     * Overrides default JNDI environment properties.  Please call this method
     * to override any JNDI environment properties this test case will set.
     * @param key the key of the hashtable entry to add to the environment
     * @param value the value of the hashtable entry to add to the environment
     */
    protected void overrideEnvironment( String key, Object value )
    {
        overrides.put( key, value );
    }


    protected Hashtable getOverriddenEnvironment()
    {
        return ( Hashtable ) overrides.clone();
    }


    /**
     * Issues a shutdown request to the server.
     */
    protected void shutdown()
    {
        try
        {
            service.shutdown();
        }
        catch ( Exception e )
        {
            LOG.error( "Encountered an error while shutting down directory service.", e );
        } 
        sysRoot = null;
        Runtime.getRuntime().gc();
    }


    /**
     * Issues a sync request to the server.
     */
    protected void sync()
    {
        try
        {
            service.sync();
        }
        catch ( Exception e )
        {
            LOG.warn( "Encountered error while syncing.", e );
        } 
    }

    
    /**
     * Sets the system context root to null.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        shutdown();
        testEntries.clear();
        ldifPath = null;
        loadClass = null;
        overrides.clear();
        service = new DefaultDirectoryService();
        doDelete( service.getWorkingDirectory() );
    }


    protected void setLoadClass( Class loadClass )
    {
        this.loadClass = loadClass;
    }
    
    
    /**
     * Inject an ldif String into the server. DN must be relative to the
     * root.
     * 
     * @param ldif the ldif containing entries to add to the server.
     * @throws NamingException if there is a problem adding the entries from the LDIF
     */
    protected void injectEntries( String ldif ) throws NamingException
    {
        LdifReader reader = new LdifReader();
        List<Entry> entries = reader.parseLdif( ldif );

        for ( Entry entry : entries )
        {
            rootDSE.createSubcontext( new LdapDN( entry.getDn() ), entry.getAttributes() );
        }
    }
}
