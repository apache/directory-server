/*
 * $Id: TupleComparator.java,v 1.3 2003/03/13 18:27:34 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


/**
 * Used to compare the sorting order of binary data.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.3 $
 */
public interface TupleComparator
{
    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
	TableComparator getKeyComparator() ;


    /**
     * Gets the binary comparator used to compare valuess.  May be null in which
     * case the compareValue method will throw an UnsupportedOperationException.
     *
     * @return the binary comparator for comparing values.
     */
	TableComparator getValueComparator() ;


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param a_key1 the first key to compare
     * @param a_key2 the other key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second byte array.
     */
    int compareKey(Object a_key1, Object a_key2) ;


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param a_value1 the first value to compare
     * @param a_value2 the other value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second Object.
     */
    int compareValue(Object a_value1, Object a_value2) ;
}
