/*
 * $Id: RegexNormalizer.java,v 1.4 2003/03/13 18:28:05 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.eve.schema ;

import org.apache.oro.text.perl.Perl5Util ;


public class RegexNormalizer
    implements Normalizer
{
    final Perl5Util m_perl = new Perl5Util() ;
    final String [] m_exprArray ;
    final String m_equalMatch ;


    public RegexNormalizer(String an_equalMatch, String [] a_exprArray)
    {
        m_equalMatch = an_equalMatch ;
        m_exprArray = a_exprArray ;
    }


    public String normalize(final String a_value)
    {
        String l_canonical = a_value ;

        for(int ii = 0; ii < m_exprArray.length; ii++) {
            l_canonical = m_perl.substitute(m_exprArray[ii], l_canonical) ;
        }

        return l_canonical ;
    }


    public String getEqualityMatch()
    {
        return m_equalMatch ;
    }


    public String toString()
    {
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append("RegexNormalizer(").append(m_equalMatch).append(", ") ;
        l_buf.append(m_exprArray).append(')') ;
        return l_buf.toString() ;
    }
}
