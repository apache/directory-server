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
package org.apache.directory.shared.ldap.schema.parser;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.schema.syntax.AbstractSchemaDescription;
import org.apache.directory.shared.ldap.schema.syntax.AttributeTypeDescription;
import org.apache.directory.shared.ldap.schema.syntax.ObjectClassDescription;
import org.apache.directory.shared.ldap.schema.syntax.parser.AbstractSchemaParser;
import org.apache.directory.shared.ldap.util.ExceptionUtils;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper for antlr generated OpenLDAP schema parsers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494164 $
 */
public class OpenLdapSchemaParser extends AbstractSchemaParser
{
    /** the monitor to use for this parser */
    private ParserMonitor monitor = new ParserMonitorAdapter();
    
    /** The list of parsed schema descriptions */
    private List<AbstractSchemaDescription> schemaDescriptions;

    private List<AttributeTypeLiteral> attributeTypeLiterals;
    private List<ObjectClassLiteral> objectClassLiterals;
    
    /**
     * Creates a reusable instance of an OpenLdapSchemaParser.
     *
     * @throws IOException if the pipe cannot be formed
     */
    public OpenLdapSchemaParser() throws IOException
    {
        init();
    }


    /**
     * Initializes a parser and its plumbing.
     */
    public void init()
    {
    }


    /**
     * Reset the parser 
     */
    public synchronized void clear()
    {
    }


    /**
     * @return the AttributeTypes list
     */
    public List<AttributeTypeLiteral> getAttributeTypes()
    {
        if(attributeTypeLiterals == null) 
        {
            splitParsedSchemaDescriptions();
        }
        
        return attributeTypeLiterals;
    }


    public List<ObjectClassLiteral> getObjectClassTypes()
    {
        if(objectClassLiterals == null) 
        {
            splitParsedSchemaDescriptions();
        }
        
        return objectClassLiterals;
    }
    
    private void splitParsedSchemaDescriptions()
    {
        objectClassLiterals = new ArrayList<ObjectClassLiteral>();
        attributeTypeLiterals = new ArrayList<AttributeTypeLiteral>();
        
        for ( AbstractSchemaDescription schemaDescription : schemaDescriptions )
        {
            if(schemaDescription instanceof AttributeTypeDescription)
            {
                AttributeTypeDescription atd = (AttributeTypeDescription)schemaDescription;
                AttributeTypeLiteral literal = new AttributeTypeLiteral(atd.getNumericOid());
                literal.setNames( atd.getNames().toArray( new String[atd.getNames().size()] ) );
                literal.setDescription( atd.getDescription() );
                literal.setSuperior( atd.getSuperType() );
                literal.setEquality( atd.getEqualityMatchingRule() );
                literal.setOrdering( atd.getOrderingMatchingRule() );
                literal.setSubstr( atd.getSubstringsMatchingRule() );
                literal.setSyntax( atd.getSyntax() );
                literal.setLength( atd.getSyntaxLength() );
                literal.setObsolete( atd.isObsolete() );
                literal.setCollective( atd.isCollective() );
                literal.setSingleValue( atd.isSingleValued() );
                literal.setNoUserModification( !atd.isUserModifiable() );
                literal.setUsage( atd.getUsage() );
                attributeTypeLiterals.add( literal );
            }
            else if(schemaDescription instanceof ObjectClassDescription)
            {
                ObjectClassDescription ocd = (ObjectClassDescription)schemaDescription;
                ObjectClassLiteral literal = new ObjectClassLiteral(ocd.getNumericOid());
                literal.setNames( ocd.getNames().toArray( new String[ocd.getNames().size()] ) );
                literal.setDescription( ocd.getDescription() );
                literal.setSuperiors( ocd.getSuperiorObjectClasses().toArray( new String[ocd.getSuperiorObjectClasses().size()] ) );
                literal.setMay( ocd.getMayAttributeTypes().toArray( new String[ocd.getMayAttributeTypes().size()] ) );
                literal.setMust( ocd.getMustAttributeTypes().toArray( new String[ocd.getMustAttributeTypes().size()] ) );
                literal.setClassType( ocd.getKind() );
                literal.setObsolete( ocd.isObsolete() );
                objectClassLiterals.add( literal );
            }
        }
    }


    /**
     * Thread safe method parses an OpenLDAP schemaObject element/object.
     *
     * @param schemaObject the String image of a complete schema object
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public synchronized AbstractSchemaDescription parse( String schemaObject ) throws ParseException
    {
        if ( schemaObject == null || schemaObject.trim().equals( "" ) )
        {
            throw new ParseException( "The schemaObject is either null or is " + "the empty String!", 0 );
        }
        
        reset( schemaObject ); // reset and initialize the parser / lexer pair
        invokeParser( schemaObject );
        
        // TODO: return
        return null;
    }


    private void invokeParser( String subject ) throws ParseException
    {
        try
        {
            monitor.startedParse( "starting parse on:\n" + subject );
            schemaDescriptions = parser.openLdapSchema();
            monitor.finishedParse( "Done parsing!" );
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e );
            init();
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 );
            init();
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Thread safe method parses a stream of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaIn a stream of schema objects
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public synchronized void parse( InputStream schemaIn ) throws IOException, ParseException
    {
        InputStreamReader in = new InputStreamReader( schemaIn );
        lexer.prepareNextInput( in );
        parser.resetState();
        
        invokeParser( "schema input stream ==> " + schemaIn.toString() );
    }


    /**
     * Thread safe method parses a file of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaFile a file of schema objects
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public synchronized void parse( File schemaFile ) throws IOException, ParseException
    {
        FileReader in = new FileReader( schemaFile );
        lexer.prepareNextInput( in );
        parser.resetState();
        
        invokeParser( "schema file ==> " + schemaFile.getAbsolutePath() );
    }


    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
        parser.setParserMonitor( monitor );
    }

}
