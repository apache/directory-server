/*
 * $Id: SearchHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */


package org.apache.eve.protocol ;


import java.util.Iterator ;
import java.util.HashSet;
import java.util.Collections;

import java.io.IOException ;
import java.io.ByteArrayInputStream ;
import java.rmi.Naming;

import javax.naming.Name ;
import javax.naming.NamingException;

import org.apache.ldap.common.Lockable ;
import org.apache.ldap.common.message.Response ;
import org.apache.ldap.common.message.SearchRequest ;
import org.apache.ldap.common.message.ResultCodeEnum ;
import org.apache.ldap.common.message.MessageTypeEnum ;
import org.apache.ldap.common.message.LockableAttribute ;
import org.apache.ldap.common.message.LockableAttributes ;
import org.apache.ldap.common.message.SearchResponseDone ;
import org.apache.ldap.common.message.SearchResponseEntry ;
import org.apache.ldap.common.message.LockableAttributeImpl ;
import org.apache.ldap.common.message.SearchResponseDoneImpl ;
import org.apache.ldap.common.message.LockableAttributesImpl ;
import org.apache.ldap.common.message.SearchResponseEntryImpl ;

import org.apache.eve.backend.Cursor ;
import org.apache.eve.client.ClientKey ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.ldap.common.message.ScopeEnum ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.schema.Schema;


