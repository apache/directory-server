/*
 * $Id: ModifyDNRequestProcessor.java,v 1.10 2003/08/22 21:15:56 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol ;


import javax.naming.Name ;

import org.apache.avalon.framework.ExceptionUtil ;
import org.apache.avalon.framework.service.ServiceManager ;
import org.apache.avalon.framework.service.ServiceException ;

import org.apache.eve.client.ClientKey ;
import org.apache.eve.backend.LdapEntry ;
import org.apache.eve.backend.UnifiedBackend ;
import org.apache.eve.event.protocol.EventManager;
import org.apache.eve.event.protocol.ModifyDnEvent;


/**
 * Processes an LDAP request to modify an entry's DN either changing its name or
 * relocation the DIT branch at an entry under a new parent entry.  The protocol
 * in <a href="http://www.faqs.org/rfcs/rfc2251.html">RFC 2251<a> defines the
 * request processed by the RequestProcessor.
 * <pre>
 *     The Modify DN Operation allows a client to change the leftmost (least
 * significant) component of the name of an entry in the directory, or
 * to move a subtree of entries to a new location in the directory.  The
 * Modify DN Request is defined as follows:
 *
 *      ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
 *              entry           LDAPDN,
 *              newrdn          RelativeLDAPDN,
 *              deleteoldrdn    BOOLEAN,
 *              newSuperior     [0] LDAPDN OPTIONAL }
 *
 * </pre>
 */
