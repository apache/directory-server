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


import org.apache.commons.lang.NotImplementedException ;

import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;


/**
 * Creates SyntaxCheckers using an Avalon Configuration.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class AvalonSyntaxCheckerFactory
{
    /** singleton instance of the factory */
    private static AvalonSyntaxCheckerFactory s_instance = null ;
    
    /**
     * Creates a singleton factory that makes SyntaxCheckers using Avalon
     * configuration nodes.
     */
    private AvalonSyntaxCheckerFactory()
    {
        if ( s_instance != null )
        {    
            throw new IllegalStateException( "Singleton create attempted more "
                    + "than once." ) ;
        }
    }
    

    /**
     * Get's a handle on a singleton instance of the factory.
     * 
     * @return the singleton instance of the factory
     */
    public static AvalonSyntaxCheckerFactory getInstance()
    {
        if ( s_instance == null )
        {
            s_instance = new AvalonSyntaxCheckerFactory() ;
        }
        
        return s_instance ;
    }
    
    
    /**
     * Creates a SyntaxChecker using an Avalon Configuration node.
     * 
     * @param a_oid the oid of the syntax the checker validates
     * @param a_config the configuration node for the SyntaxChecker
     * @return the newly created SyntaxChecker
     * @throws ConfigurationException if their is a problem with the config
     */
    public SyntaxChecker create( String a_oid, Configuration a_config ) 
        throws ConfigurationException
    {
        SyntaxChecker l_checker = null ;
        
        // complain if the node is not <syntax-checker>
        if ( ! a_config.getName().equals( "syntax-checker" ) )
        {
            throw new ConfigurationException( "Expected a configuration node "
                    + "with the name 'syntax-checker' but got " +
                    a_config.getName(), a_config ) ;
        }

        Configuration [] l_children = a_config.getChildren() ; 
        if ( l_children[0].getName().equals( "class" ) ) 
        {
            l_checker = create( l_children[0].getValue() ) ;
        }
        else if ( l_children[0].getName().equals( "built-in" ) )
        {
            String l_builtIn = l_children[0].getValue() ;
            
            if ( l_builtIn.equals( "unchecked" ) )
            {
                
            }
            else  
            {
                throw new NotImplementedException( 
                        "built-ins not implemented" ) ;
            }
            
            /*l_checker = SyntaxCheckerFactory
                .create( a_oid, l_children[0].getValue() ) ;*/
        }
        else if ( l_children[0].getName().equals( "regex" ) )
        {
            String [] l_exprs = new String [ l_children.length ] ;
            for ( int ii = 0; ii < l_children.length; ii++ )
            {
                l_exprs[ii] = l_children[ii].getValue() ;
            }
            
            l_checker = new RegexSyntaxChecker( a_oid, l_exprs ) ;
        }
        else
        {
            throw new ConfigurationException( 
                    "Undefined syntax-checker configuration", a_config ) ;
        }
        
        return l_checker ;
    }
    
    
    /**
     * Creates a SyntaxChecker by loading and instantiating a class.
     * 
     * @param a_fqcn the fully qualified class name 
     * @return the newly instantiated SyntaxChecker
     * @throws ConfigurationException if there are problems loading and 
     * instantiating from the class.
     */
    public SyntaxChecker create( String a_fqcn )
        throws ConfigurationException
    {
        try
        {
            return ( SyntaxChecker ) Class.forName( a_fqcn ).newInstance() ;
        }
        catch( ClassNotFoundException e )
        {
            throw new ConfigurationException( "Cannot find syntax-checker "
                    + "class: " + a_fqcn, e ) ;
        }
        catch( IllegalAccessException e )
        {
            throw new ConfigurationException( "syntax-checker "
                    + "class " + a_fqcn + " may not have a public default "
                    + "constructor.", e ) ;
        }
        catch( InstantiationException e )
        {
            throw new ConfigurationException( "Failed while attempting to "
                    + "instantiate syntax-checker class " + a_fqcn 
                    + " using its public default constructor.", e ) ;
        }
    }
}
