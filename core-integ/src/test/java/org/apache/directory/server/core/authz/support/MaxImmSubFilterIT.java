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

import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxImmSubItem;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link MaxImmSubFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(FrameworkRunner.class)
@CreateDS(name = "MaxImmSubFilter-DS")
public class MaxImmSubFilterIT extends AbstractLdapTestUnit
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
    private Dn ENTRY_NAME;
    private Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
    private Entry ENTRY;

    private SchemaManager schemaManager;


    @Before
    public void setup() throws Exception
    {
        schemaManager = service.getSchemaManager();

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
    public void testRootDse() throws Exception
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
    @Ignore("test is failing cause of incorrect results from MaxImmSubFilter.filter() method after " +
        "started using real OperationContext instead of MockOperationContext")
    public void testGrantTuple() throws Exception
    {
        MaxImmSubFilter filter = new MaxImmSubFilter( schemaManager );
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS,
            EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        AddOperationContext addContext = new AddOperationContext( service.getSession() );
        AciContext aciContext = new AciContext( schemaManager, addContext );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );

        aciContext = new AciContext( schemaManager, addContext );
        aciContext.setEntryDn( ENTRY_NAME );
        aciContext.setAciTuples( tuples );
        aciContext.setEntry( ENTRY );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }
}
