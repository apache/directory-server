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
package org.apache.directory.shared.ldap.filter;


import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * Tests the equals() methods of filter nodes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 575783 $
 */
public class FilterNodeEqualityTest
{
    @Test
    public void testEqualityEquals()
    {
        EqualityNode<String> eqNode1 = new EqualityNode<String>( "attr1", new ClientStringValue( "test" ) );
        EqualityNode<String> eqNode2 = new EqualityNode<String>( "attr1", new ClientStringValue( "test" ) );

        assertEquals( "two exact nodes should be equal", eqNode1, eqNode2 );

        eqNode2 = new EqualityNode<String>( "attr2", new ClientStringValue( "test" ) );
        assertFalse( "different attribute in node should return false on equals()", eqNode1.equals( eqNode2 ) );

        eqNode2 = new EqualityNode<String>( "attr2", new ClientStringValue( "foobar" ) );
        assertFalse( "different value in node should return false on equals()", eqNode1.equals( eqNode2 ) );

        PresenceNode presenceNode = new PresenceNode( "attr1" );
        assertFalse( "two different leaf nodes should not be equal", eqNode1.equals( presenceNode ) );
        assertFalse( "two different leaf nodes should not be equal", presenceNode.equals( eqNode1 ) );

        GreaterEqNode<String> greaterEqNode = new GreaterEqNode<String>( "attr1", new ClientStringValue( "test" ) );
        assertFalse( "two different simple nodes should not be equal", eqNode1.equals( greaterEqNode ) );
        assertFalse( "two different simple nodes should not be equal", greaterEqNode.equals( eqNode1 ) );
    }


    @Test
    public void testGreaterEqEquals()
    {
        GreaterEqNode<String> greaterEqNode1 = new GreaterEqNode<String>( "attr1", new ClientStringValue( "test" ) );
        GreaterEqNode<String> greaterEqNode2 = new GreaterEqNode<String>( "attr1", new ClientStringValue( "test" ) );

        assertEquals( "two exact nodes should be equal", greaterEqNode1, greaterEqNode2 );

        greaterEqNode2 = new GreaterEqNode<String>( "attr2", new ClientStringValue( "test" ) );
        assertFalse( "different attribute in node should return false on equals()", greaterEqNode1
            .equals( greaterEqNode2 ) );

        greaterEqNode2 = new GreaterEqNode<String>( "attr2", new ClientStringValue( "foobar" ) );
        assertFalse( "different value in node should return false on equals()", greaterEqNode1.equals( greaterEqNode2 ) );
    }


    @Test
    public void testLessEqEquals()
    {
        LessEqNode<String> lessEqNode1 = new LessEqNode<String>( "attr1", new ClientStringValue( "test" ) );
        LessEqNode<String> lessEqNode2 = new LessEqNode<String>( "attr1", new ClientStringValue( "test" ) );

        assertEquals( "two exact nodes should be equal", lessEqNode1, lessEqNode2 );

        lessEqNode2 = new LessEqNode<String>( "attr2", new ClientStringValue( "test" ) );
        assertFalse( "different attribute in node should return false on equals()", lessEqNode1.equals( lessEqNode2 ) );

        lessEqNode2 = new LessEqNode<String>( "attr2", new ClientStringValue( "foobar" ) );
        assertFalse( "different value in node should return false on equals()", lessEqNode1.equals( lessEqNode2 ) );
    }


    @Test
    public void testApproximateEqEquals()
    {
        ApproximateNode<String> approximateNode1 = new ApproximateNode<String>( "attr1", new ClientStringValue( "test" ) );
        ApproximateNode<String> approximateNode2 = new ApproximateNode<String>( "attr1", new ClientStringValue( "test" ) );

        assertEquals( "two exact nodes should be equal", approximateNode1, approximateNode2 );

        approximateNode2 = new ApproximateNode<String>( "attr2", new ClientStringValue( "test" ) );
        assertFalse( "different attribute in node should return false on equals()", approximateNode1
            .equals( approximateNode2 ) );

        approximateNode2 = new ApproximateNode<String>( "attr2", new ClientStringValue( "foobar" ) );
        assertFalse( "different value in node should return false on equals()", approximateNode1
            .equals( approximateNode2 ) );
    }


    @Test
    public void testPresenceEquals()
    {
        PresenceNode presenceNode1 = new PresenceNode( "attr1" );
        PresenceNode presenceNode2 = new PresenceNode( "attr1" );

        assertEquals( "two exact presence nodes on same attribute should be equal", presenceNode1, presenceNode2 );

        presenceNode2 = new PresenceNode( "attr2" );
        assertFalse( "presence nodes on different attributes should not be equal", presenceNode1.equals( presenceNode2 ) );
    }


    @Test
    public void testSubstringEquals()
    {
    }
}
