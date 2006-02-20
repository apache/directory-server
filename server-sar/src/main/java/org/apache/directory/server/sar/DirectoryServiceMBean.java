/*
 *   @(#) $Id$
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
package org.apache.directory.server.sar;


import org.w3c.dom.Element;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;


/**
 * JBoss 3.x Mbean interface for embedded and remote directory server support
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 * @version $Rev$, $Date$
 */
public interface DirectoryServiceMBean extends org.jboss.system.ServiceMBean
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Gets the root element of the XML properties list of defined LDIF filters
     *
     * @return The root DOM element
     */
    public Element getEmbeddedLDIFFilters();


    /**
     * Sets the root element of the XML properties list of defined LDIF filters
     *
     * @param fil The root DOM element
     */
    public void setEmbeddedLDIFFilters( Element fil );


    /**
     * Gets the root element of the XML properties list of additional
     * environment properties
     *
     * @return The root DOM element
     */
    public Element getEmbeddedAdditionalEnvProperties();


    /**
     * Sets the root element of the XML properties list of additional
     * environment properties
     *
     * @param env The root DOM element
     */
    public void setEmbeddedAdditionalEnvProperties( Element env );


    /**
     * Gets the root element of the XML properties list of custom bootstrap
     * schema properties
     *
     * @return The root DOM element
     */
    public Element getEmbeddedCustomBootstrapSchema();


    /**
     * Sets the root element of the XML properties list of custom bootstrap
     * schema properties
     *
     * @param cfg The root DOM element
     */
    public void setEmbeddedCustomBootstrapSchema( Element cfg );


    /**
     * Test to see if the directory service to use is embedded in this VM
     *
     * @return True if embedded else false
     */
    public boolean isEmbeddedServerEnabled();


    /**
     * Set if the directory service to use is embedded in this VM
     *
     * @param enabled True if embedded else false
     */
    public void setEmbeddedServerEnabled( boolean enabled );


    /**
     * Gets the name-to-object binding for Context INITIAL_CONTEXT_FACTORY
     *
     * @return Context.INITIAL_CONTEXT_FACTORY
     */
    public String getContextFactory();


    /**
     * Sets the name-to-object binding for Context INITIAL_CONTEXT_FACTORY
     *
     * @param factoryClass Context.INITIAL_CONTEXT_FACTORY value
     */
    public void setContextFactory( String factoryClass );


    /**
     * Gets the name-to-object binding for Context PROVIDER_URL
     *
     * @return Context.PROVIDER_URL
     */
    public String getContextProviderURL();


    /**
     * Sets the name-to-object binding for Context PROVIDER_URL
     *
     * @param providerURL Context.PROVIDER_URL value
     */
    public void setContextProviderURL( String providerURL );


    /**
     * Gets the name-to-object binding for Context SECURITY_AUTHENTICATION
     *
     * @return Context.SECURITY_AUTHENTICATION
     */
    public String getContextSecurityAuthentication();


    /**
     * Sets the name-to-object binding for Context SECURITY_AUTHENTICATION
     *
     * @param securityAuthentication Context.SECURITY_AUTHENTICATION value
     */
    public void setContextSecurityAuthentication( String securityAuthentication );


    /**
     * Gets the name-to-object binding for Context SECURITY_PRINCIPAL
     *
     * @return Context.SECURITY_PRINCIPAL
     */
    public String getContextSecurityPrincipal();


    /**
     * Sets the name-to-object binding for Context SECURITY_PRINCIPAL
     *
     * @param securityPrincipal Context.SECURITY_PRINCIPAL value
     */
    public void setContextSecurityPrincipal( String securityPrincipal );


    /**
     * Gets the name-to-object binding for Context SECURITY_CREDENTIALS
     *
     * @return Context.SECURITY_CREDENTIALS
     */
    public String getContextSecurityCredentials();


    /**
     * Sets the name-to-object binding for Context SECURITY_CREDENTIALS
     *
     * @param securityCredentials Context.SECURITY_CREDENTIALS value
     */
    public void setContextSecurityCredentials( String securityCredentials );


    /**
     * Opens a directory context based on the currently assigned name-to-object
     * bindings
     *
     * @return A valid directory context or null on error
     */
    public DirContext openDirContext() throws NamingException;


    /**
     * Embedded server only - Flushes out any I/O buffer or write cache
     *
     * @return True if flush succeeded else false
     */
    public boolean flushEmbeddedServerData();


    /**
     * Embedded server only - Changes the current password of the uid=admin
     * user
     *
     * @param oldPassword Old password for verification
     * @param newPassword New password to use
     *
     * @return Confirmation message for UI display
     */
    public String changedEmbeddedAdminPassword( String oldPassword, String newPassword );


    /**
     * Embedded server only - tests if anonymous access is permitted
     *
     * @return True if permitted else false
     */
    public boolean isEmbeddedAnonymousAccess();


    /**
     * Embedded server only - sests if anonymous access is permitted
     *
     * @param anonymousAccess True to allow else false
     */
    public void setEmbeddedAnonymousAccess( boolean anonymousAccess );


    /**
     * Embedded server only - tests if LDAP wire protocol handler is to be
     * started
     *
     * @return True if LDAP wire protocol in use else false
     */
    public boolean isEmbeddedLDAPNetworkingSupport();


    /**
     * Embedded server only - sests if LDAP wire protocol handler is to be
     * started
     *
     * @param ldapNetworkingSupport True to install LDAP support else false
     */
    public void setEmbeddedLDAPNetworkingSupport( boolean ldapNetworkingSupport );


    /**
     * Embedded server only - gets the LDAP listen port
     *
     * @return LDAP listen port
     */
    public int getEmbeddedLDAPPort();


    /**
     * Embedded server only - sets the LDAP listen port
     *
     * @param ldapPort The LDAP port listened on
     */
    public void setEmbeddedLDAPPort( int ldapPort );


    /**
     * Embedded server only - gets the LDAPSSL) listen port (!NOT YET
     * SUPPORTED!)
     *
     * @return LDAPS listen port
     */
    public int getEmbeddedLDAPSPort();


    /**
     * Embedded server only - sets the LDAPS (SSL) listen port (!NOT YET
     * SUPPORTED!)
     *
     * @param ldapsPort The LDAPS port listened on
     */
    public void setEmbeddedLDAPSPort( int ldapsPort );


    /**
     * Embedded server only - Gets the name of the root partion which was
     * automatically created on server startup
     *
     * @return The name of the custom root partition (null for no custom
     *         partition)
     */
    public String getEmbeddedCustomRootPartitionName();


    /**
     * Embedded server only - Sets the name of the root partion which is
     * automatically created on server startup
     *
     * @param rootPartitianName The name of the custom root partition (null for
     *        no partition)
     */
    public void setEmbeddedCustomRootPartitionName( String rootPartitianName );


    /**
     * Embedded server only - Gets the name of the workfile folder used by the
     * server
     *
     * @return Folder name
     */
    public String getEmbeddedWkdir();


    /**
     * Embedded server only - Sets the name of the workfile folder used by the
     * server
     *
     * @param wkdir Folder name
     */
    public void setEmbeddedWkdir( String wkdir );


    /**
     * Embedded server only - Gets the name of the LDIF import folder used by
     * the server
     *
     * @return LDIF import folder
     */
    public String getEmbeddedLDIFdir();


    /**
     * Embedded server only - Sets the name of the LDIF import folder used by
     * the server
     *
     * @param LDIFdir LDIF import folder
     */
    public void setEmbeddedLDIFdir( String LDIFdir );


    /**
     * Embedded server only - test if access control is enabled
     *
     * @return True is enabled else false
     */
    public boolean isEmbeddedAccessControlEnabled();


    /**
     * Embedded server only - Set if access control is enabled
     *
     * @param enabled True to enable else false
     */
    public void setEmbeddedAccessControlEnabled( boolean enabled );


    /**
     * Embedded server only - test if NTP wire protocol is enabled
     *
     * @return True is enabled else false
     */
    public boolean isEmbeddedEnableNtp();


    /**
     * Embedded server only - set if NTP wire protocol is enabled
     *
     * @param enabled True to enable else false
     */
    public void setEmbeddedEnableNtp( boolean enabled );


    /**
     * Embedded server only - test if Kerberos wire protocol is enabled
     *
     * @return True is enabled else false
     */
    public boolean isEmbeddedEnableKerberos();


    /**
     * Embedded server only - set if Kerberos wire protocol is enabled
     *
     * @param enabled True to enable else false
     */
    public void setEmbeddedEnableKerberos( boolean enabled );


    /**
     * Embedded server only - test if Change Password wire protocol is enabled
     *
     * @return True is enabled else false
     */
    public boolean isEmbeddedEnableChangePassword();


    /**
     * Embedded server only - set if Change Password wire protocol is enabled
     *
     * @param enabled True to enable else false
     */
    public void setEmbeddedEnableChangePassword( boolean enabled );
}
