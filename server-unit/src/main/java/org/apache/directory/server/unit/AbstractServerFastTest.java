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
package org.apache.directory.server.unit;


import junit.framework.AssertionFailedError;
import org.apache.commons.io.FileUtils;
import org.apache.directory.server.configuration.ApacheDS;
import org.apache.directory.server.jndi.ServerContextFactory;
import org.apache.directory.shared.ldap.exception.LdapConfigurationException;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.mina.util.AvailablePortFinder;
import org.junit.AfterClass;
import static org.junit.Assert.assertNotNull;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.*;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 566925 $
 */
public abstract class AbstractServerFastTest
{
    private static final Logger LOG = LoggerFactory.getLogger( AbstractServerFastTest.class );
    private static final List<Entry> EMPTY_LIST = Collections.unmodifiableList( new ArrayList<Entry>( 0 ) );
    private static final String CTX_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    /** the context root */
    protected static LdapContext root;

    /** the context root for the system partition */
    protected static LdapContext sysRoot;

    /** the context root for the rootDSE */
    protected static LdapContext rootDSE;

    /** the context root for the schema */
    protected static LdapContext schemaRoot;

    /** the context root for the tests */
    protected static LdapContext ctx;

    /** flag whether to delete database files for each test or not */
    protected static boolean doDelete = true;

    protected static int port = -1;
    protected static final String LDIF = null;
    protected static final String HOST = "localhost";
    protected static final String USER = "uid=admin,ou=system";
    protected static final String PASSWORD = "secret";
    protected static final String BASE = "dc=example,dc=com";
    protected static ApacheDS apacheDS = new ApacheDS();


    /**
     * If there is an LDIF file with the same name as the test class 
     * but with the .LDIF extension then it is read and the entries
     * it contains are added to the server.  It appears as though the
     * administor adds these entries to the server.
     *
     * @param verifyEntries whether or not all entry additions are checked
     * to see if they were in fact correctly added to the server
     * @return a list of entries added to the server in the order they were added
     * @throws NamingException of the load of test LDIF fails
     */
    protected List<Entry> loadTestLdif( boolean verifyEntries ) throws NamingException
    {
        InputStream in = getClass().getResourceAsStream( getClass().getSimpleName() + ".LDIF" );
        
        if ( in == null )
        {
            return EMPTY_LIST;
        }
        
        LdifReader ldifReader = new LdifReader( in );
        List<Entry> entries = new ArrayList<Entry>();
        
        while ( ldifReader.hasNext() )
        {
            Entry entry = ldifReader.next();
            rootDSE.createSubcontext( entry.getDn(), entry.getAttributes() );
            
            if ( verifyEntries )
            {
                verify( entry );
                LOG.info( "Successfully verified addition of entry {}", entry.getDn() );
            }
            else
            {
                LOG.info( "Added entry {} without verification", entry.getDn() );
            }
            
            entries.add( entry );
        }
        
        return entries;
    }
    

    /**
     * Verifies that an entry exists in the directory with the 
     * specified attributes.
     *
     * @param entry the entry to verify
     * @throws NamingException if there are problems accessing the entry
     */
    protected void verify( Entry entry ) throws NamingException
    {
        Attributes readAttributes = rootDSE.getAttributes( entry.getDn() );
        NamingEnumeration<String> readIds = entry.getAttributes().getIDs();
        while ( readIds.hasMore() )
        {
            String id = readIds.next();
            Attribute readAttribute = readAttributes.get( id );
            Attribute origAttribute = entry.getAttributes().get( id );
            
            for ( int ii = 0; ii < origAttribute.size(); ii++ )
            {
                if ( ! readAttribute.contains( origAttribute.get( ii ) ) )
                {
                    LOG.error( "Failed to verify entry addition of {}. {} attribute in original " +
                    		"entry missing from read entry.", entry.getDn(), id );
                    throw new AssertionFailedError( "Failed to verify entry addition of " + entry.getDn()  );
                }
            }
        }
    }
    

