package org.apache.eve.db;


import javax.naming.NamingException;


/**
 * TupleBrowser interface used to abstract 
 * 
 */
public interface TupleBrowser
{
    /**
     * Gets the next value deemed greater than the last using the key 
     * comparator.
     *
     * @param tuple the tuple to populate with a key/value pair
     * @return true if there was a next that was populated or false otherwise
     * @throws NamingException @todo
     */
    boolean getNext( Tuple tuple ) throws NamingException;

    /**
     * Gets the previous value deemed greater than the last using the key 
     * comparator.
     *
     * @param tuple the tuple to populate with a key/value pair
     * @return true if there was a previous value populated or false otherwise
     * @throws NamingException @todo
     */
    boolean getPrevious( Tuple tuple ) throws NamingException;
}
