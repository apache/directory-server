/*
 * $Id: Cursor.java,v 1.7 2003/03/13 18:26:44 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;


import java.util.ArrayList ;
import java.util.Observable ;
import java.util.Enumeration ;

import javax.naming.NamingException ;
import javax.naming.NamingEnumeration ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.logger.LogEnabled ;
import java.util.Iterator;


/**
 * Cursors are automagically closing server-side NamingEnumerations.
 * 
 * Their open/closed state changes are comunicated to their Observers which
 * are usually the backends that created them.
 * <br><br>
 * Backend implementors are required to have at least one concrete Cursor
 * implementation. To do so requires the definition of three protected methods:
 * <br>
 * <br>
 * <code> freeResources: </code>
 * Cursors are potentially valuble resources to a backend and hence generally
 * need to free up their own resources when exhausted.  The mechanism if any to
 * free such resources will be specific to the backend's implementation. Cursor
 * implementors are hence required to provide a protected <code>freeResources
 * </code> method which is called whenever <code>close</code> is called on
 * an open cursor.  Hence, <code>freeResources</code> can only be called once
 * by calls to <code>close</code>.  The <code>freeResources</code>
 * implementation should not make calls to the <code>close</code> method.
 * Cursors automatically close themselves when they detect the consumption of
 * all elements via calls to NamingEnumeration interfaces.
 * <br><br>
 * <code> advance</code>
 * Backend failures need to propagate forward without breaking the Enumeration
 * interface contract.  Unfortunately enumerations do not allow generic
 * exception handling while getting the next element.  To work around this
 * problem while also supporting NamingEnumeration methods, we require concrete
 * cursor implementations to define an <code>advance</code> method
 * implementation.  Rather than place iteration code within the
 * <code>nextElement</code> implementation which is final and hence cannot be
 * overridden by subclasses, implementors must use the protected <code>advance
 * </code> method.  In fact, the <code>nextElement</code>
 * implementation delegates calls to the <code>advance</code> method by
 * wrapping the invokation with a BackendException/NamingException try catch
 * which transduces the BackendExceptions and NamingExceptions into a subclass
 * of NoSuchElementException (a.k.a a CursorException).
 * <br><br>
 * <code> canAdvance</code>
 * Tests to see if this Cursor can advance will most likely be backend specific
 * and may in some implementations throw a BackendException.  To facilitate the
 * automatic close of cursors on list consumption and backend exceptions <code>
 * hasMoreElement</code> has been defined as a final method that wraps calls
 * to <code>canAdvance</code>.  Unlike the <code>advance</code> method
 * backend exceptions are not transduced.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.7 $
 * 
 * @see CursorException
 * @see NamingEnumeration
 */
