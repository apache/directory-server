/*
 * $Id: EmptyCursor.java,v 1.2 2003/03/13 18:26:51 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.NoSuchElementException ;


/**
 * An empty cursor that is closed on creation! Used as annon inner class in so
 * many places I had to create it.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public class EmptyCursor
    extends Cursor
{
    /**
     * Closes on creation.
     */
    public EmptyCursor()
    {
        try { close() ; } catch(Exception e) {} ;
    }

    /**
     * Throws NoSuchElementException all the time.
     */
    public Object advance() { throw new NoSuchElementException() ; }

    /**
     * Always returns false.
     */
    public boolean canAdvance() { return false ; }

    /**
     * Does nothing.
     */
    public void freeResources() { }
}
