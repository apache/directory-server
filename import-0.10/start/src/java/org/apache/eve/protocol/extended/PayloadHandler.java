/*
 * $Id: PayloadHandler.java,v 1.2 2003/08/22 21:15:56 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.protocol.extended ;


import javax.naming.NamingException ;

import org.apache.eve.backend.BackendException ;

import org.apache.avalon.framework.logger.LogEnabled ;
import org.apache.avalon.framework.service.Serviceable ;


/**
 * Interface used by all extended request payload handlers which are special
 * handlers invoked by the ExtendedRequestHandler itself to deal with responding
 * to an extended request with an extended response payload.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public interface PayloadHandler
{
    /**
     * Gets the object identifier of the extended request this handler is
     * designed for.
     *
     * @return the OID string.
     */
    String getRequestOID() ;

    /**
     * Handles the extended request by extracting parameters from the payload
     * and optionally generating a response for the request.
     */
    byte [] handle( byte [] a_payload )
        throws BackendException, NamingException ;
}

