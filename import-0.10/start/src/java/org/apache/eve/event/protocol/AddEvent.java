/*
 * $Id: AddEvent.java,v 1.4 2003/04/09 15:51:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import javax.naming.directory.Attributes ;
import javax.naming.Name;
import javax.naming.directory.DirContext;


/**
 * Event which logically represents a protocol add operation.  Changes to the
 * set of attributes for the added entry in pre-event firing will effect the
 * result.  Meaning extra attributes and values or their deletion will be
 * reflected in the added entry.
 */
public class AddEvent
    extends ProtocolEvent
{
    private Name m_name ;
	private Attributes m_attributes ;
    private DirContext m_base ;


    public AddEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, ADDREQUEST_MASK, a_isPduDelivered) ;
    }


    public DirContext getBase()
    {
        return m_base ;
    }


    public void setBase(DirContext a_base)
    {
        m_base = a_base ;
    }


    public Name getName()
    {
        return m_name ;
    }


    public void setName(Name a_name)
    {
        m_name = a_name ;
    }


    public Attributes getAttributes()
    {
        return m_attributes ;
    }


    public void setAttributes(Attributes a_attribute)
    {
        m_attributes = a_attribute ;
    }
}
