/*
 * $Id: Module.java,v 1.4 2003/08/22 21:15:54 akarasulu Exp $
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve ;

import org.apache.avalon.framework.activity.Startable ;
import org.apache.avalon.framework.thread.ThreadSafe ;
import org.apache.avalon.framework.logger.LogEnabled ;
import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.configuration.Configurable ;
import org.apache.avalon.framework.service.Serviceable ;
import org.apache.avalon.framework.activity.Initializable ;
import org.apache.avalon.framework.context.Contextualizable ;


/**
 * Modules are pluggable black box components that can be started and stopped
 * and are responsible for their own error handling and logging.  Some types of
 * modules within ldapd are backend modules and replication modules.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.4 $
 */
public interface Module
    extends
    Startable,
    ThreadSafe,
    LogEnabled,
    Serviceable,
    Configurable,
    Contextualizable,
	Initializable
{
    /**
     * Gets the service interface name of this module.
     *
     * @return the role of this module's implemented service.
     */
    String getImplementationRole() ;

    /**
     * Gets the name of the implementation.  For example the name of the
     * Berkeley DB Backend module is "Berkeley DB Backend".
     *
     * @return String representing the module implementation type name.
     */
	String getImplementationName() ;

    /**
     * Gets the name of the implementation class.  For example the name of the
     * Berkeley DB Backend implementation class is <code>
     * "ldapdd.backend.berkeley.BackendBDb" </code>.
     *
     * @return String representing the module implementation's class name.
     */
    String getImplementationClassName() ;


    /**
     * Checks to see if this module has already started.
     *
     * @return true if the module started, false otherwise.
     */
    boolean hasStarted() ;

    /**
     * Gets the logger used by this module to log messages.
     *
     * @return the logger used by this module.
     */
    Logger getLogger() ;
}

