package org.apache.eve.db;


import java.io.Serializable;
import java.util.Comparator;


/**
 * A TupleComparator that compares keys only.
 * 
 */
public class KeyOnlyComparator
    implements TupleComparator, Serializable
{
    /** TODO */
    private Comparator keyComparator = null;


    /**
     * TODO Document me! 
     *
     * @param comparator  TODO
     */
    public KeyOnlyComparator( Comparator comparator )
    {
        keyComparator = comparator;
    }


    /**
     * Gets the comparator used to compare keys.  May be null in which
     * case the compareKey method will throw an UnsupportedOperationException.
     *
     * @return the comparator for comparing keys.
     */
    public Comparator getKeyComparator()
    {
        return keyComparator;
    }


    /**
     * Will throw an UnsupportedOperationException every time.
     *
     * @return TODO
     * @throws UnsupportedOperationException every time.
     */
    public Comparator getValueComparator()
    {
        throw new UnsupportedOperationException();
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
        return keyComparator.compare( key1, key2 );
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
        throw new UnsupportedOperationException();
    }
}
