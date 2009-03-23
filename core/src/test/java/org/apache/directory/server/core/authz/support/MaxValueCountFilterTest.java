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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authz.support.MaxValueCountFilter;
import org.apache.directory.server.core.authz.support.OperationScope;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.impl.DefaultDirectoryService;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.aci.ProtectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Tests {@link MaxValueCountFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MaxValueCountFilterTest
{
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );

    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );

    private static final Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
    private static ServerEntry ENTRY;
    private static ServerEntry FULL_ENTRY;

    static
    {
        Collection<MaxValueCountItem> mvcItems = new ArrayList<MaxValueCountItem>();
        mvcItems.add( new MaxValueCountItem( "cn", 2 ) );
        PROTECTED_ITEMS.add( new ProtectedItem.MaxValueCount( mvcItems ) );
    }


    /** A reference to the directory service */
    private static DirectoryService service;

    
    @BeforeClass public static void init() throws NamingException
    {
        service = new DefaultDirectoryService();
    }
    
    @Before public void setup() throws NamingException
    {
        LdapDN entryName = new LdapDN( "ou=test, ou=system" );
        ENTRY = new DefaultServerEntry( service.getRegistries(), entryName );
        FULL_ENTRY = new DefaultServerEntry( service.getRegistries(), entryName );
        
        ENTRY.put( "cn", "1" );
        FULL_ENTRY.put( "cn", "1", "2", "3" );
    }


    @Test public void testWrongScope() throws Exception
    {
        MaxValueCountFilter filter = new MaxValueCountFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, 
            EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null, null,
            null, null, null, null, null, null, null ) );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ENTRY, null, null, null, null, null, null,
            null, null, null, null, null ) );
    }


    @Test public void testZeroTuple() throws Exception
    {
        MaxValueCountFilter filter = new MaxValueCountFilter();

        assertEquals( 0, filter.filter( null, EMPTY_ACI_TUPLE_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, 
            null, null, null, null, null, null, null, null, null, null, null ).size() );
    }


    @Test public void testDenialTuple() throws Exception
    {
        MaxValueCountFilter filter = new MaxValueCountFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS, 
            EMPTY_MICRO_OPERATION_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
            null, null, null, "cn", null, ENTRY, null, null ) );
        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
            null, null, null, "cn", null, FULL_ENTRY, null, null ) );
    }


    @Test public void testGrantTuple() throws Exception
    {
        MaxValueCountFilter filter = new MaxValueCountFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        
        // Test with this ACI :
        // 
        tuples.add( new ACITuple( 
            EMPTY_USER_CLASS_COLLECTION, 
            AuthenticationLevel.NONE, 
            PROTECTED_ITEMS, 
            EMPTY_MICRO_OPERATION_SET, 
            true, 
            0 ) );

        assertEquals( 1, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, "cn", null, ENTRY, null, ENTRY ).size() );

        assertEquals( 0, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, "cn", null, FULL_ENTRY, null, FULL_ENTRY ).size() );
    }
}
