/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/
package org.apache.eve.schema ;


import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * A factory for creating Syntaxes using Avalon Configuration nodes.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class AvalonSyntaxFactory
{
    /** singleton factory instance */
    private static AvalonSyntaxFactory s_instance = null ;
    
    
    /**
     * Creates a factory singleton checking to make sure it was not 
     * instantiated before.
     */
    private AvalonSyntaxFactory()
    {
        if ( s_instance == null )
        {
            return ;
        }
        
        throw new IllegalStateException( "Attempt to re-instantiate " +
                "singleton." ) ;
    }
    

    /**
     * Gets access to a singleton instance of a syntax generating factory.
     * 
     * @return the singleton factory
     */
    public static AvalonSyntaxFactory getInstance()
    {
        if ( s_instance == null )
        {    
            s_instance = new AvalonSyntaxFactory() ;
        }
        
        return s_instance ;
    }

    
    /**
     * Creates a Syntax based on an Avalon Configuration node.
     * 
     * @param a_config the configuration node for the syntax
     * @return the newly created Syntax
     * @throws ConfigurationException if there is a configuration error
     */
    public Syntax create( Configuration a_config ) throws ConfigurationException
    {
        AvalonSyntax l_syntax = null ;
        SyntaxChecker l_checker = null ;
        String l_oid = a_config.getChild( "oid" ).getValue() ;
        AvalonSyntaxCheckerFactory l_checkerFactory = null ;
        
        l_checkerFactory = AvalonSyntaxCheckerFactory.getInstance() ;
        l_checker = l_checkerFactory.create( l_oid, a_config
                .getChild( "syntax-checker" ) ) ;

        l_syntax = new AvalonSyntax( l_oid, l_checker ) ;
        
        if ( a_config.getChild( "description" ).getValue( null ) != null )
        {    
            l_syntax.setDescription( a_config.getChild( "description" )
                    .getValue() ) ;
        }
        
        if ( a_config.getChild( "human-readable" ).getValue( null ) != null )
        {
            l_syntax.setHumanReadable( a_config.getChild( "human-readable" )
                    .getValueAsBoolean() ) ;
        }
        
        if ( a_config.getChild( "name" ).getValue( null ) != null )
        {    
            l_syntax.setName( a_config.getChild( "name" )
                    .getValue() ) ;
        }
        
        return l_syntax ;
    }
    
    
    /**
     * Class used to safely expose mutators on the Syntax bean.
     *
     * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
     * @author $Author$
     * @version $Rev$
     */
    private class AvalonSyntax extends DefaultSyntax
    {
        AvalonSyntax( String a_oid, SyntaxChecker a_checker ) 
            throws ConfigurationException
        {
            super( a_oid, a_checker ) ;
        }
        
        public void setDescription( String a_description )
        {
            super.setDescription( a_description ) ;
        }
        
        public void setHumanReadable( boolean a_isHumanReadable )
        {
            super.setHumanReadible( a_isHumanReadable ) ;
        }
        
        public void setName( String a_name )
        {
            super.setName( a_name ) ;
        }
    }
}