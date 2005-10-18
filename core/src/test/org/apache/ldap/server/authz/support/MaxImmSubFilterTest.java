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
package org.apache.ldap.server.authz.support;

import java.util.*;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.SearchControls;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.partition.DirectoryPartitionNexusProxy;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.DirectoryServiceListener;
import org.apache.ldap.server.DirectoryServiceConfiguration;
import org.apache.ldap.server.jndi.DeadContext;


/**
 * Tests {@link MaxImmSubFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MaxImmSubFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION =
        Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET =
        Collections.unmodifiableSet( new HashSet() );

    private static final Name ROOTDSE_NAME = new LdapName();
    private static final Name ENTRY_NAME;
    private static final Collection PROTECTED_ITEMS = new ArrayList();
    private static final Attributes ENTRY = new BasicAttributes();


    static
    {
        try
        {
            ENTRY_NAME = new LdapName( "ou=test, ou=system" );
        }
        catch( NamingException e )
        {
            throw new Error();
        }

        PROTECTED_ITEMS.add( new ProtectedItem.MaxImmSub( 2 ) );
    }

    public void testWrongScope() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ) );

        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null,
                        null, null, null, null, ENTRY_NAME, null, null, ENTRY, null ) );
    }

    public void testRootDSE() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, OperationScope.ENTRY,
                        null, null, null, null, null, ROOTDSE_NAME, null, null, ENTRY, null ) );
    }

    public void testZeroTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();

        Assert.assertEquals(
                0, filter.filter(
                        EMPTY_COLLECTION, OperationScope.ENTRY,
                        null, null, null, null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );
    }

    public void testDenialTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS,
                EMPTY_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, OperationScope.ENTRY, null, null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ) );
    }


    public void testGrantTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS,
                EMPTY_SET, true, 0 ) );

        Assert.assertEquals(
                1, filter.filter(
                        tuples, OperationScope.ENTRY, new MockProxy(1), null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );

        Assert.assertEquals(
                0, filter.filter(
                        tuples, OperationScope.ENTRY, new MockProxy(3), null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );
    }


    class MockProxy extends DirectoryPartitionNexusProxy
    {
        final int count;

        public MockProxy( int count )
        {
            super( new DeadContext(), new MockDirectoryService() );
            this.count = count;
        }


        public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls ) throws NamingException
        {
            return new BogusEnumeration( count );
        }


        public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls, Collection bypass ) throws NamingException
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
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        public Context getJndiContext( String baseName ) throws NamingException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }


        public Context getJndiContext( String principal, byte[] credential, String authentication, String baseName ) throws NamingException
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }


    class BogusEnumeration implements NamingEnumeration
    {
        final int count;
        int ii;


        public BogusEnumeration( int count )
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
