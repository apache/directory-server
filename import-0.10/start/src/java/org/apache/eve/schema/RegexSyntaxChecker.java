/*
 * $Id: RegexSyntaxChecker.java,v 1.3 2003/03/13 18:28:05 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


import org.apache.oro.text.perl.Perl5Util ;


public class RegexSyntaxChecker
    implements SyntaxChecker
{
    private final String m_syntaxOid ;
    private final String [] m_expressions ;
    private final Perl5Util m_perl = new Perl5Util() ;


    public RegexSyntaxChecker(String a_syntaxOid, String [] a_matchExprArray)
    {
        m_expressions = a_matchExprArray ;
        m_syntaxOid = a_syntaxOid ;
    }


    public String getSyntaxOid()
    {
        return m_syntaxOid ;
    }


    public boolean isValidSyntax(String a_value)
    {
        boolean l_match = true ;

        for(int ii = 0; ii < m_expressions.length; ii++) {
            l_match = l_match && m_perl.match(m_expressions[ii], a_value) ;
            if(!l_match) {
                break ;
            }
        }

        return l_match ;
    }
}
