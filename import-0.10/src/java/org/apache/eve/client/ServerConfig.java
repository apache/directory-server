/*
 * $Id: ServerConfig.java,v 1.2 2003/03/13 18:27:08 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.eve.client ;



import java.util.Iterator ;


public interface ServerConfig
{
    String getServerName() ;
    String getInitParameter(String a_paramName) ;
    Iterator getInitParameterNames() ;
}
