/*
 * $Id: SyntaxChecker.java,v 1.3 2003/03/13 18:28:09 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.schema ;


public interface SyntaxChecker
{

    String getSyntaxOid() ;
    boolean isValidSyntax(String a_string) ;
}
