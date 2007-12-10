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
import java.util.List;
import java.util.Set;

import javax.naming.directory.Attribute;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.server.core.authz.support.MostSpecificProtectedItemFilter;
import org.apache.directory.server.core.authz.support.OperationScope;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.filter.PresenceNode;


/**
 * Tests {@link MostSpecificProtectedItemFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MostSpecificProtectedItemFilterTest extends TestCase
{
    private static final Collection<String> EMPTY_STRING_COLLECTION = Collections.unmodifiableCollection( new ArrayList<String>() );
    
    private static final Collection<Attribute> EMPTY_ATTRIBUTE_COLLECTION =
    	Collections.unmodifiableCollection( new ArrayList<Attribute>() );
    
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION =
    	Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION =
    	Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    
    private static final Collection<ProtectedItem> EMPTY_PROTECTED_ITEM_COLLECTION =
    	Collections.unmodifiableCollection( new ArrayList<ProtectedItem>() );
    
    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET =
    	 Collections.unmodifiableSet( new HashSet<MicroOperation>() );
    
    private static final List<ACITuple> TUPLES_A = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_B = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_C = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_D = new ArrayList<ACITuple>();
    private static final List<ACITuple> TUPLES_E = new ArrayList<ACITuple>();

    static
    {
        Collection<ProtectedItem> attributeType = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allAttributeValues = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> selfValue = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> attributeValue = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> rangeOfValues = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allUserAttributeTypes = new ArrayList<ProtectedItem>();
        Collection<ProtectedItem> allUserAttributeTypesAndValues = new ArrayList<ProtectedItem>();

        attributeType.add( new ProtectedItem.AttributeType( EMPTY_STRING_COLLECTION ) );
        allAttributeValues.add( new ProtectedItem.AllAttributeValues( EMPTY_STRING_COLLECTION ) );
        selfValue.add( new ProtectedItem.SelfValue( EMPTY_STRING_COLLECTION ) );
        attributeValue.add( new ProtectedItem.AttributeValue( EMPTY_ATTRIBUTE_COLLECTION ) );
        rangeOfValues.add( new ProtectedItem.RangeOfValues( new PresenceNode( "objectClass" ) ) );
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


    public void testZeroOrOneTuple() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        Assert.assertEquals( 0, filter.filter( EMPTY_ACI_TUPLE_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null,
            null, null, null, null, null, null, null, null ).size() );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, EMPTY_PROTECTED_ITEM_COLLECTION, EMPTY_MICRO_OPERATION_SET, false, 0 ) );

        Assert.assertEquals( 1, filter.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, null, null,
            null, null, null, null, null, null ).size() );
    }


    public void testTuplesA() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_A );
        tuples = ( List<ACITuple> ) filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null, null, null, null,
            null, null );

        Assert.assertEquals( 4, tuples.size() );
        Assert.assertSame( TUPLES_A.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_A.get( 1 ), tuples.get( 1 ) );
        Assert.assertSame( TUPLES_A.get( 2 ), tuples.get( 2 ) );
        Assert.assertSame( TUPLES_A.get( 3 ), tuples.get( 3 ) );
    }


    public void testTuplesB() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_B );
        tuples = ( List<ACITuple> ) filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null, null, null, null,
            null, null );

        Assert.assertEquals( 3, tuples.size() );
        Assert.assertSame( TUPLES_B.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_B.get( 1 ), tuples.get( 1 ) );
        Assert.assertSame( TUPLES_B.get( 2 ), tuples.get( 2 ) );
    }


    public void testTuplesC() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_C );
        tuples = ( List<ACITuple> ) filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null, null, null, null,
            null, null );

        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_C.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_C.get( 1 ), tuples.get( 1 ) );
    }


    public void testTuplesD() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_D );
        tuples = ( List<ACITuple> ) filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null, null, null, null,
            null, null );

        Assert.assertEquals( 1, tuples.size() );
        Assert.assertSame( TUPLES_D.get( 0 ), tuples.get( 0 ) );
    }


    public void testTuplesE() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();

        List<ACITuple> tuples = new ArrayList<ACITuple>( TUPLES_E );
        tuples = ( List<ACITuple> ) filter.filter( tuples, OperationScope.ENTRY, null, null, null, null, null, null, null, null,
            null, null );

        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_E.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_E.get( 1 ), tuples.get( 1 ) );
    }
}
