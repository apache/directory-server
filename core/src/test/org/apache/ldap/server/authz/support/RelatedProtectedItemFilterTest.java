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
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.aci.ProtectedItem.MaxValueCountItem;
import org.apache.ldap.common.aci.ProtectedItem.RestrictedByItem;
import org.apache.ldap.common.filter.PresenceNode;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.event.ExpressionEvaluator;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.subtree.RefinementEvaluator;
import org.apache.ldap.server.subtree.RefinementLeafEvaluator;

/**
 * Tests {@link RelatedUserClassFilter}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class RelatedProtectedItemFilterTest extends TestCase
{
    private static final Collection EMPTY_COLLECTION =
        Collections.unmodifiableCollection( new ArrayList() );
    private static final Set EMPTY_SET =
        Collections.unmodifiableSet( new HashSet() );
    
    private static final Name GROUP_NAME;
    private static final Name USER_NAME;
    private static final Set USER_NAMES = new HashSet();
    private static final Set GROUP_NAMES = new HashSet();
    
    private static final AttributeTypeRegistry ATTR_TYPE_REGISTRY_A = new DummyAttributeTypeRegistry( false );
    private static final AttributeTypeRegistry ATTR_TYPE_REGISTRY_B = new DummyAttributeTypeRegistry( true );
    private static final OidRegistry OID_REGISTRY = new DummyOidRegistry();

    private static final RelatedProtectedItemFilter filterA;
    private static final RelatedProtectedItemFilter filterB;

    
    static
    {
        try
        {
            GROUP_NAME = new LdapName( "ou=test,ou=groups,ou=system" );
            USER_NAME = new LdapName( "ou=test, ou=users, ou=system" );
            
            filterA = new RelatedProtectedItemFilter(
                    ATTR_TYPE_REGISTRY_A,
                    new RefinementEvaluator(
                            new RefinementLeafEvaluator( OID_REGISTRY ) ),
                    new ExpressionEvaluator( OID_REGISTRY, ATTR_TYPE_REGISTRY_A ) );

            filterB = new RelatedProtectedItemFilter(
                    ATTR_TYPE_REGISTRY_B,
                    new RefinementEvaluator(
                            new RefinementLeafEvaluator( OID_REGISTRY ) ),
                    new ExpressionEvaluator( OID_REGISTRY, ATTR_TYPE_REGISTRY_B ) );
        }
        catch( NamingException e )
        {
            throw new Error();
        }

        USER_NAMES.add( USER_NAME );
        GROUP_NAMES.add( GROUP_NAME );
    }
    
    public void testZeroTuple() throws Exception
    {
        Assert.assertEquals(
                0, filterA.filter(
                        EMPTY_COLLECTION, OperationScope.ATTRIBUTE_TYPE_AND_VALUE,
                        null, null, null, null, null, null, null, null, null, null ).size() );
    }
    
    public void testEntry() throws Exception
    {
        Collection tuples = getTuples( ProtectedItem.ENTRY );
        
        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, null,
                        null, AuthenticationLevel.NONE, null, null, null, null, null ).size() );
    }
    
    public void testAllUserAttributeTypes() throws Exception
    {
        Collection tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
        
        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "userAttr", null, null, null ).size() );
        
        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "userAttr", null, null, null ).size() );
        
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
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "userAttr", null, null, null ).size() );
        
        tuples = getTuples( ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "userAttr", null, null, null ).size() );
        
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
        Collection attrTypes = new ArrayList();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.AllAttributeValues( attrTypes ) );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        tuples = getTuples( new ProtectedItem.AllAttributeValues( attrTypes ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        Assert.assertEquals(
                0, filterB.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", null, null, null ).size() );  
    }
    
    public void testAttributeType() throws Exception
    {
        Collection attrTypes = new ArrayList();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.AttributeType( attrTypes ) );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        tuples = getTuples( new ProtectedItem.AttributeType( attrTypes ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", null, null, null ).size() );  
    }
    
    public void testAttributeValue() throws Exception
    {
        Collection attributes = new ArrayList();
        attributes.add( new BasicAttribute( "attrA", "valueA" ) );
        Collection tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        
        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", "valueA", null, null ).size() );
        
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", "valueB", null, null ).size() );  

        tuples = getTuples( new ProtectedItem.AttributeValue( attributes ) );

        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", "valueA", null, null ).size() );  
    }
    
    public void testClasses() throws Exception
    {
        // TODO I don't know how to test with Refinement yet.
    }

    public void testMaxImmSub() throws Exception
    {
        Collection tuples = getTuples( new ProtectedItem.MaxImmSub( 2 ) );

        // Should always retain ruples.
        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
    }
    
    public void testMaxValueCount() throws Exception
    {
        Collection mvcItems = new ArrayList();
        mvcItems.add( new MaxValueCountItem( "attrA", 3 ) );
        Collection tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        tuples = getTuples( new ProtectedItem.MaxValueCount( mvcItems ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", null, null, null ).size() );  
    }
    
    public void testRangeOfValues() throws Exception
    {
        Attributes entry = new BasicAttributes();
        entry.put( "attrA", "valueA" );
        Collection tuples = getTuples( new ProtectedItem.RangeOfValues( new PresenceNode( "attrA" ) ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, new LdapName( "ou=testEntry" ),
                        null, null, entry, null ).size() );
        
        entry.remove( "attrA" );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, new LdapName( "ou=testEntry" ),
                        null, null, entry, null ).size() );  
    }
    
    public void testRestrictedBy() throws Exception
    {
        Collection rbItems = new ArrayList();
        rbItems.add( new RestrictedByItem( "attrA", "attrB" ) );
        Collection tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        tuples = getTuples( new ProtectedItem.RestrictedBy( rbItems ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, null, null ).size() );
        
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", null, null, null ).size() );  
    }
    
    public void testSelfValue() throws Exception
    {
        Collection attrTypes = new ArrayList();
        attrTypes.add( "attrA" );
        Collection tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );
        
        Attributes entry = new BasicAttributes();
        entry.put( "attrA", USER_NAME );

        // Test wrong scope
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ENTRY, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, entry, null ).size() );
        
        tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );

        Assert.assertEquals(
                1, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, entry, null ).size() );
        
        entry.remove( "attrA" );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrA", null, entry, null ).size() );  

        tuples = getTuples( new ProtectedItem.SelfValue( attrTypes ) );
        Assert.assertEquals(
                0, filterA.filter(
                        tuples, OperationScope.ATTRIBUTE_TYPE_AND_VALUE, null, null, USER_NAME,
                        null, null, null,
                        "attrB", null, entry, null ).size() );  
    }
    
    private static Collection getTuples( ProtectedItem protectedItem )
    {
        Collection protectedItems = new ArrayList();
        protectedItems.add( protectedItem );
        
        Collection tuples = new ArrayList();
        tuples.add( new ACITuple(
                EMPTY_COLLECTION, AuthenticationLevel.NONE, protectedItems,
                EMPTY_SET, true, 0 ) );
        
        return tuples;
    }
}
