package org.apache.eve.db;


/**
 * A table key/value String renderer for the display or logging of
 * human readable potentially binary data.
 * 
 */
public interface TupleRenderer
{
    /**
     * Gets the key Object rendered as a String.
     *
     * @param key the key Object
     * @return the String representation of the key Object
     */
    String getKeyString( Object key );

    /**
     * Gets the value Object rendered as a String.
     *
     * @param value the value Object
     * @return the String representation of the value Object
     */
    String getValueString( Object value );
}
