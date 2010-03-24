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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.OperationManager;
import org.apache.directory.server.core.ReferralManager;
import org.apache.directory.server.core.changelog.ChangeLog;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.journal.Journal;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.partition.DefaultPartitionNexus;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.replication.ReplicationConfiguration;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.csn.Csn;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/**
 * Unit test cases for InterceptorChain methods which test bypass 
 * instructions in the chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InterceptorChainTest
{
    private static final int INTERCEPTOR_COUNT = 5;
    private InterceptorChain chain;
    List<MockInterceptor> interceptors = new ArrayList<MockInterceptor>( INTERCEPTOR_COUNT );

    
    public InterceptorChainTest()
    {
    }
    
    
    @Before
    public void setUp() throws Exception
    {
        chain = new InterceptorChain();

        for ( int ii = 0; ii < INTERCEPTOR_COUNT; ii++ )
        {
            MockInterceptor interceptor = new MockInterceptor();
            interceptor.setTest( this );
            interceptor.setName( Integer.toString( ii ) );
            chain.addLast(interceptor);
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        opContext.setByPassed( Collections.singleton( "0" ) );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "1" );
        opContext.setByPassed( bypass );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "0" );
        bypass.add( "4" );
        opContext.setByPassed( bypass );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        Set<String> bypass = new HashSet<String>();
        bypass.add( "1" );
        bypass.add( "3" );
        opContext.setByPassed( bypass );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
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
        DN dn = new DN( "ou=system" );
        DirectoryService ds = new MockDirectoryService();
        DefaultCoreSession session = new DefaultCoreSession( 
            new LdapPrincipal( new DN(), AuthenticationLevel.STRONG ), ds );
        LookupOperationContext opContext = new LookupOperationContext( session, dn );
        opContext.setByPassed( ByPassConstants.BYPASS_ALL_COLLECTION );
        InvocationStack.getInstance().push( opContext );

        try
        {
            chain.lookup( opContext );
        }
        catch ( Exception e )
        {
        }

        assertEquals( 0, interceptors.size() );
    }

    
    class MockDirectoryService implements DirectoryService
    {
        public Hashtable<String, Object> getEnvironment()
        {
            return null;
        }


        public void setEnvironment( Hashtable<String, Object> environment )
        {
        }


        public long revert( long revision ) throws NamingException
        {
            return 0;
        }


        public long revert() throws NamingException
        {
            return 0;
        }


        public DefaultPartitionNexus getPartitionNexus()
        {
            return null;
        }


        public InterceptorChain getInterceptorChain()
        {
            return null;
        }


        public void addPartition( Partition partition ) throws NamingException
        {
        }


        public void removePartition( Partition partition ) throws NamingException
        {
        }


        public ReferralManager getReferralManager()
        {
            return null;
        }


        public void setReferralManager( ReferralManager referralManager )
        {
        }


        public SchemaManager getSchemaManager()
        {
            return null;
        }


        public void setRegistries( Registries registries )
        {
        }


        public SchemaService getSchemaService()
        {
            return null;
        }


        public void setSchemaService( SchemaService schemaService )
        {

        }


        public void startup() throws NamingException
        {
        }


        public void shutdown() throws NamingException
        {
        }


        public void sync() throws NamingException
        {
        }


        public boolean isStarted()
        {
            return false;
        }


        public LdapContext getJndiContext() throws NamingException
        {
            return null;
        }


        public DirectoryService getDirectoryService()
        {
            return null;
        }


        public LdapContext getJndiContext( String baseName ) throws NamingException
        {
            return null;
        }


        public LdapContext getJndiContext( LdapPrincipal principal ) throws NamingException
        {
            return null;
        }


        public LdapContext getJndiContext( LdapPrincipal principal, String dn ) throws NamingException
        {
            return null;
        }


        public LdapContext getJndiContext( DN principalDn, String principal, byte[] credential,
            String authentication, String baseName ) throws NamingException
        {
            return null;
        }


        public void setInstanceId( String instanceId )
        {
        }


        public String getInstanceId()
        {
            return null;
        }


        public Set<? extends Partition> getPartitions()
        {
            return null;
        }


        public void setPartitions( Set<? extends Partition> partitions )
        {
        }


        public boolean isAccessControlEnabled()
        {
            return false;
        }


        public void setAccessControlEnabled( boolean accessControlEnabled )
        {
        }


        public boolean isAllowAnonymousAccess()
        {
            return false;
        }


        public void setAllowAnonymousAccess( boolean enableAnonymousAccess )
        {
        }


        public List<Interceptor> getInterceptors()
        {
            return null;
        }


        public void setInterceptors( List<Interceptor> interceptors )
        {
        }


        public List<LdifEntry> getTestEntries()
        {
            return null;
        }


        public void setTestEntries( List<? extends LdifEntry> testEntries )
        {
        }


        public File getWorkingDirectory()
        {
            return null;
        }


        public void setWorkingDirectory( File workingDirectory )
        {
        }


        public void validate()
        {
        }


        public void setShutdownHookEnabled( boolean shutdownHookEnabled )
        {
        }


        public boolean isShutdownHookEnabled()
        {
            return false;
        }


        public void setExitVmOnShutdown( boolean exitVmOnShutdown )
        {
        }


        public boolean isExitVmOnShutdown()
        {
            return false;
        }


        public void setMaxSizeLimit( long maxSizeLimit )
        {
        }


        public long getMaxSizeLimit()
        {
            return 0;
        }


        public void setMaxTimeLimit( int maxTimeLimit )
        {
        }


        public int getMaxTimeLimit()
        {
            return 0;
        }


        public void setSystemPartition( Partition systemPartition )
        {
        }


        public Partition getSystemPartition()
        {
            return null;
        }


        public boolean isDenormalizeOpAttrsEnabled()
        {
            return false;
        }


        public void setDenormalizeOpAttrsEnabled( boolean denormalizeOpAttrsEnabled )
        {
        }
        
        public void setChangeLog( ChangeLog changeLog )
        {
            
        }

        
        public ChangeLog getChangeLog()
        {
            return null;
        }


        public Journal getJournal()
        {
            return null;
        }


        public ServerEntry newEntry( DN dn ) throws NamingException
        {
            return null;
        }

        
        public ServerEntry newEntry( String ldif, String dn )
        {
            return null;
        }


        public OperationManager getOperationManager()
        {
            return null;
        }


        public CoreSession getSession() throws Exception
        {
            return null;
        }


        public CoreSession getSession( LdapPrincipal principal ) throws Exception
        {
            return null;
        }


        public CoreSession getSession( DN principalDn, byte[] credentials ) throws Exception
        {
            return null;
        }


        public CoreSession getSession( DN principalDn, byte[] credentials, String saslMechanism, String saslAuthId )
            throws Exception
        {
            return null;
        }


        public CoreSession getAdminSession() throws Exception
        {
            return null;
        }


        public EventService getEventService()
        {
            // TODO Auto-generated method stub
            return null;
        }


        public void setEventService( EventService eventService )
        {
            // TODO Auto-generated method stub
            
        }

        
        public boolean isPasswordHidden()
        {
            return false;
        }
        
        
        public void setPasswordHidden( boolean passwordHidden )
        {
        }


        public int getMaxPDUSize()
        {
            return Integer.MAX_VALUE;
        }


        public void setMaxPDUSize( int maxPDUSize )
        {
            // Do nothing
        }

        
        public Interceptor getInterceptor( String interceptorName )
        {
            return null;
        }
        
        
        public Csn getCSN()
        {
            return null;
        }
        
        
        public int getReplicaId()
        {
            return 0;
        }
        
        
        public void setReplicaId( int replicaId )
        {
            
        }


        public void setJournal( Journal journal )
        {
            // TODO Auto-generated method stub
            
        }


        public void setReplicationConfiguration( ReplicationConfiguration replicationConfig )
        {
            // TODO Auto-generated method stub
            
        }


        public ReplicationConfiguration getReplicationConfiguration()
        {
            // TODO Auto-generated method stub
            return null;
        }


        public void setSchemaManager( SchemaManager schemaManager )
        {
            // TODO Auto-generated method stub
            
        }
    }
}
