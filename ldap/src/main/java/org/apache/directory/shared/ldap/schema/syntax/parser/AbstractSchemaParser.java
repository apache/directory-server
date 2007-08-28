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

import java.io.StringReader;
import java.text.ParseException;

import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;

public abstract class AbstractSchemaParser
{


    /** the antlr generated parser being wrapped */
    protected ReusableAntlrSchemaParser parser;

    /** the antlr generated lexer being wrapped */
    protected ReusableAntlrSchemaLexer lexer;
    
    
    protected AbstractSchemaParser() 
    {
        lexer = new ReusableAntlrSchemaLexer( new StringReader( "" ) );
        parser = new ReusableAntlrSchemaParser( lexer );
    }
    
    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it. param spec the specification to be parsed
     */
    protected void reset( String spec )
    {
        StringReader in = new StringReader( spec );
        lexer.prepareNextInput( in );
        parser.resetState();
    }
    
    public abstract AbstractSchemaDescription parse( String schemaDescription ) throws ParseException;
    
}
