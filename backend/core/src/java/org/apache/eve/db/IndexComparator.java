package org.apache.eve.db;


import java.util.Comparator;

import org.apache.ldap.common.util.BigIntegerComparator;


/**
 * Compararator for index records.
 *
 */
public class IndexComparator
    implements TupleComparator
{
    /** Whether or not the key/value is swapped */
    private final boolean isForwardMap;
    /** The key comparison to use */
    private final Comparator keyComp;


    /**
     * Creates an IndexComparator.
     *
     * @param keyComp the table comparator to use for keys
     * @param isForwardMap whether or not the comparator should swap the 
     * key value pair while conducting comparisons.
     */
    public IndexComparator( Comparator keyComp, boolean isForwardMap )
    {
        this.keyComp = keyComp;
        this.isForwardMap = isForwardMap;
    }


    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
    public Comparator getKeyComparator()
    {
        if ( isForwardMap ) 
        {
            return keyComp;
        }

        return BigIntegerComparator.INSTANCE;
    }


    /**
     * Gets the binary comparator used to compare valuess.  May be null in which
     * case the compareValue method will throw an UnsupportedOperationException.
     *
     * @return the binary comparator for comparing values.
     */
    public Comparator getValueComparator()
    {
        if ( isForwardMap ) 
        {
            return BigIntegerComparator.INSTANCE;
        }

        return keyComp;
    }


    /**
     * Compares key Object to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param key1 the first key to compare
     * @param key2 the other key to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second byte array.
     */
    public int compareKey( Object key1, Object key2 )
    {
        return getKeyComparator().compare( key1, key2 );
    }


    /**
     * Comparse value Objects to determine their sorting order returning a
     * value = to, < or > than 0.
     *
     * @param value1 the first value to compare
     * @param value2 the other value to compare to the first
     * @return 0 if both are equal, a negative value less than 0 if the first
     * is less than the second, or a postive value if the first is greater than
     * the second Object.
     */
    public int compareValue( Object value1, Object value2 )
    {
        return getValueComparator().compare( value1, value2 );
    }
}
