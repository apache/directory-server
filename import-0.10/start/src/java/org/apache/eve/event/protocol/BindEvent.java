/*
 * $Id: BindEvent.java,v 1.3 2003/03/13 18:27:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import javax.naming.Name ;
import java.security.Principal;


/**
 * Event which logically represents a protocol bind operation.  Some properties
 * of this event will only be present after the bind operation has completed.
 * Changes to already initialized mutable event properties are not possible
 * after.
 */
public class BindEvent
    extends ProtocolEvent
{
    private Name m_dn ;
    private Boolean isSimple = null ;
	private Principal m_principal = null ;


    /**
     * Minimally creates a BindEvent without populating it with the information
     * required to fire the event.
     */
    public BindEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, BINDREQUEST_MASK, a_isPduDelivered) ;
    }


    /**
     * Gets the unnormalized distinguished name of the user trying to bind.
     * This parameter is available on both before and after event firings.
     *
     * @return the unnormalized distinguished name of the user
     */
    public Name getName()
    {
        if(null == m_dn) {
            throw new IllegalStateException("BindEvent has not been properly "
                + "initialized!") ;
        }

        return m_dn ;
    }


    /**
     * Sets the unnormalized distinguished name of the user trying to bind.
     * This parameter is available on both before and after event firings.
     *
     * @param a_dn the unnormalized distinguished name of the user
     */
    public void setName(Name a_dn)
    {
        if(null == m_dn) {
        	m_dn = a_dn ;
        } else {
            throw new IllegalStateException("Name change via setName() "
                + "after propertiy initialization not allowed.  setName() can "
                + "be called at most once.") ;
        }
    }


    /**
     * Checks to see if the authentication method is simple.  This event
     * property is available on both before and after event firings.
     *
     * @return true if the authentication method is simple, false if it is SASL
     */
    public boolean isSimple()
    {
        if(null == isSimple) {
            throw new IllegalStateException("BindEvent has not been properly "
                + "initialized!") ;
        }

		return isSimple.booleanValue() ;
    }


    /**
     * Checks to see if the authentication method is simple.  This event
     * property is available on both before and after event firings.
     *
     * @return true if the authentication method is simple, false if it is SASL
     */
    public boolean getSimple()
    {
        if(null == isSimple) {
            throw new IllegalStateException("BindEvent has not been properly "
                + "initialized!") ;
        }

		return isSimple.booleanValue() ;
    }


    /**
     * Checks to see if the authentication method is simple.  This event
     * property is available on both before and after event firings.
     *
     * @param isSimple true if the authentication method is simple, false if it
     * is SASL
     */
    public void setSimple(boolean isSimple)
    {
        if(null == this.isSimple) {
        	this.isSimple = new Boolean(isSimple) ;
        } else {
            throw new IllegalStateException("Name change via setSimple() "
                + "after propertiy initialization not allowed.  setName() can "
                + "be called at most once.") ;
        }
    }


    /**
     * Gets the user principal that has just bound a session with the server.
     * This event property is only available after the bind operation and can
     * only be set once.
     *
     * @return the authenticated user.
     */
    public Principal getPrincipal()
    {
        if(null == m_principal) {
            throw new IllegalStateException("BindEvent has not been properly "
                + "initialized!\nThis will be the case if this property is "
                + "accessed before the operation has occurred.") ;
        }

		return m_principal ;
    }


    /**
     * Sets the user principal that has just bound a session with the server.
     * This event property is only available after the bind operation.
     *
     * @param a_principal the authenticated user.
     */
    public void setPrincipal(Principal a_principal)
    {
        if(null == m_principal) {
        	m_principal = a_principal ;
        } else {
            throw new IllegalStateException("Prinicipal change via "
                + "setPrincipal() after propertiy initialization not allowed. "
                + "\nsetPrincipal() can be called at most once.") ;
        }
    }
}
