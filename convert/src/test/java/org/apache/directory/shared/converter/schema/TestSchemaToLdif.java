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

    public void testConvertOC() throws ParserException, IOException
    {
        String expected =
            HEADER + 
            "dn: m-name=objectClass, ou=testOC, ou=schema\n" +
            "objectclass: metaObjectclass\n" +
            "objectclass: metaTop\n" +
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
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
            "objectClass: top\n" +
            "m-oid: 1.3.6.1.4.1.18060.0.4.2.3.14\n" +
            "m-supObjectClass: top\n" +
            "m-supObjectClass: 1.3.6.1.4.1.18060.0.4.2.3.15\n" +
            "m-supObjectClass: metaTop\n\n";

        assertEquals( expected, transform( "testOCSupMany" ) );
    }
}
