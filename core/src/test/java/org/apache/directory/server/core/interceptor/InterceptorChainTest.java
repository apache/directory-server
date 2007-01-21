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
package org.apache.directory.server.core.interceptor;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.DirectoryServiceListener;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;
import javax.naming.Context;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;


/**
 * Unit test cases for InterceptorChain methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InterceptorChainTest extends TestCase
{
    private final MockInterceptor[] interceptorArray =
        { new MockInterceptor( "0" ), new MockInterceptor( "1" ), new MockInterceptor( "2" ),
            new MockInterceptor( "3" ), new MockInterceptor( "4" ) };
    private InterceptorChain chain;
    private List interceptors = new ArrayList( interceptorArray.length );


    protected void setUp() throws Exception
    {
        chain = new InterceptorChain();

        for ( int ii = 0; ii < interceptorArray.length; ii++ )
        {
            MutableInterceptorConfiguration config = new MutableInterceptorConfiguration();
            config.setInterceptor( interceptorArray[ii] );
            config.setName( interceptorArray[ii].getName() );
            chain.addLast( config );
        }
    }


    protected void tearDown() throws Exception
    {
        chain = null;
        interceptors.clear();
    }


    public void testNoBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn } );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( interceptorArray.length, interceptors.size() );
        for ( int ii = 0; ii < interceptorArray.length; ii++ )
        {
            assertEquals( interceptorArray[ii], interceptors.get( ii ) );
        }
    }


    public void testSingleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, Collections.singleton( "0" ) );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( interceptorArray.length - 1, interceptors.size() );
        for ( int ii = 0; ii < interceptorArray.length; ii++ )
        {
            if ( ii != 0 )
            {
                assertEquals( interceptorArray[ii], interceptors.get( ii - 1 ) );
            }
        }
        assertFalse( interceptors.contains( interceptorArray[0] ) );
    }


    public void testAdjacentDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Collection bypass = new HashSet();
        bypass.add( "0" );
        bypass.add( "1" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( interceptorArray.length - 2, interceptors.size() );
        for ( int ii = 0; ii < interceptorArray.length; ii++ )
        {
            if ( ii != 0 && ii != 1 )
            {
                assertEquals( interceptorArray[ii], interceptors.get( ii - 2 ) );
            }
        }
        assertFalse( interceptors.contains( interceptorArray[0] ) );
        assertFalse( interceptors.contains( interceptorArray[1] ) );
    }


    public void testFrontAndBackDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Collection bypass = new HashSet();
        bypass.add( "0" );
        bypass.add( "4" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( interceptorArray.length - 2, interceptors.size() );
        assertEquals( interceptorArray[1], interceptors.get( 0 ) );
        assertEquals( interceptorArray[2], interceptors.get( 1 ) );
        assertEquals( interceptorArray[3], interceptors.get( 2 ) );
        assertFalse( interceptors.contains( interceptorArray[0] ) );
        assertFalse( interceptors.contains( interceptorArray[4] ) );
    }


    public void testDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Collection bypass = new HashSet();
        bypass.add( "1" );
        bypass.add( "3" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( interceptorArray.length - 2, interceptors.size() );
        assertEquals( interceptorArray[0], interceptors.get( 0 ) );
        assertEquals( interceptorArray[2], interceptors.get( 1 ) );
        assertEquals( interceptorArray[4], interceptors.get( 2 ) );
        assertFalse( interceptors.contains( interceptorArray[1] ) );
        assertFalse( interceptors.contains( interceptorArray[3] ) );
    }


    public void testCompleteBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, PartitionNexusProxy.BYPASS_ALL_COLLECTION );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( dn );
        }
        catch ( Exception e )
        {
        }

        assertEquals( 0, interceptors.size() );
    }

    class MockInterceptor implements Interceptor
    {
        String name;


        public MockInterceptor(String name)
        {
            this.name = name;
        }


        public String getName()
        {
            return this.name;
        }


        public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg )
            throws NamingException
        {
        }


        public void destroy()
        {
        }


        public Attributes getRootDSE( NextInterceptor next ) throws NamingException
        {
            interceptors.add( this );
            return next.getRootDSE();
        }


        public LdapDN getMatchedName ( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            return next.getMatchedName( name );
        }


        public LdapDN getSuffix ( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            return next.getSuffix( name );
        }


        public Iterator listSuffixes ( NextInterceptor next ) throws NamingException
        {
            interceptors.add( this );
            return next.listSuffixes();
        }


        public void addContextPartition( NextInterceptor next, PartitionConfiguration cfg )
            throws NamingException
        {
            interceptors.add( this );
            next.addContextPartition( cfg );
        }


        public void removeContextPartition( NextInterceptor next, LdapDN suffix ) throws NamingException
        {
            interceptors.add( this );
            next.removeContextPartition( suffix );
        }


        public boolean compare( NextInterceptor next, LdapDN name, String oid, Object value ) throws NamingException
        {
            interceptors.add( this );
            return next.compare( name, oid, value );
        }


        public void delete( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            next.delete( name );
        }


        public void add(NextInterceptor next, LdapDN name, Attributes entry)
            throws NamingException
        {
            interceptors.add( this );
            next.add(name, entry );
        }


        public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes attributes ) throws NamingException
        {
            interceptors.add( this );
            next.modify( name, modOp, attributes );
        }


        public void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] items ) throws NamingException
        {
            interceptors.add( this );
            next.modify( name, items );
        }


        public NamingEnumeration list( NextInterceptor next, LdapDN baseName ) throws NamingException
        {
            interceptors.add( this );
            return next.list( baseName );
        }


        public NamingEnumeration search( NextInterceptor next, LdapDN baseName, Map environment, ExprNode filter,
            SearchControls searchControls ) throws NamingException
        {
            interceptors.add( this );
            return next.search( baseName, environment, filter, searchControls );
        }


        public Attributes lookup( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            return next.lookup( name );
        }


        public Attributes lookup( NextInterceptor next, LdapDN dn, String[] attrIds ) throws NamingException
        {
            interceptors.add( this );
            return next.lookup( dn, attrIds );
        }


        public boolean hasEntry( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            return next.hasEntry( name );
        }


        public boolean isSuffix( NextInterceptor next, LdapDN name ) throws NamingException
        {
            interceptors.add( this );
            return next.isSuffix( name );
        }


        public void modifyRn( NextInterceptor next, LdapDN name, String newRn, boolean deleteOldRn )
            throws NamingException
        {
            interceptors.add( this );
            next.modifyRn( name, newRn, deleteOldRn );
        }


        public void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName ) throws NamingException
        {
            interceptors.add( this );
            next.move( oldName, newParentName );
        }


        public void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName, String newRn, boolean deleteOldRn )
            throws NamingException
        {
            interceptors.add( this );
            next.move( oldName, newParentName, newRn, deleteOldRn );
        }


        public void bind( NextInterceptor next, LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId )
            throws NamingException
        {
            interceptors.add( this );
            next.bind( bindDn, credentials, mechanisms, saslAuthId );
        }


        public void unbind( NextInterceptor next, LdapDN bindDn ) throws NamingException
        {
            interceptors.add( this );
            next.unbind( bindDn );
        }
    }

    class MockDirectoryService extends DirectoryService
    {
        public void startup( DirectoryServiceListener listener, Hashtable environment ) throws NamingException
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        public void shutdown() throws NamingException
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        public void sync() throws NamingException
        {
            //To change body of implemented methods use File | Settings | File Templates.
        }


        public boolean isStarted()
        {
            return false; //To change body of implemented methods use File | Settings | File Templates.
        }


        public DirectoryServiceConfiguration getConfiguration()
        {
            return null; //To change body of implemented methods use File | Settings | File Templates.
        }


        public Context getJndiContext( String baseName ) throws NamingException
        {
            return null; //To change body of implemented methods use File | Settings | File Templates.
        }


        public Context getJndiContext( LdapDN principalDn, String principal, byte[] credential, 
            String authentication, String baseName ) throws NamingException
        {
            return null; //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
