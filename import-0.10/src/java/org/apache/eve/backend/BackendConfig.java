/*
 * $Id: BackendConfig.java,v 1.4 2003/03/13 18:26:41 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.backend ;

import java.util.Iterator ;
import javax.naming.InvalidNameException ;

import org.apache.avalon.framework.configuration.Configuration ;
import org.apache.avalon.framework.configuration.ConfigurationException ;
import javax.naming.Name;


/**
 * Interface used to manage the configuration of an AtomicBackend.
 */
public interface BackendConfig
{
    /** The default entry cache size. */
    int DEFAULT_ENTRY_CACHESZ = 1000 ;

    /**
     * Tests to see if the backend of this configuration is operating in read
     * only mode.  If true alterations to the backend will result in
     * ReadOnlyExceptions being thrown at runtime.
     *
     * @return true if the backend is in read only mode false otherwise.
     */
    boolean isReadOnly() ;

    /**
     * Sets the read only operational mode of a backend using this
     * configuration.
     *
     * @param a_isReadOnlyMode true if backend is to be configured for read 
     * only mode false otherwise.
     */
    void setReadOnly(boolean a_isReadOnlyMode) ;

    /**
     * Sets the maximum size the entry cache may grow to.
     *
     * @param a_numMaxEntries the maximum number of entries that can be held.
     * @throws BackendException if the configurable module has started and is 
     * not Reconfigurable.
     */
    void setEntryCacheSize(int a_numMaxEntries)
        throws BackendException ;

    /**
     * Gets the absolute path to the working directory for the configurable 
     * backend.
     *
     * @return the absolute working directory path.
     */
    String getWorkingDirPath() ;

    /**
     * Sets the working directory path for implementation specific files or 
     * logs if any.
     *
     * @param a_dirPath the absolute path to the working directory for the 
     * configurable backend.
     * @throws BackendException if the configurable module has started and is 
     * not Reconfigurable.
     */
    void setWorkingDirPath(String a_dirPath)
        throws BackendException ;

    /**
     * Sets the password for the admin user for the configurable backend.
     *
     * @param an_adminDN the distinguished name of the backend admin.
     * @throws InvalidNameException if an_adminDN does not conform to DN 
     * syntax.
     * @throws BackendException if the configurable module has started and is 
     * not Reconfigurable.
     * @throws InvalidNameException if an_adminDN not in correct DN syntax 
     */
    void setAdminUserDN(Name an_adminDN)
        throws BackendException, InvalidNameException ;

    /**
     * Gets the admin user distinguished name for the configurable backend.
     *
     * @return the distinguished name of the admin user.
     */
    Name getAdminUserDN() ;

    /**
     * Sets the password for the admin user for the configurable backend.
     *
     * @param an_adminPassword the distinguished name of the backend admin.
     * @throws BackendException if the configurable module has started and is 
     * not Reconfigurable.
     */
    void setAdminUserPassword(String an_adminPassword)
        throws BackendException ;

    /**
     * Gets the encrypted password of the user in base 64 encoded format.
     *
     * @return base64 encoded cypher.
     */
    String getAdminUserPassword() ;

    /**
     * Gets the maximum size the entry cache may grow to.
     *
     * @return the maximum number of entries that can be held.
     */
    int getEntryCacheSize() ;
}
