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


import javax.naming.NamingException ;
import javax.naming.directory.InvalidAttributeValueException ;

import org.apache.oro.text.perl.Perl5Util ;


/**
 * A SyntaxChecker implemented using Perl5 regular expressions to constrain 
 * values.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public class RegexSyntaxChecker
    implements SyntaxChecker
{
    /** the oid of the syntax checked */
    private final String m_oid ;
    /** the set of regular expressions */
    private final String [] m_expressions ;
    /** the Perl5 regex utilities */
    private final Perl5Util m_perl = new Perl5Util() ;


    /**
     * Creates a Syntax validator for a specific Syntax using Perl5 matching
     * rules for validation.
     * 
     * @param a_oid the oid of the Syntax values checked
     * @param a_matchExprArray the array of matching expressions
     */
    public RegexSyntaxChecker( String a_oid, String [] a_matchExprArray )
    {
        m_expressions = a_matchExprArray ;
        m_oid = a_oid ;
    }


    /**
     * @see org.apache.eve.schema.SyntaxChecker#getSyntaxOid()
     */
    public String getSyntaxOid()
    {
        return m_oid ;
    }


    /**
     * @see org.apache.eve.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object a_value )
    {
        String l_str = null ;
        boolean l_match = true ;
        
        if ( a_value instanceof String )
        {
            l_str = ( String ) a_value ;
        }

        for ( int ii = 0; ii < m_expressions.length; ii++ ) 
        {
            l_match = l_match && m_perl.match( m_expressions[ii], l_str ) ;
            if ( ! l_match ) 
            {
                break ;
            }
        }

        return l_match ;
    }


    /**
     * @see org.apache.eve.schema.SyntaxChecker#assertSyntax(java.lang.Object)
     */
    public void assertSyntax( Object a_value ) throws NamingException
    {
        if ( isValidSyntax( a_value ) )
        {
            return ;
        }
        
        throw new InvalidAttributeValueException( a_value 
                + " does not conform to the syntax specified by " 
                + m_oid ) ;
    }
}
