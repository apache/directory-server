/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core;


import java.net.SocketAddress;
import java.util.Set;

import javax.naming.ldap.Control;

import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;


/**
 * An interface representing a session with the core DirectoryService. These 
 * sessions may either be real representing LDAP sessions associated with an 
 * actual LDAP network client, or may be virtual in which case there is no 
 * real LDAP client associated with the session.  This interface is used by 
 * the DirectoryService core to track session specific parameters used to make 
 * various decisions during the course of operation handling.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface CoreSession
{
    /**
     * Gets the DirectoryService this session is bound to.
     *
     * @return the DirectoryService associated with this session
     */
    DirectoryService getDirectoryService();

    
    /**
     * Gets the LDAP principal used to authenticate.  This is the identity 
     * used to establish this session on authentication.
     *
     * @return the LdapPrincipal used to authenticate.
     */
    LdapPrincipal getAuthenticatedPrincipal();
    
    
    /**
     * Gets the LDAP principal used for the effective identity associated with
     * this session which may not be the same as the authenticated principal.  
     * This principal is often the same as the authenticated principal.  
     * Sometimes however, a user authenticating as one principal, may request 
     * to have all operations performed in the session as if they were another 
     * principal.  The SASL mechanism allows setting an authorized principal 
     * which is in effect for the duration of the session.  In this case all 
     * operations are performed as if they are being performed by this 
     * principal.  This method will then return the authorized principal which
     * will be different from the authenticated principal.
     * 
     * Implementations of this interface may have a means to set the 
     * authorized principal which may or may not be the same as the 
     * authenticated principal.  Implementations should default to return the 
     * authenticated principal when an authorized principal is not provided.
     *
     * @return the LdapPrincipal to use as the effective principal
     */
    LdapPrincipal getEffectivePrincipal();
    
    
    /**
     * Gets whether or not confidentiality is enabled for this session.
     * 
     * @return true if confidentiality is enabled, false otherwise
     */
    boolean isConfidential();
    
    
    /**
     * Gets the authentication level associated with this session.
     * 
     * @return the authentication level associated with the session
     */
    AuthenticationLevel getAuthenticationLevel();
    
    
    /**
     * Gets the controls enabled for this session.
     * 
     * @return the session controls as a Set
     */
    Set<Control> getControls();
    
    
    /**
     * Gets all outstanding operations currently being performed that have yet 
     * to be completed.
     * 
     * @return the set of outstanding operations
     */
    Set<OperationContext> getOutstandingOperations();

    
    /**
     * Gets whether or not this session is virtual.  Virtual sessions verses 
     * real sessions represent logical sessions established by non-LDAP 
     * services or embedding applications which do not expose the LDAP access.
     *
     * @return true if the session is virtual, false otherwise
     */
    boolean isVirtual();
    
    
    /**
     * Gets the socket address of the LDAP client or null if there is no LDAP
     * client associated with the session.  Some calls to the core can be made
     * by embedding applications or by non-LDAP services using a programmatic
     * (virtual) session.  In these cases no client address is available.
     * 
     * @return null if the session is virtual, non-null when the session is 
     * associated with a real LDAP client
     */
    SocketAddress getClientAddress();


    /**
     * Gets the socket address of the LDAP server or null if there is no LDAP
     * service associated with the session.  Some calls to the core can be 
     * made by embedding applications or by non-LDAP services using a 
     * programmatic (virtual) session.  In these cases no service address is 
     * available.
     * 
     * @return null if the session is virtual, non-null when the session is 
     * associated with a real LDAP service
     */
    SocketAddress getServiceAddress();
}
