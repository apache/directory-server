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

import org.apache.directory.junit.tools.Concurrent;
import org.apache.directory.junit.tools.ConcurrentJunitRunner;
import org.apache.directory.server.core.event.ExpressionEvaluator;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.server.core.subtree.RefinementLeafEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.aci.protectedItem.AllAttributeValuesItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeTypeItem;
import org.apache.directory.shared.ldap.aci.protectedItem.AttributeValueItem;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxImmSubItem;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxValueCountElem;
import org.apache.directory.shared.ldap.aci.protectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.aci.protectedItem.RestrictedByElem;
import org.apache.directory.shared.ldap.aci.protectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.aci.protectedItem.SelfValueItem;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.DefaultEntry;
import org.apache.directory.shared.ldap.model.entry.*;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.schemaloader.JarLdifSchemaLoader;
import org.apache.directory.shared.ldap.schemamanager.impl.DefaultSchemaManager;
import org.apache.directory.shared.util.exception.Exceptions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;


/**
 * Tests {@link RelatedUserClassFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@RunWith(ConcurrentJunitRunner.class)
@Concurrent()
public class RelatedProtectedItemFilterTest
{
    private static final Collection<UserClass> EMPTY_USER_CLASS_COLLECTION = Collections.unmodifiableCollection( new ArrayList<UserClass>() );
    private static final Collection<ACITuple> EMPTY_ACI_TUPLE_COLLECTION = Collections.unmodifiableCollection( new ArrayList<ACITuple>() );
    private static final Set<MicroOperation> EMPTY_MICRO_OPERATION_SET = Collections.unmodifiableSet( new HashSet<MicroOperation>() );

    private static Dn GROUP_NAME;
    private static Dn USER_NAME;
    private static Set<Dn> USER_NAMES = new HashSet<Dn>();
    private static Set<Dn> GROUP_NAMES = new HashSet<Dn>();

    private static SchemaManager schemaManager;

    private static RelatedProtectedItemFilter filterA;
    private static RelatedProtectedItemFilter filterB;
    
    /** The CN attribute Type */
    private static AttributeType CN_AT;
    
    /** The OU attribute Type */
    private static AttributeType OU_AT;
    
    /** The SN attribute Type */
    private static AttributeType SN_AT;

    
    @BeforeClass 
    public static void setup() throws Exception
    {
        JarLdifSchemaLoader loader = new JarLdifSchemaLoader();

        schemaManager = new DefaultSchemaManager( loader );

        boolean loaded = schemaManager.loadAllEnabled();

        if ( !loaded )
        {
            fail( "Schema load failed : " + Exceptions.printErrors(schemaManager.getErrors()) );
        }

        GROUP_NAME = new Dn( "ou=test,ou=groups,ou=system" );
        USER_NAME = new Dn( "ou=test, ou=users, ou=system" );
        
        filterA = new RelatedProtectedItemFilter( new RefinementEvaluator( new RefinementLeafEvaluator(
            schemaManager ) ), new ExpressionEvaluator( schemaManager ), schemaManager );

        filterB = new RelatedProtectedItemFilter( new RefinementEvaluator( new RefinementLeafEvaluator(
            schemaManager ) ), new ExpressionEvaluator( schemaManager ), schemaManager );

        USER_NAMES.add( USER_NAME );
        GROUP_NAMES.add( GROUP_NAME );
        CN_AT = schemaManager.lookupAttributeTypeRegistry( "cn" );
        OU_AT = schemaManager.lookupAttributeTypeRegistry( "ou" );
        SN_AT = schemaManager.lookupAttributeTypeRegistry( "sn" );
    }

    
    @Test 
    public void testZeroTuple() throws Exception
    {
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( EMPTY_ACI_TUPLE_COLLECTION );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    @Test 
    public void testEntry() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( ProtectedItem.ENTRY );

        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setAuthenticationLevel( AuthenticationLevel.NONE );
        aciContext.setAttributeType( OU_AT );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test 
    public void testAllUserAttributeTypes() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );
    }


    @Test 
    public void testAllUserAttributeTypesAndValues() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );
    }


    @Test 
    public void testAllAttributeValues() throws Exception
    {
        Set<AttributeType> attrTypes = new HashSet<AttributeType>();
        attrTypes.add( CN_AT );
        Collection<ACITuple> tuples = getTuples( new AllAttributeValuesItem( attrTypes ) );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( new AllAttributeValuesItem( attrTypes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );

        assertEquals( 0, filterB.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    @Test 
    public void testAttributeType() throws Exception
    {
        Set<AttributeType> attrTypes = new HashSet<AttributeType>();
        attrTypes.add( CN_AT );
        Collection<ACITuple> tuples = getTuples( new AttributeTypeItem( attrTypes ) );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( new AttributeTypeItem( attrTypes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );
    }


    @Test 
    public void testAttributeValue() throws Exception
    {
        Set<EntryAttribute> attributes = new HashSet<EntryAttribute>();
        attributes.add( new DefaultEntryAttribute( "cn", CN_AT, "valueA" ) );
        Collection<ACITuple> tuples = getTuples( new AttributeValueItem( attributes ) );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );
        tuples = getTuples( new AttributeValueItem( attributes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );

        tuples = getTuples( new AttributeValueItem( attributes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        aciContext.setAttrValue( new StringValue( "valueA" ) );

        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        aciContext.setAttrValue( new StringValue( "valueB" ) );

        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        tuples = getTuples( new AttributeValueItem( attributes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );
        aciContext.setAttrValue( new StringValue( "valueA" ) );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    public void testClasses() throws Exception
    {
        // TODO I don't know how to test with Refinement yet.
    }


    @Test 
    public void testMaxImmSub() throws Exception
    {
        Collection<ACITuple> tuples = getTuples( new MaxImmSubItem( 2 ) );

        // Should always retain tuples.
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 1, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );
    }


    @Test 
    public void testMaxValueCount() throws Exception
    {
        Set<MaxValueCountElem> mvcItems = new HashSet<MaxValueCountElem>();
        mvcItems.add( new MaxValueCountElem( CN_AT, 3 ) );
        Collection<ACITuple> tuples = getTuples( new MaxValueCountItem( mvcItems ) );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );
        tuples = getTuples( new MaxValueCountItem( mvcItems ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );

        tuples = getTuples( new MaxValueCountItem( mvcItems ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    /* this test requires a real registry with real values or the dummy registry
     * needs to be altered to contain some usable mock data.  This is a result of
     * using the registry now in this operation.    
     *
    public void testRangeOfValues() throws Exception
    {
        Entry entry = new DefaultEntry( service.getRegistries(), USER_NAME );
        entry.put( "cn", "valueA" );
        Collection<ACITuple> tuples = getTuples( new ProtectedItem.RangeOfValues( new PresenceNode( "cn" ) ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null,
            DNFactory.create( "ou=testEntry" ), null, null, entry, null ).size() );

        entry.remove( "cn" );
        Assert.assertEquals( 0, filterA.filter( service.getRegistries(), tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, DNFactory.create( "ou=testEntry" ), null, null, entry, null ).size() );
    }
    */


    @Test 
    public void testRestrictedBy() throws Exception
    {
        Set<RestrictedByElem> rbItems = new HashSet<RestrictedByElem>();
        rbItems.add( new RestrictedByElem( CN_AT, SN_AT ) );
        Collection<ACITuple> tuples = getTuples( new RestrictedByItem( rbItems ) );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );
        tuples = getTuples( new RestrictedByItem( rbItems ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE, null ).size() );

        tuples = getTuples( new RestrictedByItem( rbItems ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        
        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    @Test 
    public void testSelfValue() throws Exception
    {
        Set<AttributeType> attrTypes = new HashSet<AttributeType>();
        attrTypes.add( CN_AT );
        Collection<ACITuple> tuples = getTuples( new SelfValueItem( attrTypes ) );

        Entry entry = new DefaultEntry( schemaManager, USER_NAME );
        entry.put( "cn", USER_NAME.getNormName() );

        // Test wrong scope
        AciContext aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        aciContext.setEntry( entry );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ENTRY, null ).size() );

        tuples = getTuples( new SelfValueItem( attrTypes ) );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        aciContext.setEntry( entry );
        
        assertEquals( 1, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        entry.removeAttributes( "cn" );

        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( CN_AT );
        aciContext.setEntry( entry );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );

        tuples = getTuples( new SelfValueItem( attrTypes ) );
        
        aciContext = new AciContext( null, null );
        aciContext.setAciTuples( tuples );
        aciContext.setUserDn( USER_NAME );
        aciContext.setAttributeType( SN_AT );
        aciContext.setEntry( entry );
        
        assertEquals( 0, filterA.filter( aciContext, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null ).size() );
    }


    private static Collection<ACITuple> getTuples( ProtectedItem protectedItem )
    {
        Collection<ProtectedItem> protectedItems = new ArrayList<ProtectedItem>();
        protectedItems.add( protectedItem );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_USER_CLASS_COLLECTION, AuthenticationLevel.NONE, protectedItems, EMPTY_MICRO_OPERATION_SET, true, 0 ) );

        return tuples;
    }
}
