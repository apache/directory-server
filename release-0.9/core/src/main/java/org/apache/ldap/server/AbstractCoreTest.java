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
package org.apache.ldap.server;


import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.ldap.common.exception.LdapConfigurationException;
import org.apache.ldap.common.ldif.LdifIterator;
import org.apache.ldap.common.ldif.LdifParser;
import org.apache.ldap.common.ldif.LdifParserImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.jndi.EnvKeys;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.ArrayList;

                                                                                                            
/**
 * A simple testcase for testing JNDI provider functionality.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractCoreTest extends TestCase
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

    /** the context root for the system partition */
    protected LdapContext sysRoot;

    /** flag whether to delete database files for each test or not */
    protected boolean doDelete = true;

    /** extra environment parameters that can be added before setUp */
    protected Hashtable extras = new Hashtable();

    /** extra environment parameters that can be added before setUp to override values */
    protected Hashtable overrides = new Hashtable();


    private ArrayList list = null;


    public AbstractCoreTest()
    {
        list = new ArrayList();

        Attributes attributes = new LockableAttributesImpl();

        LdifParserImpl parser = new LdifParserImpl();

        try
        {
            parser.parse( attributes, LDIF );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();

            throw new NestableRuntimeException( e );
        }

        list.add( attributes );
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

        extras.put( EnvKeys.TEST_ENTRIES, list );
        
        if ( overrides.containsKey( EnvKeys.WKDIR ) )
        {
            doDelete( new File( ( String ) overrides.get( EnvKeys.WKDIR ) ) );
        }
        else
        {
            doDelete( new File( "target" + File.separator + "apacheds" ) );
        }

        setSysRoot( "uid=admin,ou=system", "secret" );
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
    protected LdapContext setSysRoot( String user, String passwd ) throws NamingException
    {
        Hashtable env = new Hashtable();

        env.put( Context.SECURITY_PRINCIPAL, user );

        env.put( Context.SECURITY_CREDENTIALS, passwd );

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
        Hashtable envFinal = new Hashtable();

        envFinal.putAll( extras );

        envFinal.putAll( env );

        envFinal.put( Context.PROVIDER_URL, "ou=system" );

        envFinal.put( EnvKeys.WKDIR, "target" + File.separator + "apacheds" );

        envFinal.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.ldap.server.jndi.CoreContextFactory" );

        envFinal.putAll( overrides );

        return sysRoot = new InitialLdapContext( envFinal, null );
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

        env.put( EnvKeys.SHUTDOWN, "" );

        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );

        env.put( Context.SECURITY_CREDENTIALS, "secret" );

        try { new InitialContext( env ); } catch( Exception e ) {}

        sysRoot = null;

        Runtime.getRuntime().gc();
    }


    /**
     * Imports the LDIF entries packaged with the Eve JNDI provider jar into
     * the newly created system partition to prime it up for operation.  Note
     * that only ou=system entries will be added - entries for other partitions
     * cannot be imported and will blow chunks.
     *
     * @throws NamingException if there are problems reading the ldif file and
     * adding those entries to the system partition
     */
    protected void importLdif( InputStream in ) throws NamingException
    {
        Hashtable env = new Hashtable();

        env.putAll( sysRoot.getEnvironment() );

        LdapContext ctx = new InitialLdapContext( env, null );

        LdifParser parser = new LdifParserImpl();

        try
        {
            LdifIterator iterator = new LdifIterator( in );

            while ( iterator.hasNext() )
            {
                Attributes attributes = new LockableAttributesImpl();

                String ldif = ( String ) iterator.next();

                parser.parse( attributes, ldif );

                Name dn = new LdapName( ( String ) attributes.remove( "dn" ).get() );

                dn.remove( 0 );

                ctx.createSubcontext( dn, attributes );
            }
        }
        catch ( Exception e )
        {
            String msg = "failed while trying to parse system ldif file";

            NamingException ne = new LdapConfigurationException( msg );

            ne.setRootCause( e );

            throw ne;
        }
    }
}
