/*
 * $Id: BigIntegerComparator.java,v 1.4 2003/03/13 18:27:25 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;


import java.math.BigInteger ;
import org.apache.eve.backend.jdbm.table.TableComparator ;


public class BigIntegerComparator extends TableComparator
{
    /**
     * Version id for serialization.
     */
    final static long serialVersionUID = 1L ;


    /**
     * Compare two objects.
     *
     * @param obj1 First object
     * @param obj2 Second object
     * @return 1 if obj1 > obj2, 0 if obj1 == obj2, -1 if obj1 < obj2
     */
     public int compare( Object an_obj1, Object an_obj2 ) {
        if (an_obj1 == null) {
            throw new IllegalArgumentException("Argument 'an_obj1' is null") ;
        }

        if (an_obj2 == null) {
            throw new IllegalArgumentException("Argument 'an_obj2' is null") ;
        }

        //System.out.println("BigIntegerComparator.compare(" + an_obj1 + ", "
        //    + an_obj2 + ")") ;
        BigInteger l_int1 = (BigInteger) an_obj1 ;
        BigInteger l_int2 = (BigInteger) an_obj2 ;
        return l_int1.compareTo(l_int2) ;
     }
}
