/*
 * $Id: ExtendedEvent.java,v 1.3 2003/03/13 18:27:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import java.util.EventObject ;


/**
 * Event which logically represents a protocol extended operation.
 */
public class ExtendedEvent
    extends ProtocolEvent
{
    /** The OID of the extended operation */
	private String m_oid = null ;
    /** The binary payload of the extended request */
    private byte [] m_payload = null ;


    /**
     * Creates an uninitialized ExtendedEvent.
     * 
     * @param a_src the source of the event.
     * @param a_isPduDelivered true if this event represents a physical PDU
     * delivery or is a logical representation of the operation through the
     * JNDI provider.
     */
    public ExtendedEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, EXTENDEDREQ_MASK, a_isPduDelivered) ;
    }


    /**
     * Gets the extended operation's Oid.
     *
     * @return the OID of the extended operation.
     */
	public String getOid()
    {
        if(null == m_oid) {
            throw new IllegalStateException("ExtendedEvent has not been "
                + "properly initialized!") ;
        }

        return m_oid ;
    }


    /**
     * Sets the extended operation's Oid.
     *
     * @param a_oid the OID of the extended operation.
     */
	public void setOid(String a_oid)
    {
        if(null == m_oid) {
        	m_oid = a_oid ;
        } else {
            throw new IllegalStateException("Can't call setOid() more than "
                + "once!") ;
        }
    }


    /**
     * Gets the payload of the extended operation.
     *
     * @return the payload of the extended request
     */
    public byte [] getPayload()
    {
        if(null == m_payload) {
            throw new IllegalStateException("ExtendedEvent has not been "
                + "properly initialized!") ;
        }

        return m_payload ;
    }


    /**
     * Sets the payload of the extended operation.
     *
     * @param a_payload the payload of the extended request
     */
    public void setPayload(byte [] a_payload)
    {
        if(null == m_payload) {
	        m_payload = a_payload ;
        } else {
            throw new IllegalStateException("Can't call setPayload() more than "
                + "once!") ;
        }
    }
}