    /**
     * Common code to get an initial context via a simple bind to the 
     * server over the wire using the SUN JNDI LDAP provider. Do not use 
     * this method until after the setUp() method is called to start the
     * server otherwise it will fail. 
     *
     * @return an LDAP context as the the administrator to the rootDSE
     * @throws NamingException if the server cannot be contacted
     */
    protected LdapContext getWiredContext() throws NamingException
    {
        return getWiredContext( "uid=admin,ou=system", "secret" );
    }
    
    
    /**
     * Common code to get an initial context via a simple bind to the 
     * server over the wire using the SUN JNDI LDAP provider. Do not use 
     * this method until after the setUp() method is called to start the
     * server otherwise it will fail.
     *
     * @param bindPrincipalDn the DN of the principal to bind as
     * @param password the password of the bind principal
     * @return an LDAP context as the the administrator to the rootDSE
     * @throws NamingException if the server cannot be contacted
     */
    protected LdapContext getWiredContext( String bindPrincipalDn, String password ) throws NamingException
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, CTX_FACTORY );
        env.put( Context.PROVIDER_URL, "ldap://localhost:" + port );
        env.put( Context.SECURITY_PRINCIPAL, bindPrincipalDn );
        env.put( Context.SECURITY_CREDENTIALS, password );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        return new InitialLdapContext( env, null );
    }
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        doDelete( apacheDS.getDirectoryService().getWorkingDirectory() );
        port = AvailablePortFinder.getNextAvailable( 1024 );
        apacheDS.getLdapServer().setIpPort( port );
        apacheDS.getDirectoryService().setShutdownHookEnabled( false );
        apacheDS.startup();
        
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put( ApacheDS.JNDI_KEY, apacheDS );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, ServerContextFactory.class.getName() );

        setContexts( env );
    }


    /**
     * Deletes the Eve working directory.
     * @param wkdir the working directory to delete
     * @throws IOException if the directory cannot be deleted
     */
    protected static void doDelete( File wkdir ) throws IOException
    {
        if ( doDelete )
        {
            if ( wkdir.exists() )
            {
                FileUtils.deleteDirectory( wkdir );
            }
            if ( wkdir.exists() )
            {
                throw new IOException( "Failed to delete: " + wkdir );
            }
        }
    }


    /**
     * Sets the contexts of this class taking into account the extras and overrides
     * properties.  
     *
     * @param env an environment to use while setting up the system root.
     * @throws NamingException if there is a failure of any kind
     */
    protected static void setContexts( Hashtable<String, Object> env ) throws NamingException
    {
        Hashtable<String, Object> envFinal = new Hashtable<String, Object>( env );
        
        envFinal.put( Context.PROVIDER_URL, "ou=system" );
        root = new InitialLdapContext( envFinal, null );

        envFinal.put( Context.PROVIDER_URL, "" );
        new InitialLdapContext( envFinal, null );

        envFinal.put( Context.PROVIDER_URL, "ou=schema" );
        new InitialLdapContext( envFinal, null );
        
        
        Hashtable<String, String> envTest = new Hashtable<String, String>();
        envTest.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        envTest.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        envTest.put( "java.naming.security.principal", "uid=admin,ou=system" );
        envTest.put( "java.naming.security.credentials", "secret" );
        envTest.put( "java.naming.security.authentication", "simple" );
        sysRoot = new InitialLdapContext( envTest, null );

        envTest = new Hashtable<String, String>();
        envTest.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        envTest.put( "java.naming.provider.url", "ldap://localhost:" + port + "/cn=schema" );
        envTest.put( "java.naming.security.principal", "uid=admin,ou=system" );
        envTest.put( "java.naming.security.credentials", "secret" );
        envTest.put( "java.naming.security.authentication", "simple" );
        schemaRoot = new InitialLdapContext( envTest, null );
        
        envTest = new Hashtable<String, String>();
        envTest.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        envTest.put( "java.naming.provider.url", "ldap://localhost:" + port + "/ou=system" );
        envTest.put( "java.naming.security.principal", "uid=admin,ou=system" );
        envTest.put( "java.naming.security.credentials", "secret" );
        envTest.put( "java.naming.security.authentication", "simple" );

        ctx = new InitialLdapContext( envTest, null );
        assertNotNull( ctx );
        
        envTest = new Hashtable<String, String>();
        envTest.put( "java.naming.factory.initial", "com.sun.jndi.ldap.LdapCtxFactory" );
        envTest.put( "java.naming.provider.url", "ldap://localhost:" + port );
        envTest.put( "java.naming.security.principal", "uid=admin,ou=system" );
        envTest.put( "java.naming.security.credentials", "secret" );
        envTest.put( "java.naming.security.authentication", "simple" );

        rootDSE = new InitialLdapContext( envTest, null );
        assertNotNull( rootDSE );
    }


    /**
     * Sets the system context root to null.
     *
     * @see junit.framework.TestCase#tearDown()
     * @throws Exception if there are problems shutting down the server
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
        try
        {
            apacheDS.shutdown();
        }
        catch ( Exception e )
        {
            LOG.error( "Encountered exception while trying to shutdown.", e );
        }

        root = null;
        doDelete( apacheDS.getDirectoryService().getWorkingDirectory() );
        apacheDS = new ApacheDS();
    }


    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into
     * the newly created system partition to prime it up for operation.  Note
     * that only ou=system entries will be added - entries for other partitions
     * cannot be imported and will blow chunks.
     *
     * @throws NamingException if there are problems reading the LDIF file and
     * adding those entries to the system partition
     * @param in the input stream to read the LDIF file from
     */
    protected void importLdif( InputStream in ) throws NamingException
    {
        try
        {
            Iterator<Entry> iterator = new LdifReader( in );

            while ( iterator.hasNext() )
            {
                Entry entry = iterator.next();
                LdapDN dn = new LdapDN( entry.getDn() );
                rootDSE.createSubcontext( dn, entry.getAttributes() );
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system LDIF file";
            NamingException ne = new LdapConfigurationException( msg );
            ne.setRootCause( e );
            throw ne;
        }
    }
    
    protected static void importLdif( LdapContext ctx, String ldif ) throws NamingException
    {
        try
        {
            LdifReader reader = new LdifReader();
            List<Entry> entries = reader.parseLdif( ldif );

            for ( Entry entry:entries )
            {
                try
                {
                    LdapDN dn = new LdapDN( entry.getDn() );
                    ctx.createSubcontext( dn, entry.getAttributes() );
                }
                catch ( NameAlreadyBoundException nabe )
                {
                    // Do nothing if the entry has already been injected
                }
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system LDIF file";
            NamingException ne = new LdapConfigurationException( msg );
            ne.setRootCause( e );
            throw ne;
        }
    }

    
    /**
     * Inject an LDIF String into the server. DN must be relative to the
     * root.
     * @param ldif the LDIF to add
     * @throws NamingException if there are problems adding the entries in the LDIF
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
