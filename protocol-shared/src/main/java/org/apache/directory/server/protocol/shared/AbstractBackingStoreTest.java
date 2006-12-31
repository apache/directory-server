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

package org.apache.directory.server.protocol.shared;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;

import junit.framework.TestCase;

import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.MutablePartitionConfiguration;
import org.apache.directory.server.core.configuration.MutableStartupConfiguration;
import org.apache.directory.server.core.configuration.ShutdownConfiguration;
import org.apache.directory.server.core.jndi.CoreContextFactory;
import org.apache.directory.server.protocol.shared.store.KerberosAttribute;
import org.apache.directory.server.schema.bootstrap.ApacheSchema;
import org.apache.directory.server.schema.bootstrap.ApachednsSchema;
import org.apache.directory.server.schema.bootstrap.CoreSchema;
import org.apache.directory.server.schema.bootstrap.CosineSchema;
import org.apache.directory.server.schema.bootstrap.InetorgpersonSchema;
import org.apache.directory.server.schema.bootstrap.Krb5kdcSchema;
import org.apache.directory.server.schema.bootstrap.SystemSchema;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Base class for testing protocol providers against the JNDI provider.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractBackingStoreTest extends TestCase
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( AbstractBackingStoreTest.class );

    /** a flag stating whether to delete the backing store for each test or not */
    protected boolean doDelete = true;

    /** the backing store configuration */
    protected MutableStartupConfiguration config;

    protected CoreContextFactory factory;
    protected Hashtable env;


    protected void setUp() throws Exception
    {
        env = new Hashtable( setUpPartition() );

        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( Context.PROVIDER_URL, "ou=system" );

        factory = new CoreContextFactory();
    }


    protected void loadPartition( String partition, String ldifFile ) throws NamingException
    {
        env.put( Context.PROVIDER_URL, partition );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );
        load( ctx, ldifFile );
    }


    /**
     * Test that the system partition was set up properly.
     *
     * @throws NamingException if there are problems
     */
    protected void doTestSystemPartition() throws Exception
    {
        env.put( Context.PROVIDER_URL, "ou=system" );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );
        Attributes attributes = ctx.getAttributes( "cn=A,dc=www,dc=apache,dc=org,ou=Zones" );
        assertEquals( 3, attributes.size() );
    }


    protected void doTestApacheZone() throws Exception
    {
        env.put( Context.PROVIDER_URL, "dc=apache,dc=org" );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );
        Attributes attributes = ctx.getAttributes( "cn=A,dc=www,dc=apache,dc=org,ou=Zones" );
        assertEquals( 3, attributes.size() );
    }


    protected void doTestExampleZone() throws Exception
    {
        env.put( Context.PROVIDER_URL, "dc=example,dc=com" );
        DirContext ctx = ( DirContext ) factory.getInitialContext( env );
        Attributes attributes = ctx.getAttributes( "cn=A,dc=www,dc=example,dc=com,ou=Zones" );
        assertEquals( 3, attributes.size() );
    }


    protected Hashtable setUpPartition() throws NamingException
    {
        config = new MutableStartupConfiguration();

        Set schemas = new HashSet();
        schemas.add( new CoreSchema() );
        schemas.add( new CosineSchema() );
        schemas.add( new ApacheSchema() );
        schemas.add( new InetorgpersonSchema() );
        schemas.add( new Krb5kdcSchema() );
        schemas.add( new SystemSchema() );
        schemas.add( new ApachednsSchema() );

        if ( true )
        {
            throw new RuntimeException( "Need to find a way to configure the use of different schemas on startup." );
        }
        
        //config.setBootstrapSchemas( schemas );

        Set partitions = new HashSet();
        partitions.add( getExamplePartition() );
        partitions.add( getApachePartition() );

        config.setPartitionConfigurations( Collections.unmodifiableSet( partitions ) );

        return config.toJndiEnvironment();
    }


    private PartitionConfiguration getExamplePartition() throws NamingException
    {
        MutablePartitionConfiguration partConfig = new MutablePartitionConfiguration();
        partConfig.setName( "example" );

        HashSet indices = new HashSet();
        indices.add( "dc" );
        indices.add( "ou" );
        indices.add( "objectClass" );
        indices.add( "krb5PrincipalName" );
        indices.add( "uid" );
        partConfig.setIndexedAttributes( indices );

        partConfig.setSuffix( "dc=example, dc=com" );

        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "domain" );
        attrs.put( objectClass );
        attrs.put( "dc", "example" );
        partConfig.setContextEntry( attrs );

        return partConfig;
    }


    private PartitionConfiguration getApachePartition() throws NamingException
    {
        MutablePartitionConfiguration partConfig = new MutablePartitionConfiguration();
        partConfig.setName( "apache" );

        HashSet indices = new HashSet();
        indices.add( "dc" );
        indices.add( "ou" );
        indices.add( "objectClass" );
        indices.add( "krb5PrincipalName" );
        indices.add( "uid" );
        partConfig.setIndexedAttributes( indices );

        partConfig.setSuffix( "dc=apache, dc=org" );

        LockableAttributesImpl attrs = new LockableAttributesImpl();
        LockableAttributeImpl objectClass = new LockableAttributeImpl( "objectClass" );
        objectClass.add( "top" );
        objectClass.add( "domain" );
        attrs.put( objectClass );
        attrs.put( "dc", "apache" );
        partConfig.setContextEntry( attrs );

        return partConfig;
    }


    /**
     * Shuts down the backing store, optionally deleting the database directory.
     *
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();

        Hashtable env = new Hashtable();

        env.put( Context.PROVIDER_URL, "ou=system" );
        env.put( Context.INITIAL_CONTEXT_FACTORY, CoreContextFactory.class.getName() );
        env.putAll( new ShutdownConfiguration().toJndiEnvironment() );
        env.put( Context.SECURITY_PRINCIPAL, "uid=admin,ou=system" );
        env.put( Context.SECURITY_CREDENTIALS, "secret" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );

        new InitialContext( env );

        doDelete( config.getWorkingDirectory() );
    }


    /**
     * Deletes the working directory.
     */
    protected void doDelete( File wkdir ) throws IOException
    {
        if ( doDelete )
        {
            if ( wkdir.exists() )
            {
                recursiveRemoveDir( wkdir );
            }

            if ( wkdir.exists() )
            {
                throw new IOException( "Failed to delete: " + wkdir );
            }
        }
    }


    private void recursiveRemoveDir( File directory )
    {
        String[] filelist = directory.list();
        File tmpFile = null;

        for ( int ii = 0; ii < filelist.length; ii++ )
        {
            tmpFile = new File( directory.getAbsolutePath(), filelist[ii] );
            if ( tmpFile.isDirectory() )
            {
                recursiveRemoveDir( tmpFile );
            }
            else
            {
                if ( tmpFile.isFile() )
                {
                    tmpFile.delete();
                }
            }
        }

        directory.delete();
    }


    /**
     * Opens the LDIF file and loads the entries into the context.
     */
    protected void load( DirContext ctx, String ldifPath )
    {
        Name rdn = null;

        try
        {
            InputStream in = getLdifStream( ldifPath );

            Iterator iterator = new LdifReader( in );

            while ( iterator.hasNext() )
            {
                Entry entry = ( Entry ) iterator.next();

                String dn = entry.getDn();
                Attributes attributes = entry.getAttributes();

                if ( attributes.get( "objectClass" ).contains( "krb5KDCEntry" ) )
                {
                    String pw = ( String ) attributes.get( "userpassword" ).get();
                    String krbPrincipal = ( String ) attributes.get( KerberosAttribute.PRINCIPAL ).get();

                    KerberosPrincipal principal = new KerberosPrincipal( krbPrincipal );
                    KerberosKey key = new KerberosKey( principal, pw.toCharArray(), "DES" );

                    byte[] encodedKey = key.getEncoded();

                    attributes.put( KerberosAttribute.KEY, encodedKey );
                    attributes.put( KerberosAttribute.VERSION, Integer.toString( key.getVersionNumber() ) );
                    attributes.put( KerberosAttribute.TYPE, Integer.toString( key.getKeyType() ) );
                }

                rdn = getRelativeName( ctx.getNameInNamespace(), dn );

                try
                {
                    ctx.lookup( rdn );

                    log.info( "Found {}, will not create.", rdn );
                }
                catch ( Exception e )
                {
                    ctx.createSubcontext( rdn, attributes );

                    log.info( "Created {}.", rdn );
                }
            }
        }
        catch ( FileNotFoundException fnfe )
        {
            log.error( "LDIF file does not exist." );
            return;
        }
        catch ( NamingException ne )
        {
            log.error( "Failed to import LDIF into backing store.", ne );
            return;
        }

        try
        {
            InputStream in = getLdifStream( ldifPath );

            Iterator iterator = new LdifReader( in );

            while ( iterator.hasNext() )
            {
                Entry entry = ( Entry ) iterator.next();

                String dn = entry.getDn();

                rdn = getRelativeName( ctx.getNameInNamespace(), dn );

                Object stored = ctx.lookup( rdn );

                log.debug( "Lookup for {} returned {}.", rdn, stored );

                if ( stored == null )
                {
                    log.error( "{} was null.", rdn );

                    throw new IllegalStateException( "LDIF entries not being pushed to disk." );
                }
            }
        }
        catch ( Exception e )
        {
            log.error( "Failed to find {}", rdn );

            if ( log.isDebugEnabled() )
            {
                log.error( "Failed to import LDIF into backing store.", e );
            }
            else
            {
                log.error( "Failed to import LDIF into backing store." );
            }

            return;
        }
    }


    /**
     * Tries to find an LDIF file either on the file system or packaged within a jar.
     *
     * @return the input stream to the ldif file.
     * @throws FileNotFoundException if the file cannot be found.
     */
    private InputStream getLdifStream( String ldifPath ) throws FileNotFoundException
    {
        File file = new File( ldifPath );

        InputStream in = null;

        if ( file.exists() )
        {
            in = new FileInputStream( file );
        }
        else
        {
            // if file not on system see if something is bundled with the jar ...
            in = getClass().getResourceAsStream( ldifPath );

            if ( in == null )
            {
                throw new FileNotFoundException( "LDIF file does not exist." );
            }
        }

        // in = loadClass.getResourceAsStream( ldifPath );

        return in;
    }


    protected Name getRelativeName( String nameInNamespace, String baseDn ) throws NamingException
    {
        Properties props = new Properties();
        props.setProperty( "jndi.syntax.direction", "right_to_left" );
        props.setProperty( "jndi.syntax.separator", "," );
        props.setProperty( "jndi.syntax.ignorecase", "true" );
        props.setProperty( "jndi.syntax.trimblanks", "true" );

        Name searchBaseDn = null;

        Name ctxRoot = new CompoundName( nameInNamespace, props );
        searchBaseDn = new CompoundName( baseDn, props );

        if ( !searchBaseDn.startsWith( ctxRoot ) )
        {
            throw new NamingException( "Invalid search base " + baseDn );
        }

        for ( int ii = 0; ii < ctxRoot.size(); ii++ )
        {
            searchBaseDn.remove( 0 );
        }

        return searchBaseDn;
    }


    /*
     * escape LDAP filter characters as \HH (hex value).
     */
    protected String escapedValue( String value )
    {
        StringBuffer escapedBuf = new StringBuffer();
        String specialChars = "*()\\";
        char c;

        for ( int i = 0; i < value.length(); ++i )
        {
            c = value.charAt( i );
            if ( specialChars.indexOf( c ) >= 0 )
            { // escape it
                escapedBuf.append( '\\' );
                String hexString = Integer.toHexString( c );
                if ( hexString.length() < 2 )
                {
                    escapedBuf.append( '0' );
                }
                escapedBuf.append( hexString );
            }
            else
            {
                escapedBuf.append( c );
            }
        }
        return escapedBuf.toString();
    }


    protected void printAttr( Attributes attrs, String id )
    {
        if ( attrs == null || id == null )
        {
            System.out.println( "No attribute" );
            return;
        }

        try
        {
            Attribute attr;
            String a = ( attr = attrs.get( id ) ) != null ? ( String ) attr.get() : null;
            System.out.println( attr.getID() + ":\t" + a );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }
    }


    protected void printAttrs( Attributes attrs )
    {
        if ( attrs == null )
        {
            System.out.println( "No attributes" );
            return;
        }

        try
        {
            for ( NamingEnumeration ae = attrs.getAll(); ae.hasMore(); )
            {
                final Attribute attr = ( Attribute ) ae.next();

                for ( NamingEnumeration e = attr.getAll(); e.hasMore(); )
                {
                    System.out.println( attr.getID() + ":\t" + e.next() );
                }
            }
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }
    }
}
