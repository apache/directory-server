/*
 * $Id: KeyOnlyComparator.java,v 1.2 2003/03/13 18:27:32 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.table ;


public class KeyOnlyComparator
    implements TupleComparator
{
    TableComparator m_keyComparator = null ;


    public KeyOnlyComparator(TableComparator a_comparator)
    {
        m_keyComparator = a_comparator ;
    }


    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
	public TableComparator getKeyComparator()
    {
        return m_keyComparator ;
    }


    /**
     * Will throw an UnsupportedOperationException every time.
     *
     * @throws UnsuporredOperation every time.
     */
	public TableComparator getValueComparator()
    {
        throw new UnsupportedOperationException() ;
    }


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
    public int compareKey(Object a_key1, Object a_key2)
    {
        return m_keyComparator.compare(a_key1, a_key2) ;
    }


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
    public int compareValue(Object a_value1, Object a_value2)
    {
        throw new UnsupportedOperationException() ;
    }
}