public class ModifyDNRequestProcessor
//    extends BaseRequestProcessor
{
    /** Response type of this request processor class *
    public static final int RES_TYPE = LDAPMessageChoice.MODDNRESPONSE_CID ;


    private EventManager m_eventManager = null ;
    private LDAPMessage m_response = null ;
	private UnifiedBackend m_nexus = null ;


    /**
     * Creates and initializes the request processor.
     *
    public ModifyDNRequestProcessor(ClientKey a_client, LDAPMessage a_request)
    {
        super(a_client, a_request, RES_TYPE) ;
    }


    /**
     * Processes the LDAP ModifyDN request.  The parameters of the request are
     * transduced into arguments fed into the move or modifyRdn methods of the
     * Backend nexus.  Below you'll find a description of the parameters
     * packaged in the ModifyRdn PDU:<br>
     * <pre>
     * Parameters of the Modify DN Request are:
     *
     * - entry: the Distinguished Name of the entry to be changed.  This
     *   entry may or may not have subordinate entries.
     *
     * - newrdn: the RDN that will form the leftmost component of the new
     *   name of the entry.
     *
     * - deleteoldrdn: a boolean parameter that controls whether the old RDN
     *   attribute values are to be retained as attributes of the entry, or
     *   deleted from the entry.
     *
     * - newSuperior: if present, this is the Distinguished Name of the entry
     *   which becomes the immediate superior of the existing entry.
     * </pre>
     *
     * @return the response message envelope formulated according to the
     * results of the ModifyDN request.
     *
    public LDAPMessage process()
    {
        try 
        {
            ModifyDnEvent l_event = null ;
            String l_supDnStr = getSuperiorEntryDN() ;
            Name l_dn = m_nexus.getNormalizedName( getEntryDN() ) ;
            Name l_rdn = m_nexus.getNormalizedName( getNewRdn() ) ;
            Name l_superior = m_nexus.getNormalizedName( l_supDnStr ) ;
            LdapEntry l_entry = m_nexus.read( l_dn ) ;

            if ( getLogger().isDebugEnabled() ) 
            {
                getLogger().debug( "ModifyDNRequestProcessor: delete oldrdn = "
                    + deleteOldRdn() ) ;
                getLogger().debug( "ModifyDNRequestProcessor: dn = " 
                    + l_dn ) ;
                getLogger().debug( "ModifyDNRequestProcessor: rdn = " 
                    + l_rdn ) ;
                getLogger().debug( "ModifyDNRequestProcessor: superior = "
                    + l_superior ) ;
            }

            
            if ( l_rdn.size() > 1 )
            {
                setResponse( LDAPResultEnum.INVALIDDNSYNTAX, null,
                    "ModifyDn: Expecting either a null RDN or one of length 1 " 
                    + "but got RDN of length " + l_rdn.size() + " for '" 
                    + l_rdn + "'" ) ;
            }
            

            /*
             * If the superior Dn String argument in the PDU is null then we
             * have a simple rdn name change as opposed to a move operation
             *
            if ( l_supDnStr == null)
            {
                m_nexus.modifyRdn( l_entry, l_rdn, deleteOldRdn() ) ;
            } 
            else if ( l_rdn.size() == 0 ) 
            {
                LdapEntry l_parent = m_nexus.read( l_superior ) ;
                m_nexus.move( l_parent, l_entry ) ;
            } 
            else 
            {
                LdapEntry l_parent = m_nexus.read( l_superior ) ;
                m_nexus.move( l_parent, l_entry, l_rdn, deleteOldRdn() ) ;
            }

            setResponse( LDAPResultEnum.SUCCESS, getEntryDNBytes(),
                "DN Modifications Successfully Completed!" ) ;

        } 
        catch ( Throwable t ) 
        {
            if( getLogger().isDebugEnabled() ) 
            {
                setResponse( LDAPResultEnum.OPERATIONSERROR, null, t ) ;
            } 
            else 
            {
                setResponse( LDAPResultEnum.OPERATIONSERROR, null,
                    t.getMessage() ) ;
            }
        }

        return m_response ;
    }


    public boolean deleteOldRdn()
    {
        return m_request.protocolOp.modDNRequest.deleteoldrdn ;
    }


    public String getSuperiorEntryDN()
    {
        if(m_request.protocolOp.modDNRequest.newSuperior != null) {
            return new String(m_request.protocolOp.modDNRequest.newSuperior) ;
        } else {
            return null ;
        }
    }


    public String getNewRdn()
    {
        if(m_request.protocolOp.modDNRequest.newrdn != null) {
            return new String(m_request.protocolOp.modDNRequest.newrdn) ;
        } else {
            return null ;
        }
    }


    public String getEntryDN()
    {
        return new String(m_request.protocolOp.modDNRequest.entry) ;
    }


    public byte [] getEntryDNBytes()
    {
        return m_request.protocolOp.modDNRequest.entry ;
    }


    public void setResponse(int a_resultType, byte [] a_dn,
        Throwable a_throwable)
    {
        setResponse(a_resultType, a_dn,
            ExceptionUtil.printStackTrace(a_throwable)) ;
    }


    public void setResponse(int a_resultType, byte [] a_dn, String an_errMsg)
    {
        m_response.protocolOp.modDNResponse.resultCode.value = a_resultType ;

        if(an_errMsg != null) {
            m_response.protocolOp.modDNResponse.errorMessage =
                an_errMsg.getBytes() ;
        } else {
            m_response.protocolOp.modDNResponse.errorMessage =
                "".getBytes() ;
        }

        if(a_dn == null) {
            m_response.protocolOp.modDNResponse.matchedDN = "".getBytes() ;
        } else {
            m_response.protocolOp.modDNResponse.matchedDN = a_dn ;
        }
    }


    ////////////////////////
    // Life-Cycle Methods //
    ////////////////////////


    public void initialize()
        throws Exception
    {
        m_response = new LDAPMessage() ;
        m_response.messageID = m_request.messageID ;
        LDAPMessageChoice l_protocolOp = new LDAPMessageChoice() ;
        l_protocolOp.choiceId = this.m_responseChoiceId ;
        ModifyDNResponse l_modDNResponse = new ModifyDNResponse() ;
        LDAPResultEnum l_resultCode = new LDAPResultEnum() ;
        l_modDNResponse.resultCode =  l_resultCode ;
		l_protocolOp.modDNResponse = l_modDNResponse ;
		m_response.protocolOp = l_protocolOp ;
    }


    public void service(ServiceManager a_manager)
        throws ServiceException
    {
        m_nexus = (UnifiedBackend) a_manager.lookup(UnifiedBackend.ROLE) ;
        m_eventManager = (EventManager) a_manager.lookup(EventManager.ROLE) ;
    }
    */
}
