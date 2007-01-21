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
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.directory.server.core.authz.support.OperationScope;
import org.apache.directory.server.core.authz.support.RelatedProtectedItemFilter;
import org.apache.directory.server.core.authz.support.RelatedUserClassFilter;
import org.apache.directory.server.core.event.ExpressionEvaluator;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.server.core.subtree.RefinementLeafEvaluator;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.server.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Tests {@link RelatedUserClassFilter}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RelatedProtectedItemFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION = Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET = Collections.unmodifiableSet( new HashSet() );

    private static final LdapDN GROUP_NAME;
    private static final LdapDN USER_NAME;
    private static final Set<LdapDN> USER_NAMES = new HashSet<LdapDN>();
    private static final Set<LdapDN> GROUP_NAMES = new HashSet<LdapDN>();

    private static final AttributeTypeRegistry ATTR_TYPE_REGISTRY_A = new DummyAttributeTypeRegistry( false );
    private static final AttributeTypeRegistry ATTR_TYPE_REGISTRY_B = new DummyAttributeTypeRegistry( true );
    private static final OidRegistry OID_REGISTRY = new DummyOidRegistry();

    private static final RelatedProtectedItemFilter filterA;
    private static final RelatedProtectedItemFilter filterB;

    static
    {
        try
        {
            GROUP_NAME = new LdapDN( "ou=test,ou=groups,ou=system" );
            USER_NAME = new LdapDN( "ou=test, ou=users, ou=system" );

            filterA = new RelatedProtectedItemFilter( new RefinementEvaluator( new RefinementLeafEvaluator(
                OID_REGISTRY ) ), new ExpressionEvaluator( OID_REGISTRY, ATTR_TYPE_REGISTRY_A ), OID_REGISTRY, ATTR_TYPE_REGISTRY_A );

            filterB = new RelatedProtectedItemFilter( new RefinementEvaluator( new RefinementLeafEvaluator(
                OID_REGISTRY ) ), new ExpressionEvaluator( OID_REGISTRY, ATTR_TYPE_REGISTRY_B ), OID_REGISTRY, ATTR_TYPE_REGISTRY_B );
        }
        catch ( NamingException e )
        {
            throw new Error();
        }

        USER_NAMES.add( USER_NAME );
        GROUP_NAMES.add( GROUP_NAME );
    }


    public void testZeroTuple() throws Exception
    {
        Assert.assertEquals( 0, filterA.filter( EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null,
            null, null, null, null, null, null, null, null ).size() );
    }


    public void testEntry() throws Exception
    {
        Collection tuples = getTuples( ProtectedItem.ENTRY );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ENTRY, null, null, null, null,
            AuthenticationLevel.NONE, null, "ou", null, null, null ).size() );
    }


    public void testAllUserAttributeTypes() throws Exception
    {
        Collection tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "userAttr", null, null, null ).size() );

        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "userAttr", null, null, null ).size() );

        /* Not used anymore
         Assert.assertEquals(
         0, filterB.filter(
         tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
         null, null, null,
         "opAttr", null, null, null ).size() );
         */
    }


    public void testAllUserAttributeTypesAndValues() throws Exception
    {
        Collection tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "userAttr", null, null, null ).size() );

        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "userAttr", null, null, null ).size() );

        /* Not used anymore
         Assert.assertEquals(
         0, filterB.filter(
         tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
         null, null, null,
         "opAttr", null, null, null ).size() );
         */
    }


    public void testAllAttributeValues() throws Exception
    {
        Collection<String> attrTypes = new ArrayList<String>();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.AllAttributeValues( attrTypes ) );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );

        tuples = getTuples( new ProtectedItem.AllAttributeValues( attrTypes ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrA", null, null, null ).size() );

        Assert.assertEquals( 0, filterB.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrB", null, null, null ).size() );
    }


    public void testAttributeType() throws Exception
    {
        Collection<String> attrTypes = new ArrayList<String>();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.AttributeType( attrTypes ) );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );

        tuples = getTuples( new ProtectedItem.AttributeType( attrTypes ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrA", null, null, null ).size() );

        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrB", null, null, null ).size() );
    }


    public void testAttributeValue() throws Exception
    {
        Collection<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add( new AttributeImpl( "attrA", "valueA" ) );
        Collection tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrA", null, null, null ).size() );

        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", "valueA", null, null ).size() );

        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", "valueB", null, null ).size() );

        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrB", "valueA", null, null ).size() );
    }


    public void testClasses() throws Exception
    {
        // TODO I don't know how to test with Refinement yet.
    }


    public void testMaxImmSub() throws Exception
    {
        Collection tuples = getTuples( new ProtectedItem.MaxImmSub( 2 ) );

        // Should always retain ruples.
        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );
    }


    public void testMaxValueCount() throws Exception
    {
        Collection<MaxValueCountItem> mvcItems = new ArrayList<MaxValueCountItem>();
        mvcItems.add( new MaxValueCountItem( "attrA", 3 ) );
        Collection tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrA", null, null, null ).size() );

        tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", null, null, null ).size() );

        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrB", null, null, null ).size() );
    }


    /* this test requires a real registry with real values or the dummy registry
     * needs to be altered to contain some usable mock data.  This is a result of
     * using the registry now in this operation.    
    public void testRangeOfValues() throws Exception
    {
        Attributes entry = new AttributesImpl( true );
        entry.put( "attrA", "valueA" );
        Collection tuples = getTuples( new ProtectedItem.RangeOfValues( new PresenceNode( "attrA" ) ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null,
            new LdapDN( "ou=testEntry" ), null, null, entry, null ).size() );

        entry.remove( "attrA" );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, new LdapDN( "ou=testEntry" ), null, null, entry, null ).size() );
    }
    */


    public void testRestrictedBy() throws Exception
    {
        Collection<RestrictedByItem> rbItems = new ArrayList<RestrictedByItem>();
        rbItems.add( new RestrictedByItem( "attrA", "attrB" ) );
        Collection tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME, null,
            null, null, "attrA", null, null, null ).size() );

        tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", null, null, null ).size() );

        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrB", null, null, null ).size() );
    }


    public void testSelfValue() throws Exception
    {
        Collection<String> attrTypes = new ArrayList<String>();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );

        Attributes entry = new AttributesImpl();
        entry.put( "attrA", USER_NAME.toNormName() );

        // Test wrong scope
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ENTRY, null, null, USER_NAME, null, null, null,
            "attrA", null, entry, null ).size() );

        tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );

        Assert.assertEquals( 1, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", null, entry, null ).size() );

        entry.remove( "attrA" );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrA", null, entry, null ).size() );

        tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );
        Assert.assertEquals( 0, filterA.filter( tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
            null, null, null, "attrB", null, entry, null ).size() );
    }


    private static Collection getTuples( ProtectedItem protectedItem )
    {
        Collection<ProtectedItem> protectedItems = new ArrayList<ProtectedItem>();
        protectedItems.add( protectedItem );

        Collection<ACITuple> tuples = new ArrayList<ACITuple>();
        tuples.add( new ACITuple( EMPTY_COLLECTION, AuthenticationLevel.NONE, protectedItems, EMPTY_SET, true, 0 ) );

        return tuples;
    }
}
