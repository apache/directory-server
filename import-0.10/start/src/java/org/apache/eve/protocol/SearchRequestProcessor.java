/*
 * $Id: SearchRequestProcessor.java,v 1.22 2003/08/22 21:15:56 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


/**
 *
 */
public class SearchRequestProcessor
//    extends BaseRequestProcessor
{
    /** Response type of this request processor class 
    public static final int RES_TYPE = LDAPMessageChoice.SEARCHRESDONE_CID ;

    private EventManager m_eventManager = null ;
    private ExprNode m_exprNode = null ;
    private Name m_baseDN = null ;
	private UnifiedBackend m_nexus = null ;
    private Encoder m_encoder = null ;
    private OutputManager m_outputManager = null ;
    private LDAPMessage m_response = null ;


    /**
     * Creates and initializes the request processor.
     *
    public SearchRequestProcessor(ClientKey a_client, LDAPMessage a_request)
    {
        super(a_client, a_request, RES_TYPE) ;
    }


    /**
     * Processes the request.
     * dummy to test protocol communication
     * @see org.apache.eve.protocol.RequestProcessor#process()
     *
    public LDAPMessage process()
    {
        try {
            SearchEvent l_event = null ;
            m_baseDN = m_nexus.getNormalizedName(
                new String( m_request.protocolOp.searchRequest.baseObject ) ) ;
            Filter l_filter = m_request.protocolOp.searchRequest.filter ;
            m_exprNode = ExprTreeComposer.compose( l_filter ) ;
			SearchControls l_controls = getSearchControls() ;

            if ( m_eventManager.hasListener( 
                ProtocolEvent.SEARCHREQUEST_MASK ) ) 
            {
                l_event = new SearchEvent( this, true ) ;
                l_event.setBase( m_baseDN ) ;
                l_event.setFilter( m_exprNode ) ;
                l_event.setSearchControls( l_controls ) ;
                m_eventManager.fireBefore( l_event ) ;
			} 
            else if ( getLogger().isDebugEnabled() ) 
            {
				getLogger().debug( 
                    "No event listeners for search operations are"
					+ " enabled - SearchEvent not constucted - not fired before"
                    + " operation!" ) ;
			}

            if ( getLogger().isDebugEnabled() ) 
            {
                StringBuffer l_buf = new StringBuffer() ;
                m_exprNode.printToBuffer( l_buf ) ;
                getLogger().debug( "Entry DN used for search base = '"
                    + m_baseDN + "'" ) ;
                getLogger().debug( "ASN1 Filter = " + l_filter ) ;
                getLogger().debug( "m_exprNode = " + l_buf.toString() ) ;
                getLogger().debug( "Search scope = " +
                    getScopeString( m_request.protocolOp.
                    	searchRequest.scope.value ) ) ;
            }

            Cursor l_cursor = m_nexus.search( m_exprNode, m_baseDN,
                l_controls.getSearchScope() ) ;
            LdapEntry l_candidate = null ;
            
            while ( l_cursor.hasMore() ) 
            {
                LdapEntry l_entry = ( LdapEntry ) l_cursor.next() ;

				// Filters out special case where some backends return the
                // base of a single scope search since they map themselves
                // as their own children.  According to the ldap spec we
                // should not be returning the base on a single scope search.

                if ( l_controls.getSearchScope() == Backend.SINGLE_SCOPE &&
                    l_entry.getNormalizedDN().equals( m_baseDN ) )
                {
                    if ( getLogger().isDebugEnabled() ) 
                    {
                        getLogger().debug( "Not returning base entry with dn '"
                            + l_entry.getNormalizedDN()
                            + "' in single scope search." ) ;
                    }

					continue ;
                } 
                else if ( getLogger().isDebugEnabled() ) 
                {
                        getLogger().debug( "Returning entry with dn '"
                            + l_entry.getNormalizedDN()
                            + "' in single scope search with base '"
                            + m_baseDN + "'" ) ;
                }

                if ( getLogger().isDebugEnabled() ) 
                {
                    getLogger().debug( getMessageKey()
                        + " - SearchRequestProcessor.process(): sending entry "
                        + l_entry.getEntryDN() + " to client" ) ;
                }

                //if(l_entry.isRefferal()) {
                //    sendReference(l_entry) ;
                //} else {
                    sendEntry(l_entry) ;
                //}
            }

			if ( l_event != null ) 
            {
                m_eventManager.fireAfter(l_event) ;
			} 
            else if ( getLogger().isDebugEnabled() ) 
            {
				getLogger().debug(
                    "No event listeners for search operations are"
					+ " enabled - SearchEvent not constucted - not fired after"
                    + " operation!" ) ;
			}

            return sendDone() ;
        } 
        catch ( IllegalArgumentException e ) 
        {
            String l_errMsg = "Naming context not found!" ;

            if ( getLogger().isDebugEnabled() ) 
            {
                l_errMsg += "\n" + ExceptionUtil.printStackTrace( e ) ;
            }

			return sendDone( LDAPResultEnum.NAMINGVIOLATION, l_errMsg ) ;
        } 
        catch ( Exception e ) 
        {
            getLogger().debug( "Failed search request:\n" 
                + m_request + "\n", e ) ;
            m_response = new LDAPMessage() ;
			m_response.messageID = m_request.messageID ;
            m_response.protocolOp = new LDAPMessageChoice() ;
            m_response.protocolOp.choiceId = 
                LDAPMessageChoice.SEARCHRESDONE_CID ;
            m_response.protocolOp.searchResDone = new SearchResultDone() ;
            m_response.protocolOp.searchResDone.matchedDN =
                m_request.protocolOp.searchRequest.baseObject ;
            m_response.protocolOp.searchResDone.resultCode = 
                new LDAPResultEnum() ;
            m_response.protocolOp.searchResDone.resultCode.value =
                LDAPResultEnum.OPERATIONSERROR ;
            m_response.protocolOp.searchResDone.errorMessage =
                "failed on search request!".getBytes() ;
            return m_response ;
        }
    }


	public String [] getAttributesToReturn()
    {
		AttributeDescriptionList l_adl =
			m_request.protocolOp.searchRequest.attributes ;
        String [] l_attribsToReturn = new String [l_adl.size()] ;
		
        for ( int ii = 0 ; ii < l_adl.size(); ii++ ) 
        {
            l_attribsToReturn[ii] = 
                new String( ( byte [] ) l_adl.elementAt( ii ) ) ;
        }

        return l_attribsToReturn ;
    }


    public String getScopeString( int a_scope )
    {
		switch ( a_scope ) 
        {
		case( SearchRequestEnum.BASEOBJECT ):
			return "BASE" ;
		case( SearchRequestEnum.SINGLELEVEL ):
			return "SINGLE" ;
		case( SearchRequestEnum.WHOLESUBTREE ):
			return "SUBTREE" ;
        default:
            throw new IllegalArgumentException( 
            "The scope arg to getScopeString"
            + " must be a SearchRequestEnum value of BASEOBJECT, SINGLELEVEL,"
            + " or WHOLESUBTREE." ) ;
		}
    }


    public SearchControls getSearchControls()
    {
        SearchControls l_ctls = new SearchControls() ;

        if ( null != m_request.protocolOp.searchRequest.sizeLimit ) 
        {
			l_ctls.setCountLimit( m_request.protocolOp.searchRequest.
				sizeLimit.longValue() ) ;
        }

        if ( null != m_request.protocolOp.searchRequest.derefAliases ) 
        {
            if ( m_request.protocolOp.searchRequest.derefAliases.value
                == SearchRequestEnum1.NEVERDEREFALIASES ) 
            {
				l_ctls.setDerefLinkFlag( false ) ;
            } 
            else 
            {
                l_ctls.setDerefLinkFlag( true ) ;
            }
        }

        l_ctls.setReturningAttributes( getAttributesToReturn() ) ;
        l_ctls.setReturningObjFlag( m_request.protocolOp.searchRequest
            .typesOnly ) ;

        if ( null != m_request.protocolOp.searchRequest.timeLimit ) 
        {
			l_ctls.setTimeLimit(m_request.protocolOp.searchRequest.
				timeLimit.intValue()) ;
        }

        if ( null != m_request.protocolOp.searchRequest.scope ) 
        {
			switch ( m_request.protocolOp.searchRequest.scope.value ) 
            {
			case( SearchRequestEnum.BASEOBJECT ):
				l_ctls.setSearchScope( SearchControls.OBJECT_SCOPE ) ;
				break ;
			case( SearchRequestEnum.SINGLELEVEL ):
				l_ctls.setSearchScope( SearchControls.ONELEVEL_SCOPE ) ;
				break ;
			case( SearchRequestEnum.WHOLESUBTREE ):
				l_ctls.setSearchScope( SearchControls.SUBTREE_SCOPE ) ;
				break ;
			default:
				break ;
			}
        }

        return l_ctls ;
    }


    public Name getBaseDN()
    {
        return m_baseDN ;
    }


    public byte [] getBaseDNBytes()
    {
        return m_request.protocolOp.searchRequest.baseObject ;
    }


    public int getSearchScope()
    {
        return m_request.protocolOp.searchRequest.scope.value ;
    }


    public Enumeration getRequestedAttributes()
    {
        final AttributeDescriptionList l_adl =
            m_request.protocolOp.searchRequest.attributes ;

        if ( l_adl.size() < 0 ) 
        {
            // Returning empty enum so we don't care about converting binary
            // bytes to a string nothing comes out of this enumeration.
            return l_adl.elements() ;
        }

        final Enumeration l_list = l_adl.elements() ;
        return new Enumeration() 
        {
            public boolean hasMoreElements() 
            {
                return l_list.hasMoreElements() ;
            }

            public Object nextElement() 
            {
                String l_retval = 
                    new String( (byte [] ) l_list.nextElement() ) ;

                if ( getLogger().isDebugEnabled() ) 
                {
                    getLogger().debug( "SearchRequest Specified Attribute '"
                        + l_retval + "'" ) ;
                }

                return l_retval ;
            }
        } ;
    }


    public void sendReference( LdapEntry l_entry )
    {
        throw new NotImplementedException() ;
    }


    /**
     * Synchronous send that does not use asynchronous event based approach.
     *
    public void sendEntry( LdapEntry a_entry )
    {
        try {
            LDAPMessage l_pdu = new LDAPMessage() ;
            l_pdu.messageID = m_request.messageID ;
    
            LDAPMessageChoice l_protocolOp = new LDAPMessageChoice() ;
            l_pdu.protocolOp = l_protocolOp ;
            l_protocolOp.choiceId = LDAPMessageChoice.SEARCHRESENTRY_CID ;
            SearchResultEntry l_result = new SearchResultEntry() ;
            l_protocolOp.searchResEntry = l_result ;
    
            l_result.attributes = new PartialAttributeList() ;

            // If entry is the RootDSE then the dn could be null
            if ( a_entry.getEntryDN() == null ) 
            {
                l_result.objectName = "".getBytes() ;
            } 
            else 
            {
            	l_result.objectName = a_entry.getEntryDN().getBytes() ;
            }

            addTo( l_result.attributes, a_entry ) ;

            byte [] l_buf = m_encoder.encode( m_client, l_pdu ) ;
            m_outputManager.write( m_client, 
                new ByteArrayInputStream( l_buf ) ) ;

            if ( getLogger().isDebugEnabled() ) 
            {
                getLogger().debug( getMessageKey()
                    + " - SearchRequestProcessor.sendEntry(): composed "
                    + "response for entry "
                    + a_entry.getEntryDN() + ":\n" + l_pdu ) ;
            }
        } 
        catch( Exception e ) 
        {
            getLogger().debug( "Could not send search entry response for entry "
                + "cadidate:\n" + a_entry, e ) ;
        }
    }


    LDAPMessage sendDone()
    {
        LDAPMessage l_doneMsg = new LDAPMessage() ;

        try {
            getLogger().debug("search complete sending done response!") ;
            l_doneMsg.messageID = m_request.messageID ;
            LDAPMessageChoice l_protocolOp = new LDAPMessageChoice() ;
            l_protocolOp.choiceId = LDAPMessageChoice.SEARCHRESDONE_CID ;
            l_doneMsg.protocolOp = l_protocolOp ;

            SearchResultDone l_result = new SearchResultDone() ;
            l_protocolOp.searchResDone = l_result ;
            LDAPResultEnum l_enum = new LDAPResultEnum() ;
            l_result.resultCode = l_enum ;
            l_enum.value = LDAPResultEnum.SUCCESS ;
            l_result.errorMessage = "search complete!".getBytes() ;
            l_result.matchedDN = getBaseDNBytes() ;
        } catch(Exception e) {
            getLogger().debug("Cant send search done response.", e) ;
        }

		return l_doneMsg ;
    }


    LDAPMessage sendDone(int a_resultCode, String a_message)
    {
        LDAPMessage l_doneMsg = new LDAPMessage() ;

        try {
            l_doneMsg.messageID = m_request.messageID ;
            LDAPMessageChoice l_protocolOp = new LDAPMessageChoice() ;
            l_protocolOp.choiceId = LDAPMessageChoice.SEARCHRESDONE_CID ;
            l_doneMsg.protocolOp = l_protocolOp ;

            SearchResultDone l_result = new SearchResultDone() ;
            l_protocolOp.searchResDone = l_result ;
            LDAPResultEnum l_enum = new LDAPResultEnum() ;
            l_result.resultCode = l_enum ;
            l_enum.value = a_resultCode ;
            l_result.errorMessage = a_message.getBytes() ;
            l_result.matchedDN = getBaseDNBytes() ;
        } catch(Exception e) {
            getLogger().debug("Cant send search done response.", e) ;
        }

		return l_doneMsg ;
    }


    /**
     * dummy to test protocol communication
     *
    void addTo( PartialAttributeList a_attribs, LdapEntry a_entry )
        throws NamingException
    {
        boolean l_userSpecified = false ;
        
        // List of attributes specified for return by the user
        Enumeration l_list = getRequestedAttributes() ;

        //
        // If the attribute description list is empty or null in the request
        // then we need to return all non-operational attributes rather than
        // user requested attributes which may request operational attribute
        // values.
        //

        if( ! l_list.hasMoreElements() ) 
        {
            l_list = Collections.enumeration( a_entry.attributes() ) ;
            l_userSpecified = false ;
        }
        else 
        {
            l_userSpecified = true ;
            
            if ( getLogger().isDebugEnabled() ) 
            {
                StringBuffer l_buf = new StringBuffer() ;
                l_buf.append( "Looks like the user is asking for specific " ) ;
                l_buf.append( "attributes to be returned!\nThe requested " ) ;
                l_buf.append( "attributes are:\n" ) ;

                Enumeration l_attribs = getRequestedAttributes() ;
                while ( l_attribs.hasMoreElements() ) 
                {
                    String l_attrib = ( String ) l_attribs.nextElement() ;
                    l_buf.append( "\t" ).append( l_attrib ).append( "\n" ) ;
                }
                getLogger().debug( l_buf.toString() ) ;
            }
        }


        PartialAttributeListSeq l_seq = null ;
        PartialAttributeListSeqSetOf l_seqSet = null ;
        
        while ( l_list.hasMoreElements() )
        {
            l_seq = new PartialAttributeListSeq() ;
            String l_name = ( String ) l_list.nextElement() ;

            if ( ! a_entry.hasAttribute( l_name ) ) 
            {
                if ( getLogger().isDebugEnabled() ) 
                {
                    getLogger().debug( "Attribute '" + l_name + "' does not "
                        + "exist in entry '" + a_entry.getEntryDN() + "'\n"
                        + "values will not be returned for this requested "
                        + "attribute." ) ;
                }

                continue ;
            }
            
            
            if ( ! l_userSpecified && 
                m_nexus.getSchema( m_baseDN ).isOperational( l_name ) )
            {
                continue ;
            }
            

            l_seq.type = l_name.getBytes() ;
            l_seqSet = new PartialAttributeListSeqSetOf() ;
            Iterator l_values = a_entry.getMultiValue( l_name ).iterator() ;
            while( l_values.hasNext() ) 
            {
                Object l_value = l_values.next() ;
                if ( l_value.getClass().isArray() ) 
                {
                    l_seqSet.put( ( byte [] ) l_value, ( byte [] ) l_value ) ;
                } 
                else 
                {
                    l_seqSet.put( ( ( String ) l_value ).getBytes(),
                        ( ( String ) l_value ).getBytes() ) ;
                }
            }

            l_seq.vals = l_seqSet ;
            a_attribs.add( l_seq ) ;
        }
    }


    ////////////////////////
    // Life-Cycle Methods //
    ////////////////////////


    public void initialize()
        throws Exception
    {
        // Does nothing!
    }


    public void service( ServiceManager a_manager )
        throws ServiceException
    {
        m_nexus = ( UnifiedBackend ) 
            a_manager.lookup( UnifiedBackend.ROLE ) ;
        m_encoder = ( Encoder ) 
            a_manager.lookup( Encoder.ROLE ) ;
        m_outputManager = ( OutputManager ) 
            a_manager.lookup( OutputManager.ROLE ) ;
        m_eventManager = ( EventManager ) 
            a_manager.lookup( EventManager.ROLE ) ;
    }
    */
}
