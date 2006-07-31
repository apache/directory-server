/*
 *   @(#) $Id$
 *
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
package org.apache.directory.server.core.authz.support;


import java.util.*;

import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.DirectoryServiceListener;
import org.apache.directory.server.core.jndi.DeadContext;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Tests {@link MaxImmSubFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MaxImmSubFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET = Collections.unmodifiableSet( new HashSet() );

    private static final LdapDN ROOTDSE_NAME = new LdapDN();
    private static final LdapDN ENTRY_NAME;
    private static final Collection PROTECTED_ITEMS = new ArrayList();
    private static final Attributes ENTRY = new BasicAttributes();

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
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION, EMPTY_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null ) );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
            null, null, ENTRY_NAME, null, null, ENTRY, null ) );
    }


    public void testRootDSE() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION, EMPTY_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null,
            ROOTDSE_NAME, null, null, ENTRY, null ) );
    }


    public void testZeroTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Assert.assertEquals( 0, filter.filter( EMPTY_COLLECTION, OperationScope.ENTRY, null, null, null, null, null,
            ENTRY_NAME, null, null, ENTRY, null ).size() );
    }


    public void testDenialTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS, EMPTY_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals( tuples, filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null,
            ENTRY_NAME, null, null, ENTRY, null ) );
    }


    public void testGrantTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple( EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS, EMPTY_SET, true, 0 ) );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ENTRY, new MockProxy( 1 ), null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null ).size() );

        Assert.assertEquals( 0, filter.filter( tuples, OperationScope.ENTRY, new MockProxy( 3 ), null, null, null,
            null, ENTRY_NAME, null, null, ENTRY, null ).size() );
    }

    class MockProxy extends PartitionNexusProxy
    {
        final int count;


        public MockProxy(int count)
        {
            super( new DeadContext(), new MockDirectoryService() );
            this.count = count;
        }


        public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
            throws NamingException
        {
            return new BogusEnumeration( count );
        }


        public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls,
                                         Collection bypass ) throws NamingException
        {
            return new BogusEnumeration( count );
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
            return true;
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
