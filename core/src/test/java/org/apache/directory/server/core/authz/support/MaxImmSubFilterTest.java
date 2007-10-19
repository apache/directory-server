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
package org.apache.directory.server.core.authz.support;


import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.schema.SchemaManager;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.schema.registries.Registries;
import org.apache.directory.shared.ldap.aci.*;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.naming.directory.DirContext;

import java.io.File;
import java.util.*;


/**
 * Tests {@link MaxImmSubFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MaxImmSubFilterTest extends TestCase
{
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );

    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );

    private static final LdapDN ROOTDSE_NAME = new LdapDN();
    private static final LdapDN ENTRY_NAME;
    private static final Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
    private static final Attributes ENTRY = new AttributesImpl();

    static
    {
        try
        {
            ENTRY_NAME = new LdapDN( "ou=test, ou=system" );
        }
        catch ( NamingException e )
        {
            throw new Error();
        }

        PROTECTED_ITEMS.add( new ProtectedItem.MaxImmSub( 2 ) );
    }


    public void testWrongScope() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
            EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null, null ) );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
            null, null, ENTRY_NAME, null, null, ENTRY, null, null ) );
    }


    public void testRootDSE() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
            EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null,
            ROOTDSE_NAME, null, null, ENTRY, null, null ) );
    }


    public void testZeroTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Assert.assertEquals( 0, filter.filter( EMPTY_ACI_TUPLE_COLLECTION, OperationScope.ENTRY, null, null, null, null, null,
            ENTRY_NAME, null, null, ENTRY, null, null ).size() );
    }


    public void testDenialTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
            PROTECTED_ITEMS, EMPTY_MICRO_OPERATION_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null,
            ENTRY_NAME, null, null, ENTRY, null, null ) );
    }


    public void testGrantTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, 
            PROTECTED_ITEMS, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, new MockProxy( 1 ), null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null, null ).size() );

        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, new MockProxy( 3 ), null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null, null ).size() );
    }

    class MockProxy extends PartitionNexusProxy
    {
        final int count;


        public MockProxy(int count) throws NamingException 
        {
            super( new DeadContext(), new MockDirectoryService() );
            this.count = count;
        }


        public NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
            throws NamingException
        {
            //noinspection unchecked
            return new BogusEnumeration( count );
        }


        public NamingEnumeration<SearchResult> search( SearchOperationContext opContext, Collection bypass ) throws NamingException
        {
            //noinspection unchecked
            return new BogusEnumeration( count );
        }
    }

    class MockDirectoryService extends DirectoryService
    {
        public Hashtable<String, Object> getEnvironment()
        {
            return null;
        }


        public void setEnvironment( Hashtable<String, Object> environment )
        {
        }


        public PartitionNexus getPartitionNexus()
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


        public Registries getRegistries()
        {
            return null;
        }


        public void setRegistries( Registries registries )
        {
        }


        public SchemaManager getSchemaManager()
        {
            return null;
        }


        public void setSchemaManager( SchemaManager schemaManager )
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
            return true;
        }


        public DirContext getJndiContext() throws NamingException
        {
            return null;
        }


        public DirectoryService getDirectoryService()
        {
            return null;
        }


        public DirContext getJndiContext( String baseName ) throws NamingException
        {
            return null;
        }


        public DirContext getJndiContext( LdapPrincipal principal ) throws NamingException
        {
            return null;
        }


        public DirContext getJndiContext( LdapPrincipal principal, String dn ) throws NamingException
        {
            return null;
        }


        public DirContext getJndiContext( LdapDN principalDn, String principal, byte[] credential, 
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


        public List<Entry> getTestEntries()
        {
            return null;
        }


        public void setTestEntries( List<? extends Entry> testEntries )
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


        public void setMaxSizeLimit( int maxSizeLimit )
        {

        }


        public int getMaxSizeLimit()
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
    }

    class BogusEnumeration implements NamingEnumeration
    {
        final int count;
        int ii;


        public BogusEnumeration(int count)
        {
            this.count = count;
        }


        public Object next() throws NamingException
        {
            if ( ii >= count )
            {
                throw new NoSuchElementException();
            }

            ii++;
            
            return new Object();
        }


        public boolean hasMore() throws NamingException
        {
            return ii < count;
        }


        public void close() throws NamingException
        {
            ii = count;
        }


        public boolean hasMoreElements()
        {
            return ii < count;
        }


        public Object nextElement()
        {
            if ( ii >= count )
            {
                throw new NoSuchElementException();
            }

            ii++;
            
            return new Object();
        }
    }
}
