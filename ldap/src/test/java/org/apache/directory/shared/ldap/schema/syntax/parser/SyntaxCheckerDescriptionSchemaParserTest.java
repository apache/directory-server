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


import org.apache.directory.shared.ldap.schema.syntax.SyntaxCheckerDescription;

import junit.framework.TestCase;


/**
 * Tests the correct operation of the SyntaxCheckerDescriptionSchemaParser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SyntaxCheckerDescriptionSchemaParserTest extends TestCase
{
    private static final String OID = "1.3.6.1.4.1.18060.0.4.0.2.10000";
    private static final String FQCN = "org.foo.Bar";
    private static final String DESC = "bogus desc";
    private static final String BYTECODE = "14561234";
    private SyntaxCheckerDescriptionSchemaParser parser = new SyntaxCheckerDescriptionSchemaParser();
    
    
    public void testSimpleSyntaxChecker() throws Exception
    {
        String simple = "( " + OID + " FQCN " + FQCN + " )";
        SyntaxCheckerDescription desc = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( desc );
        assertEquals( OID, desc.getNumericOid() );
        assertEquals( FQCN, desc.getFqcn() );
        assertNull( desc.getBytecode() );
        assertNull( desc.getDescription() );
    }
    
    
    public void testSyntaxCheckerWithDesc() throws Exception
    {
        String simple = "( " + OID + " DESC '" + DESC + "' FQCN " + FQCN + " )";
        SyntaxCheckerDescription desc = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( desc );
        assertEquals( OID, desc.getNumericOid() );
        assertEquals( FQCN, desc.getFqcn() );
        assertNull( desc.getBytecode() );
        assertEquals( DESC, desc.getDescription() );
    }
    
    
    public void testSyntaxCheckerWithDescAndByteCode() throws Exception
    {
        String simple = "( " + OID + " DESC '" + DESC + "' FQCN " + FQCN + " BYTECODE " + BYTECODE + " )";
        SyntaxCheckerDescription desc = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( desc );
        assertEquals( OID, desc.getNumericOid() );
        assertEquals( FQCN, desc.getFqcn() );
        assertEquals( BYTECODE, desc.getBytecode() );
        assertEquals( DESC, desc.getDescription() );
    }
    
    
    public void testSyntaxCheckerExample() throws Exception
    {
        String simple = "( 1.3.6.1.4.1.18060.0.4.1.0.10000 DESC 'bogus desc' FQCN org.apache.directory.shared.ldap.schema.syntax.AcceptAllSyntaxChecker )";
        SyntaxCheckerDescription desc = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( desc );
    }
    
    
    public void testRealByteCodeExample() throws Exception
    {
        String simple = "( 1.3.6.1.4.1.18060.0.4.1.0.10002 DESC 'bogus desc' " +
                "FQCN DummySyntaxChecker BYTECODE yv66vgAAADEAHgoABAAYCQADABkHABoHABsHABwBAANvaWQBABJMam" +
                "F2YS9sYW5nL1N0cmluZzsBAAY8aW5pdD4BABUoTGphdmEvbGFuZy9TdHJpbmc7KVYBAARDb2RlAQAPTGluZU51b" +
                "WJlclRhYmxlAQADKClWAQAMc2V0U3ludGF4T2lkAQAMZ2V0U3ludGF4T2lkAQAUKClMamF2YS9sYW5nL1N0cmlu" +
                "ZzsBAA1pc1ZhbGlkU3ludGF4AQAVKExqYXZhL2xhbmcvT2JqZWN0OylaAQAMYXNzZXJ0U3ludGF4AQAVKExqYXZ" +
                "hL2xhbmcvT2JqZWN0OylWAQAKRXhjZXB0aW9ucwcAHQEAClNvdXJjZUZpbGUBABdEdW1teVN5bnRheENoZWNrZX" +
                "IuamF2YQwACAAMDAAGAAcBABJEdW1teVN5bnRheENoZWNrZXIBABBqYXZhL2xhbmcvT2JqZWN0AQA8b3JnL2FwY" +
                "WNoZS9kaXJlY3Rvcnkvc2hhcmVkL2xkYXAvc2NoZW1hL3N5bnRheC9TeW50YXhDaGVja2VyAQAcamF2YXgvbmFt" +
                "aW5nL05hbWluZ0V4Y2VwdGlvbgAhAAMABAABAAUAAQACAAYABwAAAAYAAQAIAAkAAQAKAAAAKgACAAIAAAAKKrc" +
                "AASortQACsQAAAAEACwAAAA4AAwAAAAsABAAMAAkADQABAAgADAABAAoAAAAhAAEAAQAAAAUqtwABsQAAAAEACw" +
                "AAAAoAAgAAABEABAASAAEADQAJAAEACgAAACIAAgACAAAABiortQACsQAAAAEACwAAAAoAAgAAABcABQAYAAEAD" +
                "gAPAAEACgAAAB0AAQABAAAABSq0AAKwAAAAAQALAAAABgABAAAAHQABABAAEQABAAoAAAAaAAEAAgAAAAIErAAA" +
                "AAEACwAAAAYAAQAAACMAAQASABMAAgAKAAAAGQAAAAIAAAABsQAAAAEACwAAAAYAAQAAACkAFAAAAAQAAQAVAAE" +
                "AFgAAAAIAFw== X-SCHEMA 'nis' )";
        SyntaxCheckerDescription desc = parser.parseSyntaxCheckerDescription( simple );
        assertNotNull( desc );
        assertEquals( "1.3.6.1.4.1.18060.0.4.1.0.10002", desc.getNumericOid() );
        assertEquals( "DummySyntaxChecker", desc.getFqcn() );
        assertNotNull( desc.getBytecode() );
        assertEquals( "bogus desc", desc.getDescription() );
    }
}
