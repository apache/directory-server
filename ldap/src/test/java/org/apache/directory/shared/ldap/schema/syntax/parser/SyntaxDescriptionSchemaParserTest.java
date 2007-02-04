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


import org.apache.directory.shared.ldap.schema.syntax.LdapSyntaxDescription;

import junit.framework.TestCase;


/**
 * Document me!
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyntaxDescriptionSchemaParserTest extends TestCase
{
    private LdapSyntaxDescriptionSchemaParser parser = new LdapSyntaxDescriptionSchemaParser();
    
    
    /**
     * Tests the parse of a simple AttributeType with the schema extension.
     */
    public void testSyntaxWithExtensions() throws Exception
    {
        String substrate = "( 1.3.6.1.4.1.18060.0.4.0.2.10000 DESC 'bogus description' X-SCHEMA 'blah' X-IS-HUMAN-READABLE 'true' )";
        LdapSyntaxDescription desc = parser.parseLdapSyntaxDescription( substrate );
        assertEquals( "1.3.6.1.4.1.18060.0.4.0.2.10000", desc.getNumericOid() );
        assertEquals( "bogus description", desc.getDescription() );
        assertNotNull( desc.getExtensions().get( "X-IS-HUMAN-READABLE" ) );
    }
}
