/*
 * $Id: StringComparator.java,v 1.2 2003/03/13 18:27:27 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;


import org.apache.eve.backend.jdbm.table.TableComparator ;


/**
 * A StringComparator that is both a java.util.Comparator and a
 * jdbm.helper.Comparator.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class StringComparator extends TableComparator
{
    /**
     * Version id for serialization.
     */
    final static long serialVersionUID = 1L ;


    /**
     * Compare two objects.
     */
     public int compare( Object a_obj1, Object a_obj2 ) {
        if (null == a_obj1) {
            throw new IllegalArgumentException("Argument 'a_obj1' is null") ;
        }

        if (null == a_obj2) {
            throw new IllegalArgumentException("Argument 'a_obj2' is null") ;
        }

        return ((String) a_obj1).compareTo(a_obj2) ;
     }
}
