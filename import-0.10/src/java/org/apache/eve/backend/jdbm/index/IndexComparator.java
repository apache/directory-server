/*
 * $Id: IndexComparator.java,v 1.4 2003/03/13 18:27:26 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm.index ;


import org.apache.eve.backend.jdbm.table.TupleComparator ;
import org.apache.eve.backend.jdbm.table.TableComparator ;


/**
 * Doc me!
 * @todo Doc me!
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public class IndexComparator
    implements TupleComparator
{
    public static final StringComparator strComp =
        new StringComparator() ;
    public static final BigIntegerComparator intComp =
        new BigIntegerComparator() ;

    private final boolean isForwardMap ;
    private final TableComparator m_keyComp ;


    public IndexComparator(TableComparator a_keyComp, boolean isForwardMap)
    {
        m_keyComp = a_keyComp ;
        this.isForwardMap = isForwardMap ;
    }


    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
	public TableComparator getKeyComparator()
    {
        if(this.isForwardMap) {
            return m_keyComp ;
        }

        return intComp ;
    }


    /**
     * Gets the binary comparator used to compare valuess.  May be null in which
     * case the compareValue method will throw an UnsupportedOperationException.
     *
     * @return the binary comparator for comparing values.
     */
	public TableComparator getValueComparator()
    {
        if(this.isForwardMap) {
            return intComp ;
        }

        return m_keyComp ;
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
        jdbm.helper.Comparator l_comparator =
            (jdbm.helper.Comparator) getKeyComparator() ;
        return l_comparator.compare(a_key1, a_key2) ;
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
        jdbm.helper.Comparator l_comparator =
            (jdbm.helper.Comparator) getValueComparator() ;
        return l_comparator.compare(a_value1, a_value2) ;
    }
}
