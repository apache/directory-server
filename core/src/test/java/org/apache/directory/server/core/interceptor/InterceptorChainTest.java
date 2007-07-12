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
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.DirectoryServiceListener;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Unit test cases for InterceptorChain methods which test bypass 
 * instructions in the chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InterceptorChainTest extends TestCase
{
    private static final int INTERCEPTOR_COUNT = 5;
    private InterceptorChain chain;
    List<MockInterceptor> interceptors = new ArrayList<MockInterceptor>( INTERCEPTOR_COUNT );

    
    public InterceptorChainTest()
    {
    }
    
    
    protected void setUp() throws Exception
    {
        chain = new InterceptorChain();

        for ( int ii = 0; ii < INTERCEPTOR_COUNT; ii++ )
        {
            MutableInterceptorConfiguration config = new MutableInterceptorConfiguration();
            config.setInterceptorClassName( MockInterceptor.class.getName() );
            config.setName( Integer.toString( ii ) );
            chain.addLast( config );
        }
        
        List interceptorsInChain = chain.getAll();
        for ( int ii = 0; ii < INTERCEPTOR_COUNT; ii++ )
        {
            MockInterceptor interceptor = ( MockInterceptor ) interceptorsInChain.get( ii );
            interceptor.setTest( this );
            interceptor.setName( Integer.toString( ii ) );
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
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]{ dn } );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT, interceptors.size() );
        for ( int ii = 0; ii < INTERCEPTOR_COUNT; ii++ )
        {
            assertEquals( Integer.toString( ii ), interceptors.get( ii ).getName() );
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
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 1, interceptors.size() );
        for ( int ii = 1; ii < INTERCEPTOR_COUNT; ii++ )
        {
            assertEquals( Integer.toString( ii ), interceptors.get( ii - 1 ).getName() );
        }
    }


    public void testAdjacentDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "1" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 2, interceptors.size() );
        for ( int ii = 2; ii < INTERCEPTOR_COUNT; ii++ )
        {
            assertEquals( Integer.toString( ii ), interceptors.get( ii - 2 ).getName() );
        }
    }


    public void testFrontAndBackDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "4" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 2, interceptors.size() );
        assertEquals( "1", interceptors.get( 0 ).getName() );
        assertEquals( "2", interceptors.get( 1 ).getName() );
        assertEquals( "3", interceptors.get( 2 ).getName() );
    }


    public void testDoubleBypass() throws NamingException
    {
        LdapDN dn = new LdapDN( "ou=system" );
        Context ctx = new DeadContext();
        DirectoryService ds = new MockDirectoryService();
        PartitionNexusProxy proxy = new PartitionNexusProxy( ctx, ds );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "1" );
        bypass.add( "3" );
        Invocation i = new Invocation( proxy, ctx, "lookup", new Object[]
            { dn }, bypass );
        InvocationStack.getInstance().push( i );

        try
        {
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 2, interceptors.size() );
        assertEquals( "0", interceptors.get( 0 ).getName() );
        assertEquals( "2", interceptors.get( 1 ).getName() );
        assertEquals( "4", interceptors.get( 2 ).getName() );
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
            chain.lookup( new LookupOperationContext( dn ) );
        }
        catch ( Exception e )
        {
        }

        assertEquals( 0, interceptors.size() );
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