/**
 * SearchRequest handler.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class SearchHandler
    implements RequestHandler
{
    /** The protocol module this handler is used by */
    private final ProtocolModule m_module ;


    // ------------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------------


    /**
     * Creates a SearchRequest handler instance for use by a ProtocolModule.
     * 
     * @param a_module the ProtocolModule this instance is created for.
     */
    public SearchHandler( ProtocolModule a_module )
    {
        m_module = a_module ;
    }


    // ------------------------------------------------------------------------
    // SearchHandler's Primary Handling Method
    // ------------------------------------------------------------------------


    /**
     * Specifically designed handler method for processing SearchRequests
     *
     * @param a_request the SearchRequest to handle
     */
    public void handle( SearchRequest a_request )
    {
    	Name l_base = null ;
        UnifiedBackend l_nexus = m_module.getNexus() ;

        try
        {
            // Get the normalized search base as a Name
            l_base = m_module.getName( a_request.getBase() ) ;

            // Return NOSUCHOBJECT done response if the search base is not there
            if( ! l_nexus.hasEntry( l_base ) )
            {
                sendSearchDone(a_request, ResultCodeEnum.NOSUCHOBJECT, l_base,
                	"Search base not found.", null) ;
            }

            // Get a cursor over the  search results
            Cursor l_cursor = l_nexus.search( a_request.getFilter(), l_base,
                a_request.getScope().getValue() ) ;

            // For each LdapEntry send back a SearchResponseEntry PDU to client
            while( l_cursor.hasMore() )
            {
                LdapEntry l_entry = ( LdapEntry ) l_cursor.next() ;

                // Some backends make the suffix a parent/child of itself
                // so we need to catch a return of the suffix if the base
                // in a single scoped search.
                if( l_entry.getNormalizedDN().equals( l_base )
                    && a_request.getScope() == ScopeEnum.SINGLELEVEL )
                {
                    continue ;
                }

                sendEntry( a_request, l_entry ) ;
            }

            // Complete search with a successful SearchResponseDone PDU
            sendSearchDone( a_request, l_base ) ;
        }
        catch( Throwable t )
        {
            // Sent a search response done to handle any exceptional conditions
            // that may have been encountered during search request handling.
            sendSearchDone( a_request, t ) ;
        }
    }


    // ------------------------------------------------------------------------
    // RequestHandler Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the handler type for this RequestHandler.
     *
     * @return HandlerTypeEnum.SEARCH always
     */
    public HandlerTypeEnum getHandlerType()
    {
        return HandlerTypeEnum.SEARCH ;
    }


    /**
     * Gets the message type this handler is designed to respond to.
     *
     * @return MessageTypeEnum.SEARCHREQUEST always.
     */
    public MessageTypeEnum getRequestType()
    {
        return MessageTypeEnum.SEARCHREQUEST ;
    }


    // ------------------------------------------------------------------------
    // Utility Methods Used by Search Handler Method
    // ------------------------------------------------------------------------


    /**
     * Gets the attributes view of the entry by copying LdapEntry attributes and
     * their values into a LockableAttributes object.
     *
     * @param  req the search request
     * @param a_parent the Lockable parent used by the LockableAttributes
     * @param a_entry the entry to transform into a LockableAttributes instance
     * @return the LockableAttributes representation of the LdapEntry
     */
    private LockableAttributes
        getAttributes( SearchRequest req, Lockable a_parent, LdapEntry a_entry )
    {
		// Create the LockableAttributes using the default implementation
        LockableAttributes l_attributes =
            new LockableAttributesImpl( a_parent ) ;
        Schema schema = null ;

        try
        {
            Name ndn = a_entry.getNormalizedDN() ;
            schema = m_module.getNexus().getSchema(ndn) ;
        }
        catch( NamingException e )
        {
            m_module.getLogger().error("dn normalization failed", e) ;
        }

        // Iterate through the set of attributes in the entry.
        Iterator l_list = a_entry.attributes().iterator() ;
        while( l_list.hasNext() )
        {
            // Get the name/id of the attribute and create a LockableAttribute
            // using the default implementation and add it to the attributes
            String l_id = ( String ) l_list.next() ;

            // build a hashtable of requested ids
            HashSet requestedIds = new HashSet() ;
            requestedIds.addAll( req.getAttributes() ) ;

            LockableAttribute l_attribute = null ;

            if ( requestedIds.size() == 0 && schema.isOperational(l_id) )
            {
                continue ;
            }
            else if ( requestedIds.size() != 0 && !requestedIds.contains(l_id) )
            {
                continue ;
            }

            l_attribute = new LockableAttributeImpl( l_attributes, l_id ) ;
        	l_attributes.put( l_attribute ) ;

            // Add the values of the attribute in the entry to the Attribute
            Iterator l_values = a_entry.getMultiValue( l_id ).iterator() ;
            while( l_values.hasNext() )
            {
                l_attribute.add( l_values.next() ) ;
            }
        }

        return l_attributes ;
    }


    /**
     * Sends a SearchResponseEntry containing the attributes of an LdapEntry to
     * the client.
     *
     * @param a_request the request to respond to
     * @param a_entry the LdapEntry to send in the SearchResponseEntry PDU
     */
    private void sendEntry( SearchRequest a_request, LdapEntry a_entry )
    {
        SearchResponseEntry l_response =
            new SearchResponseEntryImpl( a_request.getMessageId() ) ;
        l_response.setObjectName( a_entry.getEntryDN() ) ;
        l_response.setAttributes( getAttributes( a_request,
                l_response, a_entry ) ) ;
        transmit( l_response ) ;
    }


    /**
     * Sends a SearchResponseDone for a specific error using its result code and
     * error message.
     * 
     * @param a_request the request to respond to
     * @param a_resultCode the LdapResult result code enumeration to use
     * @param a_dn the search base which is used to determine the matching Dn
     * @param a_msg the custom error message to send
     * @param a_error the error causing the negative response to the client
     */
    private void sendSearchDone( SearchRequest a_request,
        ResultCodeEnum a_resultCode, Name a_dn, String a_msg,
        Throwable a_error )
    {
        SearchResponseDone l_response =
            new SearchResponseDoneImpl( a_request.getMessageId() ) ;
        m_module.setResult( l_response, a_resultCode, a_dn, a_msg, a_error ) ;
        transmit( l_response ) ;
    }


    /**
     * Sends a SearchResponseDone for a specific error the result code and
     * error messages may be generated from the type of error that results.  In
     * a sense this overload is the generic exception handler for this request
     * handler.
     * 
     * @param a_request the request to respond to
     * @param a_error the error causing the negative response to the client
     */
    private void sendSearchDone( SearchRequest a_request, Throwable a_error )
    {
        SearchResponseDone l_response =
            new SearchResponseDoneImpl( a_request.getMessageId() ) ;
        m_module.setResult( l_response, null, a_error ) ;
        transmit( l_response ) ;
    }


    /**
     * Sends a successful SearchResponseDone.
     * 
     * @param a_request the request to respond to
     * @param a_dn the search base which is used to determine the matching Dn
     */
    private void sendSearchDone( SearchRequest a_request, Name a_dn )
    {
        SearchResponseDone l_response =
            new SearchResponseDoneImpl( a_request.getMessageId() ) ;
        m_module.setResult( l_response, a_dn ) ;
        transmit( l_response ) ;
    }


    /**
     * Synchronously transmits a response to the client using special
     * synchronous interfaces on the Encoder and OutputManager.
     *
     * @param a_response the response to transmit to the client.
     */
    private void transmit( Response a_response )
    {
        byte l_buf[] = m_module.getEncoder().encode( a_response ) ;
        ByteArrayInputStream l_in = new ByteArrayInputStream( l_buf ) ;
        ClientKey l_clientKey = m_module.getClientManager().getClientKey() ;

        try
        {
            m_module.getOutputManager().write( l_clientKey, l_in ) ;
        }
        catch( IOException ioe )
        {
            m_module.getClientManager().drop( l_clientKey ) ;
            m_module.getLogger().error(
                "Failed to transmit pdu to client " + l_clientKey.toString()
                + ". Client has been dropped." ) ;
        }
    }
}
