/*
 * $Id: Encoder.java,v 1.4 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.encoder ;


import org.apache.ldap.common.message.Response ;

import org.apache.eve.event.ResponseListener ;


/**
 * Avalon service interface for a module that encodes a Response using Basic
 * Encoding Rules into an LDAPv3 Protocol Data Unit (PDU) that can be delivered
 * over the wire.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public interface Encoder
    extends ResponseListener
{
    String ROLE = Encoder.class.getName() ;

    /**
     * Synchronously encodes an LDAPv3 protocol Response message into a byte
     * buffer that can be written to a Stream as an BER encoded PDU.
     *
     * @param a_response the LDAP Response message to be encoded.
     */
    public byte [] encode( Response a_response )
        throws EncoderException ;
}

