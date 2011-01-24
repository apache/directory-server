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
import org.apache.directory.shared.ldap.aci.protectedItem.AllAttributeValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeTypeItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeValueItem;
import org.apache.directory.shared.ldap.aci.protectedItem.RangeOfValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.SelfValueItem;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.filter.PresenceNode;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link MostSpecificProtectedItemFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class MostSpecificProtectedItemFilterTest
{
    private static final Set<AttributeType> EMPTY_STRING_COLLECTION = Collections.unmodifiableSet( new HashSet<AttributeType>() );

    private static final Set<EntryAttribute> EMPTY_ATTRIBUTE_COLLECTION = Collections
        .unmodifiableSet( new HashSet<EntryAttribute>() );

    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<UserClass>() );

    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ACITuple>() );

    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION = Collections
        .unmodifiableCollection( new ArrayList<ProtectedItem>() );

    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections
        .unmodifiableSet( new HashSet<MicroOperation>() );
    
    private static final List<ACITuple> TUPLES_A = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_B = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_C = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_D = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_E = new ArrayList<ACITuple>();

    
    @BeforeClass
    public static void init()
    {
        Collection<ProtectedItem> attributeType = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allAttributeValues = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> selfValue = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> attributeValue = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> rangeOfValues = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allUserAttributeTypes = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allUserAttributeTypesAndValues = new ArrayList<ProtectedItem>();

        attributeType.add( new AttributeTypeItem( EMPTY_STRING_COLLECTION ) );
        allAttributeValues.add( new AllAttributeValuesItem( EMPTY_STRING_COLLECTION ) );
        selfValue.add( new SelfValueItem( EMPTY_STRING_COLLECTION ) );
        attributeValue.add( new AttributeValueItem( EMPTY_ATTRIBUTE_COLLECTION ) );
        rangeOfValues.add( new RangeOfValuesItem( new PresenceNode( (String)null ) ) );
        allUserAttributeTypes.add( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
        allUserAttributeTypesAndValues.add( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        ACITuple attributeTypeTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, attributeType,
            EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple allAttributeValuesTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE,
            allAttributeValues, EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple selfValueTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, selfValue, 
                EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple attributeValueTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, attributeValue,
                EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple rangeOfValuesTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, rangeOfValues,
                EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple allUserAttributeTypesTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE,
            allUserAttributeTypes, EMPTY_MICRO_OPERATION_SET, true, 0 );
        
        ACITuple allUserAttributeTypesAndValuesTuple = new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE,
            allUserAttributeTypesAndValues, EMPTY_MICRO_OPERATION_SET, true, 0 );

        TUPLES_A.add( attributeTypeTuple );
        TUPLES_A.add( allAttributeValuesTuple );
        TUPLES_A.add( selfValueTuple );
        TUPLES_A.add( attributeValueTuple );
        TUPLES_A.add( rangeOfValuesTuple );
        TUPLES_A.add( allUserAttributeTypesTuple );
        TUPLES_A.add( allUserAttributeTypesAndValuesTuple );

        TUPLES_B.add( allAttributeValuesTuple );
        TUPLES_B.add( selfValueTuple );
        TUPLES_B.add( attributeValueTuple );
        TUPLES_B.add( rangeOfValuesTuple );
        TUPLES_B.add( allUserAttributeTypesTuple );
        TUPLES_B.add( allUserAttributeTypesAndValuesTuple );

        TUPLES_C.add( selfValueTuple );
        TUPLES_C.add( attributeValueTuple );
        TUPLES_C.add( rangeOfValuesTuple );
        TUPLES_C.add( allUserAttributeTypesTuple );
        TUPLES_C.add( allUserAttributeTypesAndValuesTuple );

        TUPLES_D.add( attributeValueTuple );
        TUPLES_D.add( rangeOfValuesTuple );
        TUPLES_D.add( allUserAttributeTypesTuple );
        TUPLES_D.add( allUserAttributeTypesAndValuesTuple );

        TUPLES_E.add( allUserAttributeTypesTuple );
        TUPLES_E.add( allUserAttributeTypesAndValuesTuple );
    }

    @Test
    public void testZeroOrOneTuple() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

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
    public void testTuplesA() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_A );
        
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter(  aciContext, OperationScope.ENTRY, null );

        assertEquals( 4, tuples.size() );
        assertSame( TUPLES_A.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_A.get( 1 ), tuples.get( 1 ) );
        assertSame( TUPLES_A.get( 2 ), tuples.get( 2 ) );
        assertSame( TUPLES_A.get( 3 ), tuples.get( 3 ) );
    }


    @Test
    public void testTuplesB() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_B );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 3, tuples.size() );
        assertSame( TUPLES_B.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_B.get( 1 ), tuples.get( 1 ) );
        assertSame( TUPLES_B.get( 2 ), tuples.get( 2 ) );
    }


    @Test
    public void testTuplesC() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_C );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 2, tuples.size() );
        assertSame( TUPLES_C.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_C.get( 1 ), tuples.get( 1 ) );
    }


    @Test
    public void testTuplesD() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_D );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 1, tuples.size() );
        assertSame( TUPLES_D.get( 0 ), tuples.get( 0 ) );
    }


    @Test
    public void testTuplesE() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_E );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );

        tuples = ( List<ACITuple> ) filter.filter( aciContext, OperationScope.ENTRY, null );

        assertEquals( 2, tuples.size() );
        assertSame( TUPLES_E.get( 0 ), tuples.get( 0 ) );
        assertSame( TUPLES_E.get( 1 ), tuples.get( 1 ) );
    }
}
