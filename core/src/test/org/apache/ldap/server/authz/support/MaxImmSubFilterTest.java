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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.configuration.DirectoryPartitionConfiguration;
import org.apache.ldap.server.interceptor.NextInterceptor;

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
    private static final NextInterceptor NEXT_INTERCEPTOR_A = new NextInterceptorImpl( 1 );
    private static final NextInterceptor NEXT_INTERCEPTOR_B = new NextInterceptorImpl( 2 );
    
    
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
                        tuples, OperationScope.ATTRIBUTE_TYPE, NEXT_INTERCEPTOR_A, null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ) );

        Assert.assertEquals(
                tuples, filter.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, NEXT_INTERCEPTOR_A,
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
                        NEXT_INTERCEPTOR_A, null, null, null, null, ROOTDSE_NAME, null, null, ENTRY, null ) );
    }
    
    public void testZeroTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter();
        
        Assert.assertEquals(
                0, filter.filter(
                        EMPTY_COLLECTION, OperationScope.ENTRY,
                        NEXT_INTERCEPTOR_A, null, null, null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );
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
                        tuples, OperationScope.ENTRY, NEXT_INTERCEPTOR_A, null, null,
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
                        tuples, OperationScope.ENTRY, NEXT_INTERCEPTOR_A, null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );

        Assert.assertEquals(
                0, filter.filter(
                        tuples, OperationScope.ENTRY, NEXT_INTERCEPTOR_B, null, null,
                        null, null, ENTRY_NAME, null, null, ENTRY, null ).size() );
    }
    
    private static class NextInterceptorImpl implements NextInterceptor
    {
        private final List list;

        public NextInterceptorImpl( int count )
        {
            list = new ArrayList();
            for( int i = 0; i < count; i++ )
            {
                list.add( new Object() );
            }
        }

        public boolean compare( Name name, String oid, Object value ) throws NamingException
        {
            return false;
        }

        public Attributes getRootDSE() throws NamingException
        {
            return null;
        }

        public Name getMatchedName( Name name, boolean normalized ) throws NamingException
        {
            return null;
        }

        public Name getSuffix( Name name, boolean normalized ) throws NamingException
        {
            return null;
        }

        public Iterator listSuffixes( boolean normalized ) throws NamingException
        {
            return null;
        }

        public void addContextPartition( DirectoryPartitionConfiguration cfg ) throws NamingException
        {
        }

        public void removeContextPartition( Name suffix ) throws NamingException
        {
        }

        public void delete( Name name ) throws NamingException
        {
        }

        public void add( String userProvidedName, Name normalizedName, Attributes entry ) throws NamingException
        {
        }

        public void modify( Name name, int modOp, Attributes attributes ) throws NamingException
        {
        }

        public void modify( Name name, ModificationItem[] items ) throws NamingException
        {
        }

        public NamingEnumeration list( Name baseName ) throws NamingException
        {
            return null;
        }

        public NamingEnumeration search( Name baseName, Map environment, ExprNode filter, SearchControls searchControls ) throws NamingException
        {
            final Iterator i = list.iterator();
            
            return new NamingEnumeration()
            {

                public Object next() throws NamingException
                {
                    return i.next();
                }

                public boolean hasMore() throws NamingException
                {
                    return i.hasNext();
                }

                public void close() throws NamingException
                {
                }

                public boolean hasMoreElements()
                {
                    return i.hasNext();
                }

                public Object nextElement()
                {
                    return i.next();
                }
            };
        }

        public Attributes lookup( Name name ) throws NamingException
        {
            return null;
        }

        public Attributes lookup( Name name, String[] attrIds ) throws NamingException
        {
            return null;
        }

        public boolean hasEntry( Name name ) throws NamingException
        {
            return false;
        }

        public boolean isSuffix( Name name ) throws NamingException
        {
            return false;
        }

        public void modifyRn( Name name, String newRn, boolean deleteOldRn ) throws NamingException
        {
        }

        public void move( Name oldName, Name newParentName ) throws NamingException
        {
        }

        public void move( Name oldName, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
        {
        }
    }
}
