/*
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
package org.apache.eve.tools.schema;


import junit.framework.TestCase;

import java.util.Map;
import java.io.ByteArrayInputStream;

import org.apache.eve.schema.DefaultOidRegistry;
import org.apache.ldap.common.schema.AttributeType;


/**
 * Tests the parser for AttributeTypes.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeParserTest extends TestCase
{
    public void testParser() throws Exception
    {
        String attributeTypeData = "# adding a comment  \n" +
            "attributetype ( 2.5.4.2 NAME 'knowledgeInformation'\n" +
            "        DESC 'RFC2256: knowledge information'\n" +
            "        EQUALITY caseIgnoreMatch\n" +
            "        SYNTAX 1.3.6.1.4.1.1466.115.121.1.15{32768} )";
        ByteArrayInputStream in = new ByteArrayInputStream( attributeTypeData.getBytes() );
        antlrOpenLdapSchemaLexer lexer = new antlrOpenLdapSchemaLexer( in );
        antlrOpenLdapSchemaParser parser = new antlrOpenLdapSchemaParser( lexer );
        parser.setParserMonitor( new ParserMonitor()
        {
            public void matchedProduction( String prod )
            {
                System.out.println( prod );
            }
        });

        DefaultOidRegistry registry = new DefaultOidRegistry();
        registry.register( "caseIgnoreMatch", "1.1.1.1.1.1" );
        parser.setOidRegistry( registry );

        Map attributeTypes = parser.getAttributeTypes();
        parser.attributeType();
        AttributeType type = ( AttributeType ) attributeTypes.get( "2.5.4.2" );

        assertNotNull( type );
        assertEquals( "2.5.4.2", type.getOid() );
        assertEquals( "knowledgeInformation", type.getName() );
        assertEquals( "RFC2256: knowledge information", type.getDescription() );
        assertEquals( "1.3.6.1.4.1.1466.115.121.1.15",
            type.getSyntax().getOid() );
        assertEquals( 32768, type.getLength() );
    }
}
