/*
 * $Id: CursorException.java,v 1.2 2003/03/13 18:26:49 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.NoSuchElementException ;


/**
 * This exception is thrown when Cursor has no more elements to return.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class CursorException
    extends NoSuchElementException
{
    /** Nested exception if any. */
    private final Throwable m_error ;


    /**
     * Constructs an Exception with a nested Throwable whose message is
     * used to compose this exceptions message.
     *
     * @param an_exception a backend exception to wrap.
     */
    public CursorException(Throwable an_exception)
    {
        super(an_exception.toString()) ;
        this.m_error = an_exception ;
    }

    /**
     * Constructs an Exception without a message and without a Throwable.
     */
    public CursorException()
    {
        super() ;
        this.m_error = null ;
    }

    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_message The message associated with the exception.
     */
    public CursorException(String a_message)
    {
        super(a_message) ;
        this.m_error = null ;
    }


    /**
     * Checks to see if this exception was due to a backend/database error.
     *
     * @return true if a Throwable is wrapped by this exception or false
     * otherwise.
     */
    public boolean isError()
    {
        return this.m_error != null ;
    }
}
