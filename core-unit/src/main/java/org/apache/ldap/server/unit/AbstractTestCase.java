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
package org.apache.ldap.server.unit;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.ldap.common.ldif.LdifIterator;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.server.configuration.Configuration;
import org.apache.ldap.server.configuration.MutableStartupConfiguration;
import org.apache.ldap.server.configuration.ShutdownConfiguration;

                                                                                                            
/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 280870 $
 */
public abstract class AbstractTestCase extends TestCase
{
    public static final String LDIF = "dn: uid=akarasulu,ou=users,ou=system\n" +
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
    
    private final String username;
    
    private final String password;

    /** the context root for the system partition */
    protected LdapContext sysRoot;
    
    /** flag whether to delete database files for each test or not */
    protected boolean doDelete = true;
    
    protected MutableStartupConfiguration configuration = new MutableStartupConfiguration();

    /** A testEntries of entries as Attributes to add to the DIT for testing */
    protected List testEntries = new ArrayList();

    /** An optional LDIF file path if set and present is read to add more test entries */
    private String ldifPath;

    /** Load resources relative to this class */
    private Class loadClass;

    private Hashtable overrides = new Hashtable(); 

    protected AbstractTestCase( String username, String password )
    {
        if( username == null || password == null )
        {
            throw new NullPointerException();
        }

        this.username = username;
        this.password = password;
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

        Attributes attributes = new LockableAttributesImpl();

        LdifParserImpl parser = new LdifParserImpl();

        try
        {
            parser.parse( attributes, LDIF );
        }
        catch ( NamingException e )
        {
            throw new NestableRuntimeException( e );
        }

        testEntries.add( attributes );

        // -------------------------------------------------------------------
        // Add more from an optional LDIF file if they exist
        // -------------------------------------------------------------------

        InputStream in = null;

        if ( loadClass == null && ldifPath != null )
        {
            File ldifFile = new File( ldifPath );

            if ( ldifFile.exists() )
            {
                in = new FileInputStream( ldifPath );
            }
            else
            {
                in = getClass().getResourceAsStream( ldifPath );
            }

            throw new FileNotFoundException( ldifPath );
        }
        else if ( loadClass != null && ldifPath != null )
        {
            in = loadClass.getResourceAsStream( ldifPath );
        }

        if ( in != null )
        {
            LdifIterator list = new LdifIterator( in );

            while ( list.hasNext() )
            {
                String ldif = ( String ) list.next();

                attributes = new LockableAttributesImpl();

                parser.parse( attributes, ldif );

                testEntries.add( attributes );
            }
        }

        // -------------------------------------------------------------------
        // Add key for extra entries to the testEntries of extras
        // -------------------------------------------------------------------

        configuration.setTestEntries( testEntries );
        configuration.setShutdownHookEnabled( false );
        doDelete( configuration.getWorkingDirectory() );
        setSysRoot( username, password, configuration );
    }


    /**
     * Deletes the Eve working directory.
     */
    protected void doDelete( File wkdir ) throws IOException
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
     * Sets and returns the system root.  Values of user and password used to
     * set the respective JNDI properties.  These values can be overriden by the
     * overrides properties.
     *
     * @param user the username for authenticating as this user
     * @param passwd the password of the user
     * @return the sysRoot context which is also set
     * @throws NamingException if there is a failure of any kind
     */
    protected LdapContext setSysRoot( String user, String passwd, Configuration cfg ) throws NamingException
    {
        Hashtable env = new Hashtable( cfg.toJndiEnvironment() );
        env.put( Context.SECURITY_PRINCIPAL, user );
        env.put( Context.SECURITY_CREDENTIALS, passwd );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        return setSysRoot( env );
    }


    /**
     * Sets the system root taking into account the extras and overrides
     * properties.  In between these it sets the properties for the working
     * directory, the provider URL and the JNDI InitialContexFactory to use.
     *
     * @param env an environment to use while setting up the system root.
     * @return the sysRoot context which is also set
     * @throws NamingException if there is a failure of any kind
     */
    protected LdapContext setSysRoot( Hashtable env ) throws NamingException
    {
        Hashtable envFinal = new Hashtable( env );
        if ( ! envFinal.containsKey( Context.PROVIDER_URL ) )
        {
            envFinal.put( Context.PROVIDER_URL, "ou=system" );
        }
        
        envFinal.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );
        envFinal.putAll( overrides );
        
        // We have to initiate the first run as an admin at least.
        Hashtable adminEnv = new Hashtable( envFinal );
        adminEnv.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        adminEnv.put( Context.SECURITY_CREDENTIALS, "secret" );
        adminEnv.put( Context.SECURITY_AUTHENTICATION, "simple" );
        new InitialLdapContext( adminEnv, null );

        // OK, now let's get an appropriate context.
        return sysRoot = new InitialLdapContext( envFinal, null );
    }

    /**
     * Overrides default JNDI environment properties.  Please call this method
     * to override any JNDI environment properties this test case will set.
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
     * Sets the system context root to null.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();

        Hashtable env = new Hashtable();

        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );
        env.putAll( new ShutdownConfiguration().toJndiEnvironment() );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        try { new InitialContext( env ); } catch( Exception e ) {}

        sysRoot = null;

        Runtime.getRuntime().gc();

        testEntries.clear();

        ldifPath = null;

        loadClass = null;
        
        overrides.clear();
        
        configuration = new MutableStartupConfiguration();
        
        doDelete( configuration.getWorkingDirectory() );
    }
}
