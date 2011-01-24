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
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link MostSpecificUserClassFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class MostSpecificUserClassFilterTest
{
    private static final Set<Dn> EMPTY_NAME_SET = Collections.unmodifiableSet( new HashSet<Dn>() );
    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Set<SubtreeSpecification> EMPTY_SUBTREE_SPECIFICATION_COLLECTION = Collections.unmodifiableSet( new HashSet<SubtreeSpecification>() );
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );

    private static final List<ACITuple> TUPLES_A = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_B = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_C = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_D = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_E = new ArrayList<ACITuple>();

    static
    {
        Collection<UserClass> name = new ArrayList<UserClass>();
        Collection<UserClass> thisEntry = new ArrayList<UserClass>();
        Collection<UserClass> userGroup = new ArrayList<UserClass>();
        Collection<UserClass> subtree = new ArrayList<UserClass>();
        Collection<UserClass> allUsers = new ArrayList<UserClass>();

        name.add( new UserClass.Name( EMPTY_NAME_SET ) );
        thisEntry.add( UserClass.THIS_ENTRY );
        userGroup.add( new UserClass.UserGroup( EMPTY_NAME_SET ) );
        subtree.add( new UserClass.Subtree( EMPTY_SUBTREE_SPECIFICATION_COLLECTION ) );
        allUsers.add( UserClass.ALL_USERS );

        ACITuple nameTuple = new ACITuple( name, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 );
        ACITuple thisEntryTuple = new ACITuple( thisEntry, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 );
        ACITuple userGroupTuple = new ACITuple( userGroup, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 );
        ACITuple subtreeTuple = new ACITuple( subtree, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 );
        ACITuple allUsersTuple = new ACITuple( allUsers, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, true, 0 );

        TUPLES_A.add( nameTuple );
        TUPLES_A.add( thisEntryTuple );
        TUPLES_A.add( userGroupTuple );
        TUPLES_A.add( subtreeTuple );
        TUPLES_A.add( allUsersTuple );

        TUPLES_B.add( thisEntryTuple );
        TUPLES_B.add( userGroupTuple );
        TUPLES_B.add( subtreeTuple );
        TUPLES_B.add( allUsersTuple );

        TUPLES_C.add( userGroupTuple );
        TUPLES_C.add( subtreeTuple );
        TUPLES_C.add( allUsersTuple );

        TUPLES_D.add( subtreeTuple );
        TUPLES_D.add( allUsersTuple );

        TUPLES_E.add( allUsersTuple );
        TUPLES_E.add( allUsersTuple );
    }


    @Test
    public void testZeroOrOneTuple() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( EMPTY_ACI_TUPLE_COLLECTION );

        assertEquals( 0, filter.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, false, 0 ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        assertEquals( 1, filter.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    @Test
    public void testNameAndThisEntry() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_A );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 2, tuples.size() );
        assertSame( TUPLES_A.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_A.get( 1 ), tuples.get( 1 ) );
    }


    @Test
    public void testThisEntry() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_B );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 1, tuples.size() );
        assertSame( TUPLES_B.get( 0 ), tuples.get( 0 ) );
    }


    @Test
    public void testUserGroup() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_C );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 1, tuples.size() );
        assertSame( TUPLES_C.get( 0 ), tuples.get( 0 ) );
    }


    @Test
    public void testSubtree() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_D );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 1, tuples.size() );
        assertSame( TUPLES_D.get( 0 ), tuples.get( 0 ) );
    }


    @Test
    public void testOthers() throws Exception
    {
        MostSpecificUserClassFilter filter = new MostSpecificUserClassFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_E );
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = (List<ACITuple>)filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 2, tuples.size() );
        assertSame( TUPLES_E.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_E.get( 1 ), tuples.get( 1 ) );
    }
}
