/*
 * $Id: Stats.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import org.apache.ldap.common.message.Request ;
import org.apache.ldap.common.message.MessageTypeEnum ;


/**
 * Collects statistics for protocol requests serviced by the server.
 */
public class Stats
{
	Request m_lastAbandonReq = null ;
	Request m_lastAddReq = null ;
	Request m_lastBindReq = null ;
	Request m_lastCompareReq = null ;
	Request m_lastDelReq = null ;
	Request m_lastExtendedReq = null ;
	Request m_lastModDNReq = null ;
	Request m_lastModifyReq = null ;
	Request m_lastSearchReq = null ;
	Request m_lastUnbindReq = null ;
	Request m_lastRequest = null ;

	int m_numAbandonReq = 0 ;
	int m_numAddReq = 0 ;
	int m_numBindReq = 0 ;
	int m_numCompareReq = 0 ;
	int m_numDelReq = 0 ;
	int m_numExtendedReq = 0 ;
	int m_numModDNReq = 0 ;
	int m_numModifyReq = 0 ;
	int m_numSearchReq = 0 ;
	int m_numUnbindReq = 0 ;
	int m_numTotal = 0 ;


	public void addRequest( Request a_msg )
    {
        m_lastRequest = a_msg ;
        m_numTotal++ ;

        switch( a_msg.getType().getValue() ) {
        case( MessageTypeEnum.ABANDONREQUEST_VAL ):
            m_lastAbandonReq = a_msg ;
        	m_numAbandonReq++ ;
            break ;
        case( MessageTypeEnum.ADDREQUEST_VAL ):
            m_lastAddReq = a_msg ;
        	m_numAddReq++ ;
            break ;
        case( MessageTypeEnum.BINDREQUEST_VAL ):
            m_lastBindReq = a_msg ;
        	m_numBindReq++ ;
            break ;
        case( MessageTypeEnum.COMPAREREQUEST_VAL ):
            m_lastCompareReq = a_msg ;
        	m_numCompareReq++ ;
            break ;
        case( MessageTypeEnum.DELREQUEST_VAL ):
            m_lastDelReq = a_msg ;
        	m_numDelReq++ ;
            break ;
        case( MessageTypeEnum.EXTENDEDREQ_VAL ):
            m_lastExtendedReq = a_msg ;
        	m_numExtendedReq++ ;
            break ;
        case( MessageTypeEnum.MODDNREQUEST_VAL ):
            m_lastModDNReq = a_msg ;
        	m_numModDNReq++ ;
            break ;
        case( MessageTypeEnum.MODIFYREQUEST_VAL ):
            m_lastModifyReq = a_msg ;
        	m_numModifyReq++ ;
            break ;
        case( MessageTypeEnum.SEARCHREQUEST_VAL ):
            m_lastSearchReq = a_msg ;
        	m_numSearchReq++ ;
            break ;
        case( MessageTypeEnum.UNBINDREQUEST_VAL ):
            m_lastUnbindReq = a_msg ;
        	m_numUnbindReq++ ;
            break ;
        default:
            throw new RuntimeException("Unknown LDAP message type.") ;
        }
    }
}
