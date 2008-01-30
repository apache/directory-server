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
package org.apache.directory.server.core.entry;

import java.util.HashSet;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.InvalidAttributeValueException;

import org.apache.directory.shared.ldap.schema.AttributeType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Tests for the DefaultServerAttribute class. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultServerAttributeTest
{
    @Test public void testAddOneValue() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add a String value
        attr.add( "test" );
        
        assertEquals( 1, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        ServerValue<?> value = attr.get();
        
        assertTrue( value instanceof ServerStringValue );
        assertEquals( "test", ((ServerStringValue)value).get() );
        
        // Add a binary value
        try
        {
            attr.add( new byte[]{0x01} );
            fail();
        }
        catch ( InvalidAttributeValueException iave )
        {
            assertTrue( true );
        }
        
        // Add a ServerValue
        ServerValue<?> ssv = new ServerStringValue( at, "test2" );
        
        attr.add( ssv );
        
        assertEquals( 2, attr.size() );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );
        
        for ( ServerValue<?> val:attr )
        {
            if ( expected.contains( val.get() ) )
            {
                expected.remove( val.get() );
            }
            else
            {
                fail();
            }
        }
        
        assertEquals( 0, expected.size() );
    }


    @Test public void testAddTwoValue() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add String values
        attr.add( "test" );
        attr.add( "test2" );
        
        assertEquals( 2, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        Set<String> expected = new HashSet<String>();
        expected.add( "test" );
        expected.add( "test2" );
        
        for ( ServerValue<?> val:attr )
        {
            if ( expected.contains( val.get() ) )
            {
                expected.remove( val.get() );
            }
            else
            {
                fail();
            }
        }
        
        assertEquals( 0, expected.size() );
    }


    @Test public void testAddNullValue() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        // Add a null value
        attr.add( new ServerStringValue( at, null ) );
        
        assertEquals( 1, attr.size() );
        
        assertTrue( attr.getType().getSyntax().isHumanReadable() );
        
        ServerValue<?> value = attr.get();
        
        assertTrue( value instanceof ServerStringValue );
        assertNull( ((ServerStringValue)value).get() );
    }
    
    @Test public void testGetAttribute() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        attr.add( "Test1" );
        attr.add( "Test2" );
        attr.add( "Test3" );
        
        Attribute attribute = ServerEntryUtils.toBasicAttribute( attr );
        
        assertEquals( "1.1",attribute.getID() );
        assertEquals( 3, attribute.size() );
        assertTrue( attribute.contains( "Test1" ) );
        assertTrue( attribute.contains( "Test2" ) );
        assertTrue( attribute.contains( "Test3" ) );
    }


    /**
     * Test the contains() method
     */
    @Test public void testContains() throws NamingException
    {
        AttributeType at = TestServerEntryUtils.getIA5StringAttributeType();
        
        DefaultServerAttribute attr = new DefaultServerAttribute( at );
        
        attr.add( "Test  1" );
        attr.add( "Test  2" );
        attr.add( "Test  3" );
        
        assertTrue( attr.contains( "test 1" ) );
        assertTrue( attr.contains( "Test 2" ) );
        assertTrue( attr.contains( "TEST     3" ) );
    }
}
