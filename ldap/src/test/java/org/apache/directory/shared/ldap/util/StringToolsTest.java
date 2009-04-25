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
package org.apache.directory.shared.ldap.util;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.util.StringTools;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


/**
 * Tests the StringTools class methods.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class StringToolsTest
{
    @Test
    public void testDecodeEscapedHex() throws Exception
    {
        assertEquals( "Ferry", StringTools.decodeEscapedHex( "\\46\\65\\72\\72\\79" ) );
        assertEquals( "Ferry", StringTools.decodeEscapedHex( "Fe\\72\\72\\79" ) );
        assertEquals( "Ferry", StringTools.decodeEscapedHex( "Fe\\72\\72y" ) );
        assertEquals( "Ferry", StringTools.decodeEscapedHex( "Fe\\72ry" ) );

        assertEquals( "<DC", StringTools.decodeEscapedHex( "\\<DC" ) );
        assertEquals( ">DC", StringTools.decodeEscapedHex( "\\>DC" ) );
        assertEquals( "\"DC", StringTools.decodeEscapedHex( "\\\"DC" ) );
        assertEquals( "+DC", StringTools.decodeEscapedHex( "\\+DC" ) );
        assertEquals( ",DC", StringTools.decodeEscapedHex( "\\,DC" ) );
        assertEquals( ";DC", StringTools.decodeEscapedHex( "\\;DC" ) );
        assertEquals( "=DC", StringTools.decodeEscapedHex( "\\=DC" ) );
        assertEquals( " DC", StringTools.decodeEscapedHex( "\\ DC" ) );
        assertEquals( "#DC", StringTools.decodeEscapedHex( "\\#DC" ) );

        // test a corner case: ESC ESC HEX HEX
        assertEquals( "\\DC", StringTools.decodeEscapedHex( "\\5CDC" ) );
        assertEquals( "\\DC", StringTools.decodeEscapedHex( "\\\\DC" ) );
    }
    
    @Test
    public void testDecodeHexString() throws Exception
    {
        // weird stuff - corner cases
        try{assertEquals( "", StringTools.decodeHexString( "" ) ); fail("should not get here");} catch( NamingException e ){}
        assertEquals( "", StringTools.decodeHexString( "#" ) );
        assertEquals( "F", StringTools.decodeHexString( "#46" ) );
        try{assertEquals( "F", StringTools.decodeHexString( "46" ) ); fail("should not get here");} catch( NamingException e ){}

        assertEquals( "Ferry", StringTools.decodeHexString( "#4665727279" ) );
    }
    
    
    @Test
    public void testTrimConsecutiveToOne()
    {
        String input = null;
        String result = null;

        input = "akarasulu**";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "akarasulu*", result );

        input = "*****akarasulu**";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "*akarasulu*", result );

        input = "**akarasulu";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "*akarasulu", result );

        input = "**akar****asulu**";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "*akar*asulu*", result );

        input = "akarasulu";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "akarasulu", result );

        input = "*a*k*a*r*a*s*u*l*u*";
        result = StringTools.trimConsecutiveToOne( input, '*' );
        assertEquals( "*a*k*a*r*a*s*u*l*u*", result );

    }


    @Test
    public void testOneByteChar()
    {
        char res = StringTools.bytesToChar( new byte[]
            { 0x30 } );

        assertEquals( '0', res );
    }


    @Test
    public void testOneByteChar00()
    {
        char res = StringTools.bytesToChar( new byte[]
            { 0x00 } );

        assertEquals( 0x00, res );
    }


    @Test
    public void testOneByteChar7F()
    {
        char res = StringTools.bytesToChar( new byte[]
            { 0x7F } );

        assertEquals( 0x7F, res );
    }


    @Test
    public void testTwoBytesChar()
    {
        char res = StringTools.bytesToChar( new byte[]
            { ( byte ) 0xCE, ( byte ) 0x91 } );

        assertEquals( 0x0391, res );
    }


    @Test
    public void testThreeBytesChar()
    {
        char res = StringTools.bytesToChar( new byte[]
            { ( byte ) 0xE2, ( byte ) 0x89, ( byte ) 0xA2 } );

        assertEquals( 0x2262, res );
    }


    @Test
    public void testcharToBytesOne()
    {
        assertEquals( "0x00 ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x0000 ) ) );
        assertEquals( "0x61 ", StringTools.dumpBytes( StringTools.charToBytes( 'a' ) ) );
        assertEquals( "0x7F ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x007F ) ) );
    }


    @Test
    public void testcharToBytesTwo()
    {
        assertEquals( "0xC2 0x80 ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x0080 ) ) );
        assertEquals( "0xC3 0xBF ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x00FF ) ) );
        assertEquals( "0xC4 0x80 ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x0100 ) ) );
        assertEquals( "0xDF 0xBF ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x07FF ) ) );
    }


    @Test
    public void testcharToBytesThree()
    {
        assertEquals( "0xE0 0xA0 0x80 ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x0800 ) ) );
        assertEquals( "0xE0 0xBF 0xBF ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x0FFF ) ) );
        assertEquals( "0xE1 0x80 0x80 ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0x1000 ) ) );
        assertEquals( "0xEF 0xBF 0xBF ", StringTools.dumpBytes( StringTools.charToBytes( ( char ) 0xFFFF ) ) );
    }


    @Test
    public void testListToString()
    {
        List<String> list = new ArrayList<String>();

        list.add( "elem1" );
        list.add( "elem2" );
        list.add( "elem3" );

        assertEquals( "elem1, elem2, elem3", StringTools.listToString( list ) );
    }


    @Test
    public void testMapToString()
    {
        class Value
        {
            String name;

            int val;


            public Value(String name, int val)
            {
                this.name = name;
                this.val = val;
            }


            public String toString()
            {
                return "[" + name + ", " + val + "]";
            }
        }

        Map<String, Value> map = new HashMap<String, Value>();

        map.put( "elem1", new Value( "name1", 1 ) );
        map.put( "elem2", new Value( "name2", 2 ) );
        map.put( "elem3", new Value( "name3", 3 ) );

        String result = StringTools.mapToString( map );

        boolean res = "elem1 = '[name1, 1]', elem2 = '[name2, 2]', elem3 = '[name3, 3]'".equals( result )
            || "elem1 = '[name1, 1]', elem3 = '[name3, 3]', elem2 = '[name2, 2]'".equals( result )
            || "elem2 = '[name2, 2]', elem1 = '[name1, 1]', elem3 = '[name3, 3]'".equals( result )
            || "elem2 = '[name2, 2]', elem3 = '[name3, 3]', elem1 = '[name1, 1]'".equals( result )
            || "elem3 = '[name3, 3]', elem1 = '[name1, 1]', elem2 = '[name2, 2]'".equals( result )
            || "elem3 = '[name3, 3]', elem2 = '[name2, 2]', elem1 = '[name1, 1]'".equals( result );

        assertTrue( res );
    }


    @Test
    public void testGetRegexpEmpty() throws Exception
    {
        Pattern pattern = StringTools.getRegex( "", new String[]
            { "" }, "" );

        boolean b1 = pattern.matcher( "" ).matches();

        assertTrue( b1 );
    }


    @Test
    public void testGetRegexpInitial() throws Exception
    {
        Pattern pattern = StringTools.getRegex( "Test", new String[]
            { "" }, "" );

        boolean b1 = pattern.matcher( "Test just a test" ).matches();

        assertTrue( b1 );

        boolean b3 = pattern.matcher( "test just a test" ).matches();

        assertFalse( b3 );
    }


    @Test
    public void testGetRegexpFinal() throws Exception
    {
        Pattern pattern = StringTools.getRegex( "", new String[]
            { "" }, "Test" );

        boolean b1 = pattern.matcher( "test just a Test" ).matches();

        assertTrue( b1 );

        boolean b3 = pattern.matcher( "test just a test" ).matches();

        assertFalse( b3 );
    }


    @Test
    public void testGetRegexpAny() throws Exception
    {
        Pattern pattern = StringTools.getRegex( "", new String[]
            { "just", "a" }, "" );

        boolean b1 = pattern.matcher( "test just a Test" ).matches();

        assertTrue( b1 );

        boolean b3 = pattern.matcher( "test just A test" ).matches();

        assertFalse( b3 );
    }


    @Test
    public void testGetRegexpFull() throws Exception
    {
        Pattern pattern = StringTools.getRegex( "Test", new String[]
            { "just", "a" }, "test" );

        boolean b1 = pattern.matcher( "Test (this is) just (truly !) a (little) test" ).matches();

        assertTrue( b1 );

        boolean b3 = pattern.matcher( "Test (this is) just (truly !) A (little) test" ).matches();

        assertFalse( b3 );
    }


    /**
     * Tests StringTools.getRegex() with some LDAP filter special characters.
     */
    @Test
    public void testGetRegexpWithLdapFilterSpecialChars() throws Exception
    {
        Pattern[] patterns = new Pattern[]
            { StringTools.getRegex( null, new String[]
                { "(" }, null ), StringTools.getRegex( null, new String[]
                { ")" }, null ), StringTools.getRegex( null, new String[]
                { "*" }, null ), StringTools.getRegex( null, new String[]
                { "\\" }, null ), };

        for ( Pattern pattern : patterns )
        {
            boolean b1 = pattern.matcher( "a(b*c\\d)e" ).matches();
            assertTrue( b1 );

            boolean b3 = pattern.matcher( "Test test" ).matches();
            assertFalse( b3 );
        }
    }


    @Test
    public void testDeepTrim()
    {
        assertEquals( "", StringTools.deepTrim( " ", false ) );
        assertEquals( "ab", StringTools.deepTrim( " ab ", false ) );
        assertEquals( "a b", StringTools.deepTrim( " a b ", false ) );
        assertEquals( "a b", StringTools.deepTrim( " a  b ", false ) );
        assertEquals( "a b", StringTools.deepTrim( "  a  b  ", false ) );
        assertEquals( "ab", StringTools.deepTrim( "ab ", false ) );
        assertEquals( "ab", StringTools.deepTrim( " ab", false ) );
        assertEquals( "ab", StringTools.deepTrim( "ab  ", false ) );
        assertEquals( "ab", StringTools.deepTrim( "  ab", false ) );
        assertEquals( "a b", StringTools.deepTrim( "a b", false ) );
        assertEquals( "a b", StringTools.deepTrim( "a  b", false ) );
        assertEquals( "a b", StringTools.deepTrim( " a b", false ) );
        assertEquals( "a b", StringTools.deepTrim( "a b ", false ) );
    }

    @Test
    public void testTrim()
    {
        assertEquals( "", StringTools.trim( (String)null ) );
        assertEquals( "", StringTools.trim( "" ) );
        assertEquals( "", StringTools.trim( " " ) );
        assertEquals( "", StringTools.trim( "  " ) );
        assertEquals( "a", StringTools.trim( "a  " ) );
        assertEquals( "a", StringTools.trim( "  a" ) );
        assertEquals( "a", StringTools.trim( "  a  " ) );
    }

    @Test
    public void testTrimLeft()
    {
        assertEquals( "", StringTools.trimLeft( (String)null ) );
        assertEquals( "", StringTools.trimLeft( "" ) );
        assertEquals( "", StringTools.trimLeft( " " ) );
        assertEquals( "", StringTools.trimLeft( "  " ) );
        assertEquals( "a  ", StringTools.trimLeft( "a  " ) );
        assertEquals( "a", StringTools.trimLeft( "  a" ) );
        assertEquals( "a  ", StringTools.trimLeft( "  a  " ) );
    }

    @Test
    public void testTrimRight()
    {
        assertEquals( "", StringTools.trimRight( (String)null ) );
        assertEquals( "", StringTools.trimRight( "" ) );
        assertEquals( "", StringTools.trimRight( " " ) );
        assertEquals( "", StringTools.trimRight( "  " ) );
        assertEquals( "a", StringTools.trimRight( "a  " ) );
        assertEquals( "  a", StringTools.trimRight( "  a" ) );
        assertEquals( "  a", StringTools.trimRight( "  a  " ) );
    }
}
