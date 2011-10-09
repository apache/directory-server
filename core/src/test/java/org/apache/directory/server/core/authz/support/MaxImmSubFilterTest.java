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


import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.api.MockOperation;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxImmSubItem;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.mycila.junit.concurrent.Concurrency;
import com.mycila.junit.concurrent.ConcurrentJunitRunner;


/**
 * Tests {@link MaxImmSubFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrency()
public class MaxImmSubFilterTest
{
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ProtectedItem>() );

    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections
        .unmodifiableSet( new HashSet<MicroOperation>() );

    private static final Dn ROOTDSE_NAME = Dn.ROOT_DSE;
    private static Dn ENTRY_NAME;
    private static Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
    private static Entry ENTRY;

    /** A reference to the schemaManager */
    private static SchemaManager schemaManager;


    @BeforeClass
    public static void setup() throws Exception
    {
        schemaManager = new DefaultSchemaManager();

        ENTRY_NAME = new Dn( schemaManager, "ou=test, ou=system" );
        PROTECTED_ITEMS.add( new MaxImmSubItem( 2 ) );
        ENTRY = new DefaultEntry( schemaManager, ENTRY_NAME );
    }


    @Test
    public void testWrongScope() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE,
            EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        AciContext aciContext = new AciContext( schemaManager, null );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( tuples, filter.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ) );

        aciContext = new AciContext( schemaManager, null );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( tuples, filter.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ) );
    }


    @Test
    public void testRootDSE() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE,
            EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        AciContext aciContext = new AciContext( schemaManager, null );
        aciContext.setEntryDn( ROOTDSE_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( tuples, filter.filter( aciContext, OperationScope.ENTRY, null ) );
    }


    @Test
    public void testZeroTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( EMPTY_ACI_TUPLE_COLLECTION );
        aciContext.setEntry( ENTRY );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test
    public void testDenialTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS,
            EMPTY_MICRO_OPERATION_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( tuples, filter.filter( aciContext, OperationScope.ENTRY, null ) );
    }


    @Test
    public void testGrantTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS,
            EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        AciContext aciContext = new AciContext( schemaManager, new MockOperation( schemaManager, 1 ) );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( schemaManager, new MockOperation( schemaManager, 3 ) );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }
}
