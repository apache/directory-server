/*
 * $Id: SearchEvent.java,v 1.3 2003/03/13 18:27:29 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.event.protocol ;


import javax.naming.Name ;
import javax.naming.directory.Attributes ;
import javax.naming.directory.SearchControls ;
import org.apache.ldap.common.filter.ExprNode ;


/**
 * Event which logically represents a protocol search operation.
 */
public class SearchEvent
    extends ProtocolEvent
{
    /** The search base as a name */
	private Name m_base = null ;
    /** The search filter as an expression tree or AST */
    private ExprNode m_filter = null ;
    /** The attributes of the search base */
    private Attributes m_attributes = null ;
    /** The search controls associated with the search */
	private SearchControls m_controls = null ;


    /**
     * Creates an uninitialized SearchEvent.
     *
     * @param a_src the source of the event.
     * @param a_isPduDelivered true if this event represents a physical PDU
     * delivery or is a logical representation of the operation through the
     * JNDI provider.
     */
    public SearchEvent(Object a_src, boolean a_isPduDelivered)
    {
       super(a_src, SEARCHREQUEST_MASK, a_isPduDelivered) ;
    }


    /**
     * Sets the distinguished name of search base.
     *
     * @param a_base the distinguished name of the search base.
     */
    public void setBase(Name a_base)
    {
        if(null == m_base) {
	        m_base = a_base ;
        } else {
            throw new IllegalStateException("Can't call setBase() more than "
                + "once!") ;
        }
    }


    /**
     * Gets the distinguished name of the search base.
     *
     * @return the dn of the search base.
     */
    public Name getBase()
    {
        if(null == m_base) {
            throw new IllegalStateException("SearchEvent has not been properly "
                + "initialized!") ;
        }

        return m_base ;
    }


    /**
     * Gets the root ExprNode for the search filter as a AST.
     *
     * @return the root ExprNode
     */
    public ExprNode getFilter()
    {
        if(null == m_filter) {
            throw new IllegalStateException("SearchEvent has not been properly "
                + "initialized!") ;
        }

        return m_filter ;
    }


    /**
     * Sets the root ExprNode for the search filter as a AST.
     *
     * @param a_filter the root ExprNode
     */
    public void setFilter(ExprNode a_filter)
    {
        if(null == m_filter) {
			m_filter = a_filter ;
        } else {
            throw new IllegalStateException("The filter of this event "
                + " may only be set at most one time.") ;
        }
    }


    /**
     * Gets the specific search controls associated with this search operation.
     *
     * @return the search controls for this search.
     */
    public SearchControls getSearchControls()
    {
        if(null == m_controls) {
            throw new IllegalStateException("SearchEvent has not been properly "
                + "initialized!") ;
        }

        return m_controls ;
    }


    /**
     * Sets the specific search controls associated with this search operation.
     *
     * @param a_ctls the search controls associated with this search operation
     */
    public void setSearchControls(SearchControls a_ctls)
    {
        if(null == m_controls) {
			m_controls = a_ctls ;
        } else {
            throw new IllegalStateException("The search controls of this event "
                + " may only be set at most one time.") ;
        }
    }
}
