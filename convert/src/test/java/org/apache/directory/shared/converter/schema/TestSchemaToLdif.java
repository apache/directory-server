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
package org.apache.directory.shared.converter.schema;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;


public class TestSchemaToLdif extends TestCase
{
    private static final String HEADER = 
        "#\n" +
        "#  Licensed to the Apache Software Foundation (ASF) under one\n" +
        "#  or more contributor license agreements.  See the NOTICE file\n" +
        "#  distributed with this work for additional information\n" +
        "#  regarding copyright ownership.  The ASF licenses this file\n" +
        "#  to you under the Apache License, Version 2.0 (the\n" +
        "#  \"License\"); you may not use this file except in compliance\n" +
        "#  with the License.  You may obtain a copy of the License at\n" +
        "#  \n" +
        "#    http://www.apache.org/licenses/LICENSE-2.0\n" +
        "#  \n" +
        "#  Unless required by applicable law or agreed to in writing,\n" +
        "#  software distributed under the License is distributed on an\n" +
        "#  \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY\n" +
        "#  KIND, either express or implied.  See the License for the\n" +
        "#  specific language governing permissions and limitations\n" +
        "#  under the License. \n" +
        "#\n" +
        "version: 1\n" +
        "\n";  

    private String transform( String name ) throws ParserException, IOException
    {
        List<Schema> schemas = new ArrayList<Schema>();
        Schema schema = new Schema();
        schema.setName( name );
        schema.setInput( getClass().getResourceAsStream( name + ".schema" ) );
        
        Writer out = new StringWriter( 2048 );
        schema.setOutput( out );
        schemas.add( schema );
        
        SchemaToLdif.transform( schemas );
        
        String res = out.toString();
        out.close();
        
        return res;
    }

    //-------------------------------------------------------------------------
    // Tests for ObjectClass
    //-------------------------------------------------------------------------
    public void testConvertOC() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=objectClass, ou=testOC, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: objectClass\n" +
            "m-description: An objectClass\n" +
            "m-obsolete: true\n" +
            "m-supObjectClass: top\n" +
            "m-typeObjectClass: ABSTRACT\n" +
            "m-must: attr1\n" +
            "m-must: attr2\n" +
            "m-may: attr3\n" +
            "m-may: attr4\n\n";

