/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.directory.shared.ldap;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collection;

import java.io.PrintWriter;
import java.io.PrintStream;


/**
 * This exception is thrown when Base class for nested exceptions.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public class RuntimeMultiException extends RuntimeException
{
    static final long serialVersionUID = 8582253398936366771L;

    /** Collection of nested exceptions. */
    private Collection m_nestedExceptions = new ArrayList();


    /**
     * Constructs an Exception without a message.
     */
    public RuntimeMultiException()
    {
        super();
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_message
     *            The message associated with the exception.
     */
    public RuntimeMultiException(String a_message)
    {
        super( a_message );
    }


    /**
     * Lists the nested exceptions that this Exception encapsulates.
     * 
     * @return an Iterator over the nested exceptions.
     */
    public Iterator listNestedExceptions()
    {
        return m_nestedExceptions.iterator();
    }


    /**
     * Gets the size (number of) exceptions nested within this exception.
     * 
     * @return the size of this nested exception.
     */
    public int size()
    {
        return m_nestedExceptions.size();
    }


    /**
     * Tests to see if exceptions are nested within this exception.
     * 
     * @return true if an exception is nested, false otherwise
     */
    public boolean isEmpty()
    {
        return m_nestedExceptions.isEmpty();
    }


    /**
     * Add an exeception to this multiexception.
     * 
     * @param a_nested
     *            exception to add to this MultiException.
     */
    public void addThrowable( Throwable a_nested )
    {
        this.m_nestedExceptions.add( a_nested );
    }


    // ///////////////////////////////////////////
    // Overriden Throwable Stack Trace Methods //
    // ///////////////////////////////////////////

    /**
     * Beside printing out the standard stack trace this method prints out the
     * stack traces of all the nested exceptions.
     * 
     * @param an_out
     *            PrintWriter to write the nested stack trace to.
     */
    public void printStackTrace( PrintWriter an_out )
    {
        super.printStackTrace( an_out );

        an_out.println( "Nested exceptions to follow:\n" );
        Iterator l_list = listNestedExceptions();
        Throwable l_throwable = null;
        while ( l_list.hasNext() )
        {
            l_throwable = ( Throwable ) l_list.next();
            l_throwable.printStackTrace();
            if ( l_list.hasNext() )
            {
                an_out.println( "\n\t<<========= Next Nested Exception" + " ========>>\n" );
            }
            else
            {
                an_out.println( "\n\t<<========= Last Nested Exception" + " ========>>\n" );
            }
        }
    }


    /**
     * Beside printing out the standard stack trace this method prints out the
     * stack traces of all the nested exceptions.
     * 
     * @param an_out
     *            PrintStream to write the nested stack trace to.
     */
    public void printStackTrace( PrintStream an_out )
    {
        super.printStackTrace( an_out );

        an_out.println( "Nested exceptions to follow:\n" );
        Iterator l_list = listNestedExceptions();
        Throwable l_throwable = null;
        while ( l_list.hasNext() )
        {
            l_throwable = ( Throwable ) l_list.next();
            l_throwable.printStackTrace();
            if ( l_list.hasNext() )
            {
                an_out.println( "\n\t<<========= Next Nested Exception" + " ========>>\n" );
            }
            else
            {
                an_out.println( "\n\t<<========= Last Nested Exception" + " ========>>\n" );
            }
        }
    }


    /**
     * Beside printing out the standard stack trace this method prints out the
     * stack traces of all the nested exceptions using standard error.
     */
    public void printStackTrace()
    {
        this.printStackTrace( System.err );
    }
}
