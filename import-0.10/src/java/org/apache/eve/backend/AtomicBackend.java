/*
 * $Id: AtomicBackend.java,v 1.5 2003/08/22 21:15:54 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import javax.naming.Name ; 
import org.apache.eve.schema.Schema ;


/**
 * Atomic backends represent a concrete configurable Directory Information Base
 *(DIB). They are not composed of other Backends.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.5 $
 */
public interface AtomicBackend
    extends Backend, BackendConfig
{
    /**
     * Role of this service interface as mandated by the avalon framework.
     */
    public static final String ROLE = AtomicBackend.class.getName() ;

    /**
     * Gets the normalized DN of the suffix managed by this AtomicBackend.
     *
     * @return Name representing the normalized root suffix for this
     * AtomicBackend.
     */
    Name getSuffix() ;
}
