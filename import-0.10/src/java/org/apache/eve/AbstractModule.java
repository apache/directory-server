/*
 * $Id: AbstractModule.java,v 1.2 2003/03/13 18:26:29 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve ;

import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;


/**
 * Abstract module class provided for convenience.  Provides start, stop and
 * logger methods out of the box.  Subclasses that override start and stop must
 * call start() and stop() super class methods after performing their required
 * opertations in the respective override.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.2 $
 */
public abstract class AbstractModule
    implements Module
{
    /** The logger used by this LogEnabled module. */
    private Logger m_logger = null ;
    /** Member used to track whether or not this module has been started. */
    private boolean m_hasStarted = false ;



	public void service(ServiceManager a_manager)
        throws ServiceException
    {
    }


	public void initialize()
        throws Exception
    {
    }


    /**
     * Starts this module.  All subclasses much call this super method after
     * performing their own start tasks.
     *
     * @throws Exception of any kind subclasses of which would depend on the
     * nature of derived concrete modules.
     */
    public void start()
        throws Exception
    {
        m_hasStarted = true ;
    }


    /**
     * Stops this module.  All subclasses much call this super method after
     * performing their own stop tasks.
     * 
     * @throws Exception of any kind subclasses of which would depend on the
     * nature of derived concrete modules.
     */
    public void stop()
        throws Exception
    {
        m_hasStarted = false ;
    }


    /**
     * Checks to see if this module has started.
     *
     * @return true if it has started, false otherwise.
     */
    public final boolean hasStarted()
    {
        return m_hasStarted ;
    }


    /**
     * Gets the Logger used by this module to log messages.
     *
     * @return this modules Logger.
     */
    public final Logger getLogger()
    {
        return m_logger ;
    }


    /**
     * LogEnabled interface implementation which sets this Modules or LogEnabled
     * class' Logger.
     *
     * @param a_logger used by this LogEnabled module.
     */
    public void enableLogging(Logger a_logger)
    {
        m_logger = a_logger ;
    }


    public void contextualize(Context a_context)
        throws ContextException
    {
        if(getLogger().isDebugEnabled()) {
        	getLogger().debug(this.getImplementationName() +
                " executing contextualize phase") ;
        }
    }
}

