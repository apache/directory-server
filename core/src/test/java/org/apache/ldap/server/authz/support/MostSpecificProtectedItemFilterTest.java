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
import java.util.List;
import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.filter.PresenceNode;

/**
 * Tests {@link MostSpecificProtectedItemFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MostSpecificProtectedItemFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION =
        Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET =
        Collections.unmodifiableSet( new HashSet() );
    
    private static final List TUPLES_A = new ArrayList();
    private static final List TUPLES_B = new ArrayList();
    private static final List TUPLES_C = new ArrayList();
    private static final List TUPLES_D = new ArrayList();
    private static final List TUPLES_E = new ArrayList();
    
    static
    {
        Collection attributeType = new ArrayList();
        Collection allAttributeValues = new ArrayList();
        Collection selfValue = new ArrayList();
        Collection attributeValue = new ArrayList();
        Collection rangeOfValues = new ArrayList();
        Collection allUserAttributeTypes = new ArrayList();
        Collection allUserAttributeTypesAndValues = new ArrayList();

        attributeType.add( new ProtectedItem.AttributeType( EMPTY_COLLECTION ) );
        allAttributeValues.add( new ProtectedItem.AllAttributeValues( EMPTY_COLLECTION ) );
        selfValue.add( new ProtectedItem.SelfValue( EMPTY_COLLECTION ) );
        attributeValue.add( new ProtectedItem.AttributeValue( EMPTY_COLLECTION ) );
        rangeOfValues.add( new ProtectedItem.RangeOfValues( new PresenceNode( "objectClass" ) ) );
        allUserAttributeTypes.add( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
        allUserAttributeTypesAndValues.add( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        ACITuple attributeTypeTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, attributeType,
                EMPTY_SET, true, 0 );
        ACITuple allAttributeValuesTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, allAttributeValues,
                EMPTY_SET, true, 0 );
        ACITuple selfValueTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, selfValue,
                EMPTY_SET, true, 0 );
        ACITuple attributeValueTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, attributeValue,
                EMPTY_SET, true, 0 );
        ACITuple rangeOfValuesTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, rangeOfValues,
                EMPTY_SET, true, 0 );
        ACITuple allUserAttributeTypesTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, allUserAttributeTypes,
                EMPTY_SET, true, 0 );
        ACITuple allUserAttributeTypesAndValuesTuple = new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, allUserAttributeTypesAndValues,
                EMPTY_SET, true, 0 );

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

        Assert.assertEquals(
                0, filter.filter(
                        EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE,
                        null, null, null, null, null, null, null, null, null, null ).size() );

        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, EMPTY_COLLECTION,
                EMPTY_SET, false, 0 ) );
        
        Assert.assertEquals(
                1, filter.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE,
                        null, null, null, null, null, null, null, null, null, null ).size() );
    }
    
    public void testTuplesA() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();
        
        List tuples = new ArrayList( TUPLES_A );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 4, tuples.size() );
        Assert.assertSame( TUPLES_A.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_A.get( 1 ), tuples.get( 1 ) );
        Assert.assertSame( TUPLES_A.get( 2 ), tuples.get( 2 ) );
        Assert.assertSame( TUPLES_A.get( 3 ), tuples.get( 3 ) );
    }

    public void testTuplesB() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();
        
        List tuples = new ArrayList( TUPLES_B );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 3, tuples.size() );
        Assert.assertSame( TUPLES_B.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_B.get( 1 ), tuples.get( 1 ) );
        Assert.assertSame( TUPLES_B.get( 2 ), tuples.get( 2 ) );
    }

    public void testTuplesC() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();
        
        List tuples = new ArrayList( TUPLES_C );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_C.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_C.get( 1 ), tuples.get( 1 ) );
    }

    public void testTuplesD() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();
        
        List tuples = new ArrayList( TUPLES_D );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 1, tuples.size() );
        Assert.assertSame( TUPLES_D.get( 0 ), tuples.get( 0 ) );
    }
    
    public void testTuplesE() throws Exception
    {
        MostSpecificProtectedItemFilter filter = new MostSpecificProtectedItemFilter();
        
        List tuples = new ArrayList( TUPLES_E );
        tuples = ( List ) filter.filter(
                tuples, OperationScope.ENTRY, null, null, null,
                null, null, null, null, null, null, null );
        
        Assert.assertEquals( 2, tuples.size() );
        Assert.assertSame( TUPLES_E.get( 0 ), tuples.get( 0 ) );
        Assert.assertSame( TUPLES_E.get( 1 ), tuples.get( 1 ) );
    }
}
