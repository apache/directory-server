/*
 * $Id: DelEvent.java,v 1.3 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import javax.naming.Name ;
import javax.naming.directory.Attributes ;


/**
 * Event which logically represents a protocol del operation.
 */
public class DelEvent
    extends ProtocolEvent
{
    /** The distinguished name of the entry to remove. */
    private Name m_dn = null ;
    /** The attributes of the entry to remove */
    private Attributes m_attributes = null ;


    /**
     * Creates an uninitialized DelEvent.
     *
     * @param a_src the source of the event.
     * @param a_isPduDelivered true if this event represents a physical PDU
     * delivery or is a logical representation of the operation through the
     * JNDI provider.
     */
    public DelEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, DELREQUEST_MASK, a_isPduDelivered) ;
    }


    /**
     * Sets the distinguished name of entry to remove.
     *
     * @param a_dn the distinguished name of the entry to remove.
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
     * Gets the distinguished name of the entry to remove.
     *
     * @return the dn of the entry to remove.
     */
    public Name getName()
    {
        if(null == m_dn) {
            throw new IllegalStateException("DelEvent has not been properly "
                + "initialized!") ;
        }

        return m_dn ;
    }


	/**
     * Gets the Attributes of the entry is about to be removed or was removed.
     * This property is available for both before and after operations event
     * firings.
     *
     * @return the entry Attributes
     */
    public Attributes getAttributes()
    {
        if(null == m_attributes) {
            throw new IllegalStateException("DelEvent has not been properly "
                + "initialized!") ;
        }

        return m_attributes ;
    }


    /**
     * Sets the events attributes for the entry that is about to or already has
     * been deleted from the server.  This property is available for both before
     * and after operations event firings.
     * 
     * @param a_attributes the attributes for the deleted entry.
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
