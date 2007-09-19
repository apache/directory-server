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


import junit.framework.TestCase;


/**
 * Tests the equals() methods of filter nodes.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 575783 $
 */
public class FilterNodeEqualityTest extends TestCase 
{
	public void testEqualityEquals()
	{
		EqualityNode eqNode1 = new EqualityNode( "attr1", "test" ); 
		EqualityNode eqNode2 = new EqualityNode( "attr1", "test" );
		
		assertEquals( "two exact nodes should be equal", eqNode1, eqNode2 );
		
		eqNode2 = new EqualityNode( "attr2", "test" );
		assertFalse( "different attribute in node should return false on equals()", 
				eqNode1.equals( eqNode2 ) );
		
		eqNode2 = new EqualityNode( "attr2", "foobar" );
		assertFalse( "different value in node should return false on equals()", 
				eqNode1.equals( eqNode2 ) );
		
		PresenceNode presenceNode = new PresenceNode( "attr1" );
		assertFalse( "two different leaf nodes should not be equal", eqNode1.equals( presenceNode ) );
		assertFalse( "two different leaf nodes should not be equal", presenceNode.equals( eqNode1 ) );
		
		GreaterEqNode greaterEqNode = new GreaterEqNode ( "attr1", "test" );
		assertFalse( "two different simple nodes should not be equal", eqNode1.equals( greaterEqNode ) );
		assertFalse( "two different simple nodes should not be equal", greaterEqNode.equals( eqNode1 ) );
	}


	public void testGreaterEqEquals()
	{
		GreaterEqNode greaterEqNode1 = new GreaterEqNode( "attr1", "test" ); 
		GreaterEqNode greaterEqNode2 = new GreaterEqNode( "attr1", "test" );
		
		assertEquals( "two exact nodes should be equal", greaterEqNode1, greaterEqNode2 );
		
		greaterEqNode2 = new GreaterEqNode( "attr2", "test" );
		assertFalse( "different attribute in node should return false on equals()", 
				greaterEqNode1.equals( greaterEqNode2 ) );
		
		greaterEqNode2 = new GreaterEqNode( "attr2", "foobar" );
		assertFalse( "different value in node should return false on equals()", 
				greaterEqNode1.equals( greaterEqNode2 ) );
	}


	public void testLessEqEquals()
	{
		LessEqNode lessEqNode1 = new LessEqNode( "attr1", "test" ); 
		LessEqNode lessEqNode2 = new LessEqNode( "attr1", "test" );
		
		assertEquals( "two exact nodes should be equal", lessEqNode1, lessEqNode2 );
		
		lessEqNode2 = new LessEqNode( "attr2", "test" );
		assertFalse( "different attribute in node should return false on equals()", 
				lessEqNode1.equals( lessEqNode2 ) );
		
		lessEqNode2 = new LessEqNode( "attr2", "foobar" );
		assertFalse( "different value in node should return false on equals()", 
				lessEqNode1.equals( lessEqNode2 ) );
	}


	public void testApproximateEqEquals()
	{
		ApproximateNode approximateNode1 = new ApproximateNode( "attr1", "test" ); 
		ApproximateNode approximateNode2 = new ApproximateNode( "attr1", "test" );
		
		assertEquals( "two exact nodes should be equal", approximateNode1, approximateNode2 );
		
		approximateNode2 = new ApproximateNode( "attr2", "test" );
		assertFalse( "different attribute in node should return false on equals()", 
				approximateNode1.equals( approximateNode2 ) );
		
		approximateNode2 = new ApproximateNode( "attr2", "foobar" );
		assertFalse( "different value in node should return false on equals()", 
				approximateNode1.equals( approximateNode2 ) );
	}
	
	
	public void testPresenceEquals()
	{
		PresenceNode presenceNode1 = new PresenceNode( "attr1" );
		PresenceNode presenceNode2 = new PresenceNode( "attr1" );
		
		assertEquals( "two exact presence nodes on same attribute should be equal", 
				presenceNode1, presenceNode2 );
		
		presenceNode2 = new PresenceNode( "attr2" );
		assertFalse( "presence nodes on different attributes should not be equal", 
				presenceNode1.equals( presenceNode2 ) );
	}
	
	
	public void testSubstringEquals()
	{
	}
}
