/*
 * $Id: TupleRenderer.java,v 1.2 2003/03/13 18:27:35 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


import org.apache.eve.backend.BackendException ;


/**
 * A table key/value String renderer for the display or logging of
 * human readable potentially binary data.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public interface TupleRenderer
{
    /**
     * Gets the key Object rendered as a String.
     *
     * @param a_key the key Object
     * @return the String representation of the key Object
     * @throws BackendException if there is a backend failure while trying to
     * interpret the key.
     */
    String getKeyString(Object a_key) ;

    /**
     * Gets the value Object rendered as a String.
     *
     * @param a_value the value Object
     * @return the String representation of the value Object
     * @throws BackendException if there is a backend failure while trying to
     * interpret the value.
     */
    String getValueString(Object a_value) ;
}
