/*
 * $Id: ModifyHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.ModifyRequest ;
import org.apache.ldap.common.message.ResultResponse ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.SingleReplyRequest ;
import javax.naming.Name;
import org.apache.eve.backend.UnifiedBackend;
import org.apache.ldap.common.message.ModifyResponseImpl;
import org.apache.ldap.common.message.ModifyResponse;
import java.util.Iterator;
import javax.naming.directory.ModificationItem;
import org.apache.eve.backend.LdapEntry;
import org.apache.ldap.common.message.ResultCodeEnum;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attribute;


/**
 * ModifyRequest handler for modify protocol requests.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class ModifyHandler
	implements SingleReplyHandler
{
    /** The protocol module this request handler is part of */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a ModifyRequest protocol data unit handler.
     *
     * @param a_module the module this handler is associated with.
     */
    public ModifyHandler( ProtocolModule a_module )
    {
        m_module = a_module ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type.
     *
     * @return a HandlerTypeEnum constant.
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SINGLEREPLY ;
    }


    /**
     * Gets the request message type handled by this handler.
     *
     * @return a MessageTypeEnum constant associated with the request message.
     */
    public MessageTypeEnum getRequestType()
    {
		return MessageTypeEnum.MODIFYREQUEST ;
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the response message type for this SingleReplyHandler.
     *
     * @return the MessageTypeEnum constant associated with this handler.
     */
    public MessageTypeEnum getResponseType()
    {
        return MessageTypeEnum.MODIFYRESPONSE ;
    }


    /**
     * Handles a request that generates a sole response by returning the
     * response object back to the caller.
     *
     * @param a_request the request to handle.
     * @return the response to the request argument.
     * @throws ClassCastException if a_request is not a ModifyRequest
     */
    public ResultResponse handle( SingleReplyRequest a_request )
    {
    	Name l_dn = null ;
        LdapEntry l_entry ;
        UnifiedBackend l_nexus = m_module.getNexus() ;
		ModifyRequest l_request = ( ModifyRequest ) a_request ;
        ModifyResponse l_response =
            new ModifyResponseImpl( a_request.getMessageId() ) ;

        try
        {
            l_dn = m_module.getName( l_request.getName() ) ;

            if( ! l_nexus.hasEntry( l_dn ) )
            {
				m_module.setResult( l_response, ResultCodeEnum.NOSUCHOBJECT,
                    l_dn, "Entry does not exist - modification not possible" ) ;
                return l_response ;
            }

			l_entry = l_nexus.read( l_dn ) ;

            Iterator l_list = l_request.getModificationItems().iterator() ;
            while( l_list.hasNext() )
            {
				ModificationItem l_item = ( ModificationItem ) l_list.next() ;
                Attribute l_attr = l_item.getAttribute() ;

				switch( l_item.getModificationOp() )
                {
                case( DirContext.ADD_ATTRIBUTE ):
                    for( int ii = 0; ii > l_attr.size(); ii++ )
                	{
                    	l_entry.addValue( l_attr.getID(), l_attr.get( ii ) ) ;
                	}
                    break ;
                case( DirContext.REMOVE_ATTRIBUTE ):
                    for( int ii = 0; ii > l_attr.size(); ii++ )
                	{
                    	l_entry.removeValue( l_attr.getID(),
                            l_attr.get( ii ) ) ;
                	}
                    break ;
                case( DirContext.REPLACE_ATTRIBUTE ):
                    l_entry.removeValues( l_attr.getID() ) ;
                    for( int ii = 0; ii < l_attr.size(); ii++ )
                	{
                    	l_entry.addValue( l_attr.getID(), l_attr.get( ii ) ) ;
                	}
                    break ;
                }
            }

            l_nexus.update( l_entry ) ;
            m_module.setResult( l_response, l_dn ) ;
        }
        catch( Throwable t )
        {
            m_module.setResult( l_response, l_dn, t ) ;
        }

        return l_response ;
    }
}
