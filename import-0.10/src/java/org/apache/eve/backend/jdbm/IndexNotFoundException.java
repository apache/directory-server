/*
 * $Id: IndexNotFoundException.java,v 1.2 2003/03/13 18:27:15 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend.jdbm ;


import org.apache.eve.backend.BackendException ;


public class IndexNotFoundException
    extends BackendException
{
    public final String m_indexName ;

    /**
     * Constructs an Exception with a detailed message.
     * @param a_message The message associated with the exception.
     */
    public IndexNotFoundException(String a_indexName)
    {
        super("Cannot efficiently search the DIB w/o an index on attribute "
            + a_indexName + "\n. To allow such searches please contact the "
            + "directory\nadministrator to create the index or to enable "
            + "referals on searches using these\nattributes to a replica with "
            + "the required set of indices.") ;
        m_indexName = a_indexName ;
    }


    /**
     * Constructs an Exception with a detailed message.
     * @param a_message The message associated with the exception.
     */
    public IndexNotFoundException(String a_message, String a_indexName)
    {
        super(a_message) ;
        m_indexName = a_indexName ;
    }


    /**
     * Constructs an Exception with a detailed message and an error.
     * @param a_message The message associated with the exception.
     */
    public IndexNotFoundException(String a_message, String a_indexName,
        Throwable a_throwable)
    {
        super(a_message, a_throwable) ;
        m_indexName = a_indexName ;
    }


    public String getIndexName()
    {
        return m_indexName ;
    }
}
