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
package org.apache.directory.shared.ldap.schema.syntax.parser;


import junit.framework.TestCase;

import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;


/**
 * Tests the AttributeTypeDescriptionSchemaParser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeDescriptionSchemaParserTest extends TestCase
{
    private AttributeTypeDescriptionSchemaParser parser = new AttributeTypeDescriptionSchemaParser();
    
    
    /**
     * Tests the parse of a simple AttributeType
     */
    public void testAddAttributeType() throws Exception
    {
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) " +
                "DESC 'bogus description' SUP name SINGLE-VALUE )";
        AttributeTypeDescription desc = parser.parseAttributeTypeDescription( substrate );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", desc.getNumericOid() );
        assertEquals( "bogus", desc.getNames().get( 0 ) );
        assertEquals( "bogusName", desc.getNames().get( 1 ) );
        assertEquals( "bogus description", desc.getDescription() );
        assertEquals( "name", desc.getSuperType() );
        assertEquals( true, desc.isSingleValued() );
    }

    
    /**
     * Tests the parse of a simple AttributeType with the schema extension.
     */
    public void testAttributeTypeWithSchemaExtension() throws Exception
    {
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 NAME ( 'bogus' 'bogusName' ) " +
                "DESC 'bogus description' SUP name SINGLE-VALUE X-SCHEMA 'blah' )";
        AttributeTypeDescription desc = parser.parseAttributeTypeDescription( substrate );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", desc.getNumericOid() );
        assertEquals( "bogus", desc.getNames().get( 0 ) );
        assertEquals( "bogusName", desc.getNames().get( 1 ) );
        assertEquals( "bogus description", desc.getDescription() );
        assertEquals( "name", desc.getSuperType() );
        assertEquals( true, desc.isSingleValued() );
        assertEquals( "blah", desc.getExtensions().get( "X-SCHEMA" ).get( 0 ) );
    }
}
