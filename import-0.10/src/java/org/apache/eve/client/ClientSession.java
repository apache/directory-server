/*
 * $Id: ClientSession.java,v 1.4 2003/08/22 21:15:55 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client ;


import java.util.Locale ;
import java.util.Iterator;

import org.apache.eve.security.LdapPrincipal ;



public interface ClientSession
{
    ClientKey getClientKey() ;

    ////////////////////////////
    // Session Attribute APIs //
    ////////////////////////////

    boolean isValid() ;

    Iterator getAttributeNames() ;

    Object getAttribute(String an_attrName) ;

    void removeAttribute(String an_attrName) ;

    void setAttribute(String an_attrName, Object a_attrValue) ;

    void invalidate() ;

    boolean isNew() ;

    long getCreationTime() ;

    long getLastAccessedTime() ;

    int getMaxInactiveInterval() ;

    void setMaxInactiveInterval(int an_interval) ;

    //ServerConfig getServerConfig() ;


    /////////////////////////
    // Session Client APIs //
    /////////////////////////


    Locale getLocale() ;

    LdapPrincipal getPrincipal() ;
}