public abstract class Cursor extends Observable
        implements          				  // Cursors enumerate records
            NamingEnumeration,                // Cursors can & should be closed
            LogEnabled                        // Cursors can be enabled to log
{
    public static boolean debug = false ; 
    /** Tracks this Cursors closed/open state. */
    private boolean m_isClosed = false ;
    /** Logger used by this Cursor */
    private Logger m_logger = null ;
    /** Cursor Listener List */
    private ArrayList m_listeners = new ArrayList() ;


    /////////////////////////////////////////
    // LogEnabled Interface Implementation //
    /////////////////////////////////////////


    /**
     * Sets the logger used by this Cursor which most probably will be set by
     * its backend (factory).
     *
     * @param a_logger the logger to be used by this Cursor.
     */
    public void enableLogging(Logger a_logger)
    {
        m_logger = a_logger ;
    }


    /**
     * Gets the logger used by this Cursor which most probably was inherited
     * from its backend (factory).
     *
     * @return the logger used by this Cursor.
     */
    protected Logger getLogger()
    {
        return m_logger ;
    }


    ////////////////////////////////////////
    // Closable Interface Implementations //
    ////////////////////////////////////////


    /**
     * Gets whether this Cursor is currently open or closed.
     *
     * @return true if this Cursor is closed, otherwise false.
     */
    public final boolean isClosed()
    {
        return m_isClosed ;
    }


    /**
     *
     */
    public final void close()
        throws NamingException
    {
        if (!m_isClosed) {
            m_isClosed = true ;
            freeResources() ;
            super.setChanged() ;
        }
    }


    ///////////////////////////////////////////
    // Enumeration Interface Implementations //
    ///////////////////////////////////////////


    /**
     * Wrapper around advance which serves as an exception trap that transduces
     * BackendExceptions into CursorExceptions (a subclass of the expected RT
     * NoSuchElementException).
     *
     * @warning Invokations within the advance() implementations of subclasses
     * will result in a never ending chain recursion.
     * @return the next element in this Cursor.
     * @side-effect Closes this cursor before throwing any exceptions.
     */
    public final Object nextElement()
    {
        if (m_isClosed) {
            throw new CursorException("Cannot advance a closed cursor.") ;
        }

        Object l_element = null ;
        try {
            l_element = advance() ;

            if (l_element == null) {
                try {
                    close() ;
                } catch(NamingException ne) {
					if( null != m_logger ) {
						m_logger.error("Failed to close this cursor on null return"
							+ " from advance():", ne) ;
					}
                }

                throw new
                    CursorException("Cursor advance returned null element!") ;
            }

            if(m_listeners.size() > 0) {
                Iterator l_list = m_listeners.iterator() ;
                CursorEvent l_event = new CursorEvent(this, l_element) ;

                while(l_list.hasNext()) {
                    CursorListener l_listener = (CursorListener) l_list.next() ;
                    l_listener.cursorAdvanced(l_event) ;
                }
            }

            return l_element ;
        } catch (BackendException e) {
			if( null != m_logger ) {
				m_logger.error("Backing store errors resulted in cursor failure " +
					"closing cursor:", e) ;
			}

            try {
                close() ;
            } catch(NamingException ne) {
				if( null != m_logger ) {
					m_logger.error("Failed to close this cursor on error:", ne) ;
				}
            }

            throw new CursorException(e) ;
        } catch (NamingException ne) {
			if( null != m_logger ) {
				m_logger.error("Failed on advance() closing cursor: ", ne) ;
			}

            try {
                close() ;
            } catch(NamingException ne2) {
				if( null != m_logger ) {
					m_logger.error("Failed to close this cursor on error:", ne2) ;
				}
            }

            throw new CursorException(ne) ;
        }
    }


    /**
     * Wrapper around advance which serves as an exception trap that transduces
     * BackendExceptions into CursorExceptions (a subclass of the expected RT
     * NoSuchElementException).
     *
     * @warning Invokations within the advance() implementations of subclasses
     * will result in a never ending chain recursion.
     * @return the next element in this Cursor.
     * @side-effect Closes this cursor before throwing any exceptions.
     */
    public final Object next()
        throws NamingException
    {
        if (m_isClosed) {
            throw new CursorException("Cannot advance a closed cursor.") ;
        }

        Object l_element = null ;
        try {
            l_element = advance() ;

            if (l_element == null) {
                close() ;
                throw new
                    CursorException("Cursor advance returned null element!") ;
            }

            return l_element ;
        } catch (BackendException e) {
			if( null != m_logger ) {
				m_logger.error("Backing store errors resulted in cursor failure " +
					"closing cursor:", e) ;
			}
            close() ;
            throw new CursorException(e) ;
        }
    }


    /**
     * Wrapper around canAdvance which serves as means to close this Cursor 
     * when the enumeration completes or an exception is encountered.
     *
     * @warning Invokations within the canAdvance implementations of subclasses
     * will result in a never ending chain recursion.
     * @return true if there exists a next element in this Cursor, else false
     * @side-effect Closes this cursor if canAdvance() returns false or if it
     * encounters a BackendException on the call to canAdvance().
     */
    public final boolean hasMoreElements()
    {
        if (m_isClosed) {
            return false ;
        }

        try {
            if (!canAdvance()) {
                try {
					if( null != m_logger ) {
						if(m_logger.isDebugEnabled() && debug) {
							m_logger.debug("Automagically closing exhausted "
								+ "cursor.") ;
						}
                    }

                    close() ;
                } catch(NamingException ne) {
					if( null != m_logger ) {
						m_logger.error("Failed close upon exhausting cursor.", ne);
					}
                }

                return false ;
            }

            return true ;
        } catch (BackendException e) {
			if( null != m_logger ) {
				m_logger.error("Backend errors closing cursor prematurely", e) ;
			}

            try {
                close() ;
            } catch(NamingException ne) {
				if( null != m_logger ) {
					m_logger.error("Failed close upon backend error:", ne) ;
				}
            }

            return false ;
        } catch (NamingException e) {
			if( null != m_logger ) {
				m_logger.error("Naming errors closing cursor prematurely", e) ;
			}

            try {
                close() ;
            } catch(NamingException ne) {
				if( null != m_logger ) {
					m_logger.error("Failed close upon backend error:", ne) ;
				}
            }

            return false ;
        }
    }


    /**
     * Wrapper around canAdvance which serves as a means to close this Cursor 
     * when the enumeration completes or an exception is encountered.
     *
     * @warning Invokations within the canAdvance implementations of subclasses
     * will result in a never ending chain recursion.
     * @return true if there exists a next element in this Cursor, else false
     * @side-effect Closes this cursor if canAdvance() returns false or if it
     * encounters a BackendException on the call to canAdvance().
     */
    public final boolean hasMore()
        throws NamingException
    {
        if (m_isClosed) {
            return false ;
        }

        try {
            if (!canAdvance()) {
				if( null != m_logger ) {
					if(m_logger.isDebugEnabled() && debug) {
						m_logger.debug("Automagically closing exhausted cursor.") ;
					}
				}

                close() ;
                return false ;
            }

            return true ;
        } catch (BackendException e) {
			if( null != m_logger ) {
				m_logger.error("Backend errors closing cursor prematurely", e) ;
			}
            close() ;
            return false ;
        }
    }


    //////////////////////
    // Abstract Methods //
    //////////////////////


    /**
     * Delagate method called by hasMore[Elements] to test if we can move
     * forward.  Use this method to hold the code that would have normally gone
     * into the hasMore[Elements] body. Unlike the hasMore[Elements] method the
     * method allows the backend developer to throw backend specific exceptions
     * which are eventually transduced into an automatic close of this Cursor.
     * After closing this Cursor BackendExceptions are not rethrown as runtime
     * exceptions to maintain [Naming]Enumeration semantics.
     *
     * @return true if Cursor can move to next non null entry, false otherwise.
     * @throws BackendException when a error occurs on the entry backing store.
     */
    protected abstract boolean canAdvance()
        throws BackendException, NamingException ;


    /**
     * Delagate method called by next[Element] to move this Cursor one element
     * forward.  Use this method to hold the code that would have normally gone
     * into the next[Element] body. Unlike the next[Element] method this method
     * allows the backend developer to throw backend specific exceptions.
     * BackendExceptions (which are not runtime exceptions) are transduced into
     * CursorExceptions (which are runtime exceptions derived from
     * NoSuchElementException).
     *
     * @return the element pointed to by this Cursor after advancing a step.
     * @throws BackendException when a error occurs on the entry backing store.
     */
    protected abstract Object advance()
        throws BackendException, NamingException ;


    /**
     * Called only once on cursor close to free up resources held by this
     * Cursor.
     */
    protected abstract void freeResources() ;
}