        assertEquals( expected, transform( "testOC") );
    }
    
    public void testConvertOCMinimal() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMinimal, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n\n";

        assertEquals( expected, transform( "testOCMinimal" ) );
    }
    
    public void testConvertOCNoName() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCNoName, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-description: An objectClass\n" +
            "m-obsolete: true\n" +
            "m-supObjectClass: top\n" +
            "m-typeObjectClass: ABSTRACT\n" +
            "m-must: attr1\n" +
            "m-must: attr2\n" +
            "m-may: attr3\n" +
            "m-may: attr4\n\n";

        assertEquals( expected, transform( "testOCNoName" ) );
    }

    public void testConvertOCAbstract() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCAbstract, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-typeObjectClass: ABSTRACT\n\n";

        assertEquals( expected, transform( "testOCAbstract" ) );
    }

    public void testConvertOCAuxiliary() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCAuxiliary, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-typeObjectClass: AUXILIARY\n\n";

        assertEquals( expected, transform( "testOCAuxiliary" ) );
    }

    public void testConvertOCDesc() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCDesc, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-description: An objectClass\n\n";

        assertEquals( expected, transform( "testOCDesc" ) );
    }

    public void testConvertOCMayOne() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMayOne, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-may: attr1\n\n";

        assertEquals( expected, transform( "testOCMayOne" ) );
    }

    public void testConvertOCMay2() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMay2, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-may: attr1\n" +
            "m-may: attr2\n\n";
        
        assertEquals( expected, transform( "testOCMay2" ) );
    }

    public void testConvertOCMayMany() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMayMany, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-may: attr1\n" +
            "m-may: attr2\n" +
            "m-may: attr3\n\n";

        assertEquals( expected, transform( "testOCMayMany" ) );
    }

    public void testConvertOCMustOne() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMustOne, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-must: attr1\n\n";

        assertEquals( expected, transform( "testOCMustOne" ) );
    }

    public void testConvertOCMust2() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMust2, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-must: attr1\n" +
            "m-must: attr2\n\n";
        
        assertEquals( expected, transform( "testOCMust2" ) );
    }

    public void testConvertOCMustMany() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCMustMany, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-must: attr1\n" +
            "m-must: attr2\n" +
            "m-must: attr3\n\n";

        assertEquals( expected, transform( "testOCMustMany" ) );
    }

    public void testConvertOCNameOne() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=objectClass, ou=testOCNameOne, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: objectClass\n\n";

        assertEquals( expected, transform( "testOCNameOne" ) );
    }

    public void testConvertOCName2() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=oc, ou=testOCName2, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: oc\n" +
            "m-name: objectClass\n\n";

        assertEquals( expected, transform( "testOCName2" ) );
    }

    public void testConvertOCNameMany() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=oc, ou=testOCNameMany, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: oc\n" +
            "m-name: objectClass\n" +
            "m-name: object\n\n";

        assertEquals( expected, transform( "testOCNameMany" ) );
    }

    public void testConvertOCObsolete() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCObsolete, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-obsolete: true\n\n";

        assertEquals( expected, transform( "testOCObsolete" ) );
    }

    public void testConvertOCSupOne() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCSupOne, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supObjectClass: top\n\n";

        assertEquals( expected, transform( "testOCSupOne" ) );
    }

    public void testConvertOCSup2() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCSup2, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supObjectClass: top\n" +
            "m-supObjectClass: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testOCSup2" ) );
    }

    public void testConvertOCSupMany() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testOCSupMany, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supObjectClass: top\n" +
            "m-supObjectClass: 1.3.6.1.4.1.18060.0.4.2.3.15\n" +
            "m-supObjectClass: metaTop\n\n";

        assertEquals( expected, transform( "testOCSupMany" ) );
    }
    
    //-------------------------------------------------------------------------
    // Tests for Attributetype
    //-------------------------------------------------------------------------
    public void testConvertATMinimal() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATMinimal, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n\n";

        assertEquals( expected, transform( "testATMinimal" ) );
    }
    
    public void testConvertATNoName() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATNoName, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n\n";

        assertEquals( expected, transform( "testATNoName" ) );
    }

    public void testConvertATNameOne() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=attribute, ou=testATNameOne, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: attribute\n\n";

        assertEquals( expected, transform( "testATNameOne" ) );
    }

    public void testConvertATName2() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=at, ou=testATName2, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: at\n" +
            "m-name: attribute\n\n";

        assertEquals( expected, transform( "testATName2" ) );
    }

    public void testConvertATNameMany() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=at, ou=testATNameMany, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-name: at\n" +
            "m-name: attribute\n" +
            "m-name: attribute2\n\n";

        assertEquals( expected, transform( "testATNameMany" ) );
    }

    public void testConvertATDesc() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATDesc, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-description: An attributeType\n\n";

        assertEquals( expected, transform( "testATDesc" ) );
    }

    public void testConvertATObsolete() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATObsolete, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-obsolete: true\n\n";

        assertEquals( expected, transform( "testATObsolete" ) );
    }

    public void testConvertATSup() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSup, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supAttributeType: anotherAttribute\n\n";

        assertEquals( expected, transform( "testATSup" ) );
    }

    public void testConvertATSupOID() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSupOID, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supAttributeType: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testATSupOID" ) );
    }

    public void testConvertATEquality() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATEquality, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-equality: booleanMatch\n\n";

        assertEquals( expected, transform( "testATEquality" ) );
    }

    public void testConvertATEqualityOID() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATEqualityOID, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-equality: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testATEqualityOID" ) );
    }

    public void testConvertATOrdering() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATOrdering, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-ordering: booleanMatch\n\n";

        assertEquals( expected, transform( "testATOrdering" ) );
    }

    public void testConvertATOrderingOID() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATOrderingOID, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-ordering: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testATOrderingOID" ) );
    }

    public void testConvertATSubstr() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSubstr, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-substr: booleanMatch\n\n";

        assertEquals( expected, transform( "testATSubstr" ) );
    }

    public void testConvertATSubstrOID() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSubstrOID, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-substr: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testATSubstrOID" ) );
    }

    public void testConvertATSyntax() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSyntax, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-syntax: 1.3.6.1.4.1.18060.0.4.2.3.15\n\n";

        assertEquals( expected, transform( "testATSyntax" ) );
    }

    public void testConvertATSyntaxOidLen() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSyntaxOidLen, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-syntax: 1.3.6.1.4.1.18060.0.4.2.3.15{123}\n\n";

        assertEquals( expected, transform( "testATSyntaxOidLen" ) );
    }

    public void testConvertATSingleValue() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATSingleValue, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-singleValue: true\n\n";

        assertEquals( expected, transform( "testATSingleValue" ) );
    }

    public void testConvertATCollective() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATCollective, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-collective: true\n\n";

        assertEquals( expected, transform( "testATCollective" ) );
    }

    public void testConvertATNoUserModification() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATNoUserModification, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-noUserModification: true\n\n";

        assertEquals( expected, transform( "testATNoUserModification" ) );
    }

    public void testConvertATUsageUserApp() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATUsageUserApp, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n\n";

        assertEquals( expected, transform( "testATUsageUserApp" ) );
    }

    public void testConvertATUsageDirOp() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATUsageDirOp, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-usage: directoryOperation\n\n";

        assertEquals( expected, transform( "testATUsageDirOp" ) );
    }

    public void testConvertATUsageDistrOp() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATUsageDistrOp, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-usage: distributedOperation\n\n";

        assertEquals( expected, transform( "testATUsageDistrOp" ) );
    }

    public void testConvertATUsageDSAOp() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=1.3.6.1.4.1.18060.0.4.2.3.14, ou=testATUsageDsaOp, ou=schema\n" +
            "objectclass: metaAttributeType\n" +
            "objectclass: metaTop\n" +
            "objectclass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-usage: dSAOperation\n\n";

        assertEquals( expected, transform( "testATUsageDsaOp" ) );
    }
}
