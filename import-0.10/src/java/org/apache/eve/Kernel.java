/*
 * $Id: Kernel.java,v 1.4 2003/08/22 21:15:54 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve ;


import org.apache.avalon.framework.logger.Logger ;
import org.apache.avalon.framework.service.ServiceManager ;


/**
 * Some very primitive functionality to a kernel used to fire up the server or
 * just parts of it.
 */
public interface Kernel
{
    /**
     * Gets path to the root directory where the sar has been unraveled.
     */
    String getRoot() ;

    /**
     * Sets the root directory where the sar has been unraveled.  Must be set
     * before bootstraping.
     */
    void setRoot( String a_rootDirPath ) ;

    /**
     * Starts up the kernel starting a single module and all modules it depends
     * on.  Using this configuration backend subsystems alone can be started
     * without firing up the entire server.
     */
    void bootStrap( String a_module ) throws Exception ;

    /**
     * Starts up the entire server with all the modules.
     */
    void bootStrap() throws Exception ;

    void shutdown() ;

    /**
     * Gets the service manager used by components to resolve dependent
     * services exposed by server plugins.
     */
    ServiceManager getServiceManager() ;

    /**
     * Gets the system logger for this Kernel
     */
    Logger getLogger() ;

    /**
     * Gets the component logger for a module in this Kernel.
     */
    Logger getLogger( String a_module ) ;
}
