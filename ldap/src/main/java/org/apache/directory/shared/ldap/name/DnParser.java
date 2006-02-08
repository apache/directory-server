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

package org.apache.directory.shared.ldap.name ;


import java.io.IOException;
import java.io.StringReader;

import javax.naming.Name ;
import javax.naming.NameParser ;
import javax.naming.NamingException ;

import org.apache.directory.shared.ldap.exception.LdapInvalidNameException;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.directory.shared.ldap.util.NestableRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import antlr.TokenStreamSelector ;
import antlr.RecognitionException ;
import antlr.TokenStreamException ;


/**
 * A distinguished name parser which generates JNDI Ldap exception on error.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc2253.html">RFC 2253</a>
 * @see <a href="http://www.faqs.org/rfcs/rfc1779.html">RFC 1779</a>
 */
public class DnParser implements NameParser
{
    private static final Logger log = LoggerFactory.getLogger( DnParser.class );

    private TokenStreamSelector m_selector ;

    private final boolean m_isNormalizing ;

    private ReusableAntlrNameParser m_parser;

    private ReusableAntlrTypeLexer typeLexer;

    private ReusableAntlrValueLexer valueLexer;

    private static final Object parserMutex = new Object();

    /**
     * Creates a regular non normalizing name parser.
     *
     * @throws LdapNamingException if there is a problem creating the pipe
     */
    public DnParser() throws NamingException
    {
        this.m_isNormalizing = false ;
        
        try
        {
            init() ;
        }
        catch ( IOException e )
        {
            String msg = "failed while initializing a name parser:\n";
            msg += ExceptionUtils.getStackTrace( e );
            LdapNamingException ne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
    }


    /**
     * Creates a normalizing name parser.
     *
     * @param a_normalizer the name component value normaliser used
     * @throws LdapNamingException if there is a problem creating the pipe
     */
    public DnParser( NameComponentNormalizer a_normalizer ) throws NamingException
    {
        try
        {
            init() ;
        }
        catch ( IOException e )
        {
            String msg = "failed while initializing a name parser:\n";
            msg += ExceptionUtils.getStackTrace( e );
            LdapNamingException ne = new LdapNamingException( msg, ResultCodeEnum.OTHER );
            ne.setRootCause( e );
            throw ne;
        }
        
        synchronized ( parserMutex )
        {
            this.m_isNormalizing = true ;
            this.m_parser.setNormalizer( a_normalizer ) ;
        }
    }


    /**
     * Tests to see if this parser is normalizing.
     *
     * @return true if it normalizes false otherwise
     */
    public boolean isNormizing()
    {
        return this.m_isNormalizing ;
    }


    /**
     * Initializes the parser machinery and the pluming.
     *
     * @throws IOException if there is a problem creating the parser's pipe
     */
    private void init() throws IOException
    {
        synchronized ( parserMutex )
        {
            this.m_selector = new TokenStreamSelector() ;
    
            // Create lexers and add them to the selector.
            typeLexer = new ReusableAntlrTypeLexer( new StringReader( "" ) );
            this.m_selector.addInputStream( typeLexer, ReusableAntlrTypeLexer.LEXER_KEY );
            valueLexer = new ReusableAntlrValueLexer( typeLexer.getInputState() );
            this.m_selector.addInputStream( valueLexer, ReusableAntlrValueLexer.LEXER_KEY );
    
            // Set selector on lexers, select initial lexer and initalize parser
            typeLexer.setSelector( this.m_selector ) ;
            valueLexer.setSelector( this.m_selector ) ;
            this.m_selector.select( ReusableAntlrTypeLexer.LEXER_KEY );
            this.m_parser = new ReusableAntlrNameParser( m_selector );
        }
    }


    /**
     * Resets the parser and lexers to be reused with new input
     */
    private void reset( String name )     
    {
        this.typeLexer.prepareNextInput( new StringReader( name + "#\n" ) );
        this.valueLexer.prepareNextInput( typeLexer.getInputState() );
        this.m_parser.resetState();
    }


    /**
     * Parses a name as a String into an existing Name object.
     *
     * @param name the distinguished name as a string.
     * @param emptyName the empty LdapName to be populated or null.
     * @return the populated LdapName
     * @throws NamingException if a_name is invalid or the parsers plumbing 
     *     breaks
     */
    public Name parse( String name, LdapName emptyName ) throws NamingException
    {
        if ( log.isDebugEnabled() )
        {
            log.debug( "Parsing DN '" + name + "'" );
        }
        
        // Handle the empty name basis case.
        if ( name == null || name.trim().equals( "" ) )
        {
            return null == emptyName ? new LdapName() : emptyName ;
        }

        try
        {
            if ( null == emptyName )
            {
                synchronized ( parserMutex )
                {
                    reset( name );
                    emptyName = new LdapName( m_parser.name() ) ;
                }
            }
            else 
            {
                synchronized ( parserMutex )
                {
                    reset( name );
                    emptyName.setList( m_parser.name() ) ;
                }
            }
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on name:\n\t" + name ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e ) ;

            throw new LdapInvalidNameException( msg, ResultCodeEnum.INVALIDDNSYNTAX ) ;
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on name:\n\t" + name ;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 ) ;
            throw new LdapInvalidNameException( msg, ResultCodeEnum.INVALIDDNSYNTAX ) ;
        }
        catch ( NestableRuntimeException e )
        {
            Throwable throwable = e.getCause() ;
            if ( throwable instanceof NamingException )
            {
                NamingException ne = ( NamingException ) throwable ;
                throw ne ;
            }
            else
            {
                throw e ;
            }
        }

        return emptyName ;
    }
    
    
    /**
     * Parses a name as a String into a Name object.
     *
     * @see javax.naming.NameParser#parse(java.lang.String)
     */
    public Name parse( String name ) throws NamingException
    {
        return parse( name, new LdapName() ) ;
    }
}
