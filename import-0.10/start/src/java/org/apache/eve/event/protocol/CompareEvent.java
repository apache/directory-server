/*
 * $Id: CompareEvent.java,v 1.3 2003/03/13 18:27:26 akarasulu Exp $
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
 * Event which logically represents a protocol compare operation.  The events
 * properties are all initialized for both the before and after event firings
 * available to both event listener methods.  Alteration attempts to event
 * properties result in a IllegalStateException since they can only be set once.
 */
public class CompareEvent
    extends ProtocolEvent
{
    /** The Dn of the entry compared */
	private Name m_dn = null ;
    /** The attributes in the entry whose values are compared. */
	private Attributes m_attributes = null ;
    /** The name of the attribute to compare */
    private String m_attrId = null ;
    /** The value to use in the attribute comparison */
    private Object m_attrValue = null ;


    /**
     * Creates a baseline uninitialized ComareEvent.
     *
     * @param a_src the source of the event.
     * @param a_isPduDelivered whether or not an actual physical compare pdu
     * caused this event.
     */
    public CompareEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, COMPAREREQUEST_MASK, a_isPduDelivered) ;
    }


    /**
     * Sets the distinguished name of entry to compare.
     *
     * @param a_dn the distinguished name of the entry to compare.
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
     * Gets the distinguished name of the entry to compare.
     *
     * @return the dn of the entry to compare.
     */
    public Name getName()
    {
        if(null == m_dn) {
            throw new IllegalStateException("CompareEvent has not been properly "
                + "initialized!") ;
        }

        return m_dn ;
    }


	/**
     * Gets the Attributes of the entry is about to be compared.
     * This property is available for both before and after operations event
     * firings.
     *
     * @return the entry Attributes
     */
    public Attributes getAttributes()
    {
        if(null == m_attributes) {
            throw new IllegalStateException("CompareEvent has not been properly "
                + "initialized!") ;
        }

        return m_attributes ;
    }


    /**
     * Sets the events attributes for the entry that is about to or already has
     * been compared by the server.  This property is available for both before
     * and after operation event firings.
     * 
     * @param a_attributes the attributes for the compared entry.
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


    /**
     * Gets the value of the attribute to use in this comparison.
     *
     * @return the value of the attribute to compare with the entry.
     */
	public Object getAttributeValue()
    {
        if(null == m_attrValue) {
            throw new IllegalStateException("CompareEvent has not been properly"
                + " initialized!") ;
        }

        return this.m_attrValue ;
    }


    /**
     * Gets the name or Id of the attribute used in this comparison.
     *
     * @return the name or id of the comparison attribute.
     */
    public String getAttributeId()
    {
        if(null == m_attrId) {
            throw new IllegalStateException("CompareEvent has not been properly"
                + " initialized!") ;
        }

        return this.m_attrId ;
    }


    /**
     * Sets the value of the attribute to use in this comparison.
     *
     * @param a_value the value of the attribute to compare with the entry.
     */
	public void setAttributeValue(Object a_value)
    {
        if(null == m_attrValue) {
	        m_attrValue = a_value ;
        } else {
            throw new IllegalStateException("The attributes of this event "
                + " may only be set at most one time.") ;
        }
    }


    /**
     * Sets the name or Id of the attribute used in this comparison.
     *
     * @return the name or id of the comparison attribute.
     */
    public void setAttributeId(String a_attrId)
    {
        if(null == m_attrId) {
	        m_attrId = a_attrId ;
        } else {
            throw new IllegalStateException("The attributes of this event "
                + " may only be set at most one time.") ;
        }
    }
}
