/*
 * $Id: Decoder.java,v 1.4 2003/03/26 02:09:15 jmachols Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.decoder ;


import org.apache.eve.event.InputListener ;


/**
 * Service interface for decoding an ASN.1 BER encoded stream to demarshal
 * LDAP protocol request messages.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: jmachols $
 * @version $Revision: 1.4 $
 */
public interface Decoder
    extends InputListener
{
    /** Role played by the service as defined in Avalon */
    String ROLE = Decoder.class.getName() ;
}
