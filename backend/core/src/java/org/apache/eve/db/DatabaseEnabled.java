package org.apache.eve.db;


/**
 * Database enabled components have a database assigned to them.
 *
 */
public interface DatabaseEnabled
{
    /**
     * Sets the database this SearchEngine operates on.
     * 
     * @param db the database which this engine operates on.
     */
    void enableDatabase( Database db );
}
