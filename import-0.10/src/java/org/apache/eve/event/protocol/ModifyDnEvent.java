/*
 * $Id: ModifyDnEvent.java,v 1.3 2003/03/13 18:27:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import java.util.EventObject ;
import javax.naming.Name;
import javax.naming.directory.Attributes;


/**
 * Event which logically represents a protocol modifyDn operation.
 */
public class ModifyDnEvent
    extends ProtocolEvent
{
    /** The distinguished name of the entry modified. */
	private Name m_dn = null ;
    /** The new relative distinguished name of the entry modified. */
	private Name m_rdn = null ;
    /** The new base distinguished name of the entry to be moved. */
	private Name m_newBase = null ;
    /** The attributes of the entry being modified. */
    private Attributes m_attributes = null ;


    /**
     * Creates an uninitialized ModifyDnEvent.
     */
    public ModifyDnEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, MODDNREQUEST_MASK, a_isPduDelivered) ;
    }


    /**
     * Sets the distinguished name of entry to move.
     *
     * @param a_dn the distinguished name of the entry to move.
     */
    public void setName(Name a_dn)
    {
        if(null == m_dn) {
	        m_dn = a_dn ;
        } else {
            throw new IllegalStateException("Can't call setName() more than "
                + "once!") ;
        }
    }


    /**
     * Gets the distinguished name of the entry to change the dn of.
     *
     * @return the dn of the entry to move.
     */
    public Name getName()
    {
        if(null == m_dn) {
            throw new IllegalStateException("ModifyDnEvent has not been"
                + "properly initialized!") ;
        }

        return m_dn ;
    }


    /**
     * Sets the new relative name of entry to move.
     *
     * @param a_rdn the relative name of the entry to move.
     */
    public void setRdn(Name a_rdn)
    {
        if(null == m_rdn) {
	        m_rdn = a_rdn ;
        } else {
            throw new IllegalStateException("Can't call setRdn() more than "
                + "once!") ;
        }
    }


    /**
     * Gets the relative name of the entry to change the dn of.
     *
     * @return the rdn of the entry to move.
     */
    public Name getRdn()
    {
        if(null == m_rdn) {
            throw new IllegalStateException("ModifyDnEvent has not been"
                + "properly initialized!") ;
        }

        return m_rdn ;
    }


    /**
     * Sets the distinguished name of entry to move.
     *
     * @param a_newBase the distinguished name of the entry to move.
     */
    public void setBaseName(Name a_newBase)
    {
        if(null == m_dn) {
	        m_newBase = a_newBase ;
        } else {
            throw new IllegalStateException("Can't call setBaseName() more than"
                + " once!") ;
        }
    }


    /**
     * Gets the new base distinguished name of the entry to move the entry
     * under.
     *
     * @return the base dn to move the entry to.
     */
    public Name getBaseName()
    {
        if(null == m_newBase) {
            throw new IllegalStateException("ModifyDnEvent has not been"
                + "properly initialized!") ;
        }

        return m_newBase ;
    }


	/**
     * Gets the Attributes of the entry is about to have its DN modified.
     * This property is available for both before and after operations event
     * firings.
     *
     * @return the entry Attributes
     */
    public Attributes getAttributes()
    {
        if(null == m_attributes) {
            throw new IllegalStateException("ModifyDnEvent has not been"
                + "properly initialized!") ;
        }

        return m_attributes ;
    }


    /**
     * Sets the events attributes for the entry that is about to or already has
     * been modified by the server.  This property is available for both before
     * and after operations event firings.
     * 
     * @param a_attributes the attributes for the modified entry.
     */
	public void setAttributes(Attributes a_attributes)
    {
        if(null == m_attributes) {
	        m_attributes = a_attributes ;
        } else {
            throw new IllegalStateException("The attributes of this event "
                + " may only be set at most one time.") ;
        }
    }
}
