/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.kerberos.shared.store;


import javax.security.auth.kerberos.KerberosPrincipal;


/**
 * The store interface used by Kerberos services.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:330489 $, $Date$
 */
public interface PrincipalStore
{
    /**
     * Add a principal.
     *
     * @param entry
     * @return The name of the principal being added.
     * @throws Exception
     */
    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception;


    /**
     * Change a principal's password.
     *
     * @param principal
     * @param newPassword
     * @return The name of the principal whose password is being changed.
     * @throws Exception
     */
    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception;


    /**
     * Delete a principal.
     *
     * @param principal
     * @return The name of the principal being deleted.
     * @throws Exception
     */
    public String deletePrincipal( KerberosPrincipal principal ) throws Exception;


    /**
     * Get all principals for a given realm.
     *
     * @param realm
     * @return An array of {@link PrincipalStoreEntry}'s.
     * @throws Exception
     */
    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception;


    /**
     * Get a {@link PrincipalStoreEntry} given a Kerberos principal.
     *
     * @param principal
     * @return The {@link PrincipalStoreEntry} for the given Kerberos principal.
     * @throws Exception
     */
    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception;
}
