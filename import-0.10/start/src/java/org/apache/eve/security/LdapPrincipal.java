/*
 * $Id: LdapPrincipal.java,v 1.1 2003/03/26 02:07:19 jmachols Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.security ;


import java.util.Locale ;
import javax.naming.Name ;
import java.security.Principal ;
import org.apache.ldap.common.name.LdapName ;


/**
 * Principal implementation with some extra methods to access the Principal's
 * locale and get the principal's name as a javax.naming.Name.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: jmachols $
 * @version $Revision: 1.1 $
 */
public class LdapPrincipal
    implements Principal
{
    public final static String ANONYMOUS = "" ;
    final Name m_name ;
    final Locale m_locale ;


    public LdapPrincipal()
    {
        this(new LdapName(), Locale.getDefault()) ;
    }


    public LdapPrincipal(Name a_name)
    {
        this(a_name, Locale.getDefault()) ;
    }


    public LdapPrincipal(Name a_name, Locale a_locale)
    {
        m_name = a_name ;
        m_locale = a_locale ;
    }


    public String getName()
    {
        return m_name.toString() ;
    }


    public Name getDn()
    {
        return m_name ;
    }


    public Locale getLocale()
    {
        return m_locale ;
    }
}
