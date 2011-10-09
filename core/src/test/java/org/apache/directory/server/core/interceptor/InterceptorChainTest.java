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


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.MockDirectoryService;
import org.apache.directory.server.core.api.MockInterceptor;
import org.apache.directory.server.core.api.interceptor.InterceptorChain;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.partition.ByPassConstants;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Unit test cases for InterceptorChain methods which test bypass 
 * instructions in the chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InterceptorChainTest
{
    private static final int INTERCEPTOR_COUNT = 5;
    private InterceptorChain chain;
    List<MockInterceptor> interceptors = new ArrayList<MockInterceptor>( INTERCEPTOR_COUNT );
    private static SchemaManager schemaManager;

    @BeforeClass
    public static void init() throws Exception
    {
        schemaManager = new DefaultSchemaManager();
    }


    @Before
    public void setUp() throws Exception
    {
        chain = new InterceptorChain();

        for ( int ii = 0; ii < INTERCEPTOR_COUNT; ii++ )
        {
            MockInterceptor interceptor = new MockInterceptor( Integer.toString( ii ), interceptors );
            chain.addLast( interceptor );
        }

    }


    @After
    public void tearDown() throws Exception
    {
        chain = null;
        interceptors.clear();
    }


    @Test
    public void testNoBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn(schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
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


    @Test
    public void testSingleBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn( schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        lookupContext.setByPassed( Collections.singleton( "0" ) );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
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


    @Test
    public void testAdjacentDoubleBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn( schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "1" );
        lookupContext.setByPassed( bypass );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
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


    @Test
    public void testFrontAndBackDoubleBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn( schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "4" );
        lookupContext.setByPassed( bypass );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 2, interceptors.size() );
        assertEquals( "1", interceptors.get( 0 ).getName() );
        assertEquals( "2", interceptors.get( 1 ).getName() );
        assertEquals( "3", interceptors.get( 2 ).getName() );
    }


    @Test
    public void testDoubleBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn( schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "1" );
        bypass.add( "3" );
        lookupContext.setByPassed( bypass );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
        }
        catch ( Exception e )
        {
        }

        assertEquals( INTERCEPTOR_COUNT - 2, interceptors.size() );
        assertEquals( "0", interceptors.get( 0 ).getName() );
        assertEquals( "2", interceptors.get( 1 ).getName() );
        assertEquals( "4", interceptors.get( 2 ).getName() );
    }


    @Test
    public void testCompleteBypass() throws Exception
    {
        Dn dn = new Dn( schemaManager, "ou=system" );
        DirectoryService ds = new MockDirectoryService( 0 );
        DefaultCoreSession session = new DefaultCoreSession( new LdapPrincipal( schemaManager, new Dn( schemaManager ), AuthenticationLevel.STRONG ),
            ds );
        LookupOperationContext lookupContext = new LookupOperationContext( session, dn );
        lookupContext.setByPassed( ByPassConstants.BYPASS_ALL_COLLECTION );
        InvocationStack.getInstance().push( lookupContext );

        try
        {
            chain.lookup( lookupContext );
        }
        catch ( Exception e )
        {
        }

        assertEquals( 0, interceptors.size() );
    }
}
