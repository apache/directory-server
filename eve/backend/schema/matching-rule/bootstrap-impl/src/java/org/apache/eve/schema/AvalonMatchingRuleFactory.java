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


import java.util.Comparator;

import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException;


/**
 * A MatchingRule factory that creates MatchingRules using Avalon Configuration
 * information associated with a component.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class AvalonMatchingRuleFactory
{
    /** singleton factory instance */
    private static AvalonMatchingRuleFactory s_instance = null ;
    
    
    /**
     * Creates a factory singleton checking to make sure it was not 
     * instantiated before.
     */
    private AvalonMatchingRuleFactory()
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
    public static AvalonMatchingRuleFactory getInstance()
    {
        if ( s_instance == null )
        {    
            s_instance = new AvalonMatchingRuleFactory() ;
        }
        
        return s_instance ;
    }

    
    /**
     * Creates a MatchingRule instance using a Configuration object.
     * 
     * @param a_config an Avalon components Configuration object.
     * @return the MatchingRule represented by the Configuration
     * @throws ConfigurationException if there is an error with 
     *      the Configuration
     */
    public MatchingRule create( Configuration a_config, 
                                SyntaxRegistry a_syntaxes )
        throws ConfigurationException
    {
        AvalonMatchingRule l_matchingRule = null ;
        SyntaxChecker l_checker = null ;
        String l_oid = a_config.getChild( "oid" ).getValue() ;

        // l_matchingRule = new AvalonMatchingRule( l_oid ) ;
        if ( true ) throw new ConfigurationException( 
                    "not implemented yet" ) ;
        
        if ( a_config.getChild( "description" ).getValue( null ) != null )
        {    
            l_matchingRule.setDescription( a_config.getChild( "description" )
                    .getValue() ) ;
        }
        
        if ( a_config.getChild( "name" ).getValue( null ) != null )
        {    
            l_matchingRule.setName( a_config.getChild( "name" )
                    .getValue() ) ;
        }
        
        return l_matchingRule ;
    }
    
    
    /**
     * Class used to safely expose mutators on the Syntax bean.
     *
     * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
     * @author $Author: akarasulu $
     * @version $Rev: 1465 $
     */
    private class AvalonMatchingRule extends DefaultMatchingRule
    {
        AvalonMatchingRule( String a_oid, Syntax a_syntax, 
                Comparator a_comparator, Normalizer a_normalizer ) 
            throws ConfigurationException
        {
            super( a_oid, a_syntax, a_comparator, a_normalizer ) ;
        }
        
        public void setDescription( String a_description )
        {
            super.setDescription( a_description ) ;
        }
        
        public void setName( String a_name )
        {
            super.setName( a_name ) ;
        }

        public void setObsolete( boolean a_isObsolete )
        {
            super.setObsolete( a_isObsolete ) ;
        }
    }
}
