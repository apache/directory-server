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
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.schema.DefaultSchemaManager;
import org.apache.directory.shared.schema.loader.ldif.JarLdifSchemaLoader;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests {@link RestrictedByFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RestrictedByFilterTest
{
    private static final Collection<UserClass> UC_EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ACITuple> AT_EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Collection<ProtectedItem> PI_EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );
    private static final Set<MicroOperation> MO_EMPTY_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );

    private static final Collection<ProtectedItem> PROTECTED_ITEMS = new ArrayList<ProtectedItem>();
    private static ServerEntry ENTRY;

    static
    {
        Collection<RestrictedByItem> mvcItems = new ArrayList<RestrictedByItem>();
        mvcItems.add( new RestrictedByItem( "sn", "cn" ) );
        PROTECTED_ITEMS.add( new ProtectedItem.RestrictedBy( mvcItems ) );
    }


    /** A reference to the registries */
    private static Registries registries;

    
    @BeforeClass public static void setup() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

        SchemaManager sm = new DefaultSchemaManager( loader );

        boolean loaded = sm.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + ExceptionUtils.printErrors( sm.getErrors() ) );
        }

        registries = sm.getRegistries();

        LdapDN entryName = new LdapDN( "ou=test, ou=system" );
        PROTECTED_ITEMS.add( new ProtectedItem.MaxImmSub( 2 ) );
        ENTRY = new DefaultServerEntry( registries, entryName );

        ENTRY.put( "cn", "1", "2" );
    }


    @Test public void testWrongScope() throws Exception
    {
        RestrictedByFilter filter = new RestrictedByFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PI_EMPTY_COLLECTION, MO_EMPTY_SET, true, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE, null, null, null, null,
            null, null, null, null, null, null, null ) );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ENTRY, null, null, null, null, null, null,
            null, null, null, null, null ) );
    }


    @Test public void testZeroTuple() throws Exception
    {
        RestrictedByFilter filter = new RestrictedByFilter();

        assertEquals( 0, filter.filter( null, AT_EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null,
            null, null, null, null, null, null, null, null, null ).size() );
    }


    @Test public void testDenialTuple() throws Exception
    {
        RestrictedByFilter filter = new RestrictedByFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS, MO_EMPTY_SET, false, 0 ) );

        tuples = Collections.unmodifiableCollection( tuples );

        assertEquals( tuples, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null,
            null, null, null, "testAttr", null, ENTRY, null, null ) );
    }


    @Test public void testGrantTuple() throws Exception
    {
        RestrictedByFilter filter = new RestrictedByFilter();
        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( UC_EMPTY_COLLECTION, AuthenticationLevel.NONE, PROTECTED_ITEMS, MO_EMPTY_SET, true, 0 ) );

        assertEquals( 1, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, "sn", new ClientStringValue( "1" ), ENTRY, null, null ).size() );

        assertEquals( 1, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, "sn", new ClientStringValue( "2" ), ENTRY, null, null ).size() );

        assertEquals( 0, filter.filter( null, tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, "sn", new ClientStringValue( "3" ), ENTRY, null, null ).size() );
    }
}
