/*
 * $Id: LdapClientSession.java,v 1.2 2003/08/22 21:15:55 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client ;


import java.util.Locale ;
import java.util.Iterator ;
import java.util.Hashtable ;
import java.util.Observable ;

import org.apache.eve.security.LdapPrincipal ;


public class LdapClientSession
    extends Observable
    implements ClientSession
{
    public static final int MAX_INACTIVE_INTERVAL = 900 ;

    private final long m_creationTime = System.currentTimeMillis() ;
    private final Hashtable m_attribs = new Hashtable() ;

    protected boolean m_isNew = true ;
    protected boolean m_isValid = true ;
    protected int m_maxInactiveInterval = MAX_INACTIVE_INTERVAL ;
    protected long m_lastAccessedTime = m_creationTime ;
    protected final ClientKey m_clientKey ;
    protected LdapPrincipal m_principal = null ;
    protected ServerConfig m_serverConfig = null ;


    public LdapClientSession(ClientKey a_clientKey, LdapPrincipal a_principal)
    {
        m_clientKey = a_clientKey ;
        m_principal = a_principal ;
    }


    ////////////////////////////
    // Session Attribute APIs //
    ////////////////////////////


    public Object getAttribute(String an_attrName)
    {
        return m_attribs.get(an_attrName) ;
    }


    public void removeAttribute(String an_attrName)
    {
        m_attribs.remove(an_attrName) ;
    }


    public void setAttribute(String an_attrName, Object a_attrValue)
    {
        m_attribs.put(an_attrName, a_attrValue) ;
    }


    public Iterator getAttributeNames()
    {
        return m_attribs.keySet().iterator() ;
    }


    public void invalidate()
    {
        m_isValid = false ;
        m_attribs.clear() ;
        super.setChanged() ;
    }


    public ClientKey getClientKey()
    {
        return m_clientKey ;
    }


    public boolean isNew()
    {
        return m_isNew ;
    }


    public long getCreationTime()
    {
        return m_creationTime ;
    }


    public long getLastAccessedTime()
    {
        return m_lastAccessedTime ;
    }


    public int getMaxInactiveInterval()
    {
        return m_maxInactiveInterval ;
    }


    public void setMaxInactiveInterval(int a_maxInactiveInterval)
    {
        m_maxInactiveInterval = a_maxInactiveInterval ;
    }


    /////////////////////////
    // Session Client APIs //
    /////////////////////////


    public Locale getLocale()
    {
        return m_principal.getLocale() ;
    }


    public LdapPrincipal getPrincipal()
    {
        return m_principal ;
    }


    public void setPrincipal(LdapPrincipal a_principal)
    {
        m_principal = a_principal ;
    }


    public boolean isValid()
    {
        return m_isValid ;
    }


    void reset() {
        m_attribs.clear() ;
        m_isNew = true ;
        m_isValid = true ;
        m_maxInactiveInterval = MAX_INACTIVE_INTERVAL ;
        m_lastAccessedTime = System.currentTimeMillis() ;
    }
}
