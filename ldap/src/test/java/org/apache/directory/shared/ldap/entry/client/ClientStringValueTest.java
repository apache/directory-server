/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.DeepTrimToLowerNormalizer;
import org.apache.directory.shared.ldap.schema.syntax.Ia5StringSyntaxChecker;
import org.apache.directory.shared.ldap.schema.syntax.OctetStringSyntaxChecker;
import org.junit.Test;

/**
 * 
 * Test the ClientStringValue class
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ClientStringValueTest
{
    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#hashCode()}.
     */
    @Test
    public void testHashCode()
    {
        ClientStringValue csv = new ClientStringValue( "test" );
        
        int hash = "test".hashCode();
        assertEquals( hash, csv.hashCode() );
        
        csv = new ClientStringValue();
        hash = "".hashCode();
        assertEquals( hash, csv.hashCode() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#ClientStringValue()}.
     */
    @Test
    public void testClientStringValueNull() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertNull( csv.get() );
        assertFalse( csv.isNormalized() );
        assertFalse( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertTrue( csv.isNull() );
        assertNull( csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#ClientStringValue(java.lang.String)}.
     */
    @Test
    public void testClientStringValueEmpty() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue( "" );
        
        assertNotNull( csv.get() );
        assertEquals( "", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new OctetStringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
        assertNotNull( csv.getNormalizedValue() );
        assertEquals( "", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#ClientStringValue(java.lang.String)}.
     */
    @Test
    public void testClientStringValueString() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue( "test" );
        
        assertEquals( "test", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
        assertNotNull( csv.getNormalizedValue() );
        assertEquals( "test", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#get()}.
     */
    @Test
    public void testGet()
    {
        ClientStringValue csv = new ClientStringValue( "test" );
        
        assertEquals( "test", csv.get() );
        
        csv.set( "" );
        assertEquals( "", csv.get() );
        
        csv.clear();
        assertNull( csv.get() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#getCopy()}.
     */
    @Test
    public void testGetCopy()
    {
        ClientStringValue csv = new ClientStringValue( "test" );
        
        assertEquals( "test", csv.getCopy() );
        
        csv.set( "" );
        assertEquals( "", csv.getCopy() );
        
        csv.clear();
        assertNull( csv.getCopy() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#set(java.lang.String)}.
     */
    @Test
    public void testSet() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        csv.set( null );
        assertNull( csv.get() );
        assertFalse( csv.isNormalized() );
        assertFalse( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertTrue( csv.isNull() );

        csv.set( "" );
        assertNotNull( csv.get() );
        assertEquals( "", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new OctetStringSyntaxChecker() ) );
        assertFalse( csv.isNull() );

        csv.set( "Test" );
        assertNotNull( csv.get() );
        assertEquals( "Test", csv.get() );
        assertFalse( csv.isNormalized() );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNull() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#isNull()}.
     */
    @Test
    public void testIsNull()
    {
        ClientStringValue csv = new ClientStringValue();
        
        csv.set( null );
        assertTrue( csv.isNull() );
        
        csv.set( "test" );
        assertFalse( csv.isNull() );
        
        csv.clear();
        assertTrue( csv.isNull() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#clear()}.
     */
    @Test
    public void testClear() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        csv.clear();
        assertTrue( csv.isNull() );
        
        csv.set( "test" );
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        csv.clear();
        assertTrue( csv.isNull() );
        assertFalse( csv.isValid( new Ia5StringSyntaxChecker() ) );
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#isNormalized()}.
     */
    @Test
    public void testIsNormalized() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertFalse( csv.isNormalized() );
        
        csv.set(  "  This is    a   TEST  " );
        assertFalse( csv.isNormalized() );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertTrue( csv.isNormalized() );
        
        csv.set( "test" );
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#setNormalized(boolean)}.
     */
    @Test
    public void testSetNormalized() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertFalse( csv.isNormalized() );
        
        csv.setNormalized( true );
        assertTrue( csv.isNormalized() );
        
        csv.set(  "  This is    a   TEST  " );
        assertFalse( csv.isNormalized() );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertTrue( csv.isNormalized() );
        
        csv.setNormalized( false );
        assertEquals( "this is a test", csv.getNormalizedValue() );
        assertFalse( csv.isNormalized() );

        csv.normalize( new DeepTrimToLowerNormalizer() );
        csv.clear();
        assertFalse( csv.isNormalized() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#getNormalizedValue()}.
     */
    @Test
    public void testGetNormalizedValue() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertEquals( null, csv.getNormalizedValue() );
        
        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValue() );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );

        csv.clear();
        assertFalse( csv.isNormalized() );
        assertEquals( null, csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#getNormalizedValueCopy()}.
     */
    @Test
    public void getNormalizedValueCopy() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertEquals( null, csv.getNormalizedValueCopy() );
        
        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValueCopy() );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertEquals( "this is a test", csv.getNormalizedValueCopy() );

        csv.clear();
        assertFalse( csv.isNormalized() );
        assertEquals( null, csv.getNormalizedValueCopy() );
    }

    
    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#normalize(org.apache.directory.shared.ldap.schema.Normalizer)}.
     */
    @Test
    public void testNormalize() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();

        csv.normalize( new DeepTrimToLowerNormalizer() );
        assertEquals( null, csv.getNormalizedValue() );
        
        csv.set( "" );
        csv.normalize( new DeepTrimToLowerNormalizer() );
        assertEquals( "", csv.getNormalizedValue() );

        csv.set(  "  This is    a   TEST  " );
        assertEquals( "  This is    a   TEST  ", csv.getNormalizedValue() );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertEquals( "this is a test", csv.getNormalizedValue() );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#isValid(org.apache.directory.shared.ldap.schema.syntax.SyntaxChecker)}.
     */
    @Test
    public void testIsValid() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue( "Test" );
        
        assertTrue( csv.isValid( new Ia5StringSyntaxChecker() ) );
        
        csv.set(  "é" );
        assertFalse( csv.isValid( new Ia5StringSyntaxChecker() ) );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#compareTo(org.apache.directory.shared.ldap.entry.Value)}.
     */
    @Test
    public void testCompareTo() throws NamingException
    {
        ClientStringValue csv1 = new ClientStringValue();
        ClientStringValue csv2 = new ClientStringValue();
        
        assertEquals( 0, csv1.compareTo( csv2 ) );
        
        csv1.set( "Test" );
        assertEquals( 1, csv1.compareTo( csv2 ) );
        assertEquals( -1, csv2.compareTo( csv1 ) );
        
        csv2.set( "Test" );
        assertEquals( 0, csv1.compareTo( csv2 ) );

        // Now check that the equals method works on normalized values.
        csv1.set(  "  This is    a TEST   " );
        csv2.set( "this is a test" );
        csv1.normalize( new DeepTrimToLowerNormalizer() );
        assertEquals( 0, csv1.compareTo( csv2 ) );
        
        csv1.set( "a" );
        csv2.set( "b" );
        assertEquals( -1, csv1.compareTo( csv2 ) );

        csv1.set( "b" );
        csv2.set( "a" );
        assertEquals( 1, csv1.compareTo( csv2 ) );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#clone()}.
     */
    @Test
    public void testClone() throws NamingException
    {
        ClientStringValue csv = new ClientStringValue();
        
        ClientStringValue csv1 = (ClientStringValue)csv.clone();
        
        assertEquals( csv, csv1 );
        
        csv.set( "" );
        
        assertNotSame( csv, csv1 );
        assertNull( csv1.get() );
        assertEquals( "", csv.get() );
        
        csv.set(  "  This is    a   TEST  " );
        csv1 = (ClientStringValue)csv.clone();
        
        assertEquals( csv, csv1 );
        
        csv.normalize( new DeepTrimToLowerNormalizer() );
        
        assertNotSame( csv, csv1 );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#equals(java.lang.Object)}.
     */
    @Test
    public void testEquals() throws NamingException
    {
        ClientStringValue csv1 = new ClientStringValue();
        ClientStringValue csv2 = new ClientStringValue();
        
        assertEquals( csv1, csv2 );
        
        csv1.set( "Test" );
        assertNotSame( csv1, csv2 );
        
        csv2.set( "Test" );
        assertEquals( csv1, csv2 );

        // Now check that the equals method works on normalized values.
        csv1.set(  "  This is    a TEST   " );
        csv2.set( "this is a test" );
        csv1.normalize( new DeepTrimToLowerNormalizer() );
        assertEquals( csv1, csv2 );
    }


    /**
     * Test method for {@link org.apache.directory.shared.ldap.entry.client.ClientStringValue#toString()}.
     */
    @Test
    public void testToString()
    {
        ClientStringValue csv = new ClientStringValue();
        
        assertEquals( "null", csv.toString() );

        csv.set( "" );
        assertEquals( "", csv.toString() );

        csv.set( "Test" );
        assertEquals( "Test", csv.toString() );
        
        csv.clear();
        assertEquals( "null", csv.toString() );
    }
}
