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
package org.apache.directory.server.kerberos.shared.store;


import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.shared.ldap.model.name.Dn;


/**
 * A PrincipalStore backing entries in a DirectoryService.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DirectoryPrincipalStore implements PrincipalStore
{
    /** The directory service backing store for this PrincipalStore. */
    private final DirectoryService directoryService;
    private final Dn searchBaseDn;


    /**
     * Creates a new instance of DirectoryPrincipalStore.
     *
     * @param directoryService backing store for this PrincipalStore
     */
    public DirectoryPrincipalStore( DirectoryService directoryService, Dn searchBaseDn )
    {
        this.directoryService = directoryService;
        this.searchBaseDn = searchBaseDn;
    }


    /**
     * {@inheritDoc}
     */
    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        SingleBaseSearch singleBaseSearch = new SingleBaseSearch( directoryService, searchBaseDn );
        return singleBaseSearch.changePassword( principal, newPassword );
    }


    /**
     * {@inheritDoc}
     */
    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        SingleBaseSearch singleBaseSearch = new SingleBaseSearch( directoryService, searchBaseDn );
        return singleBaseSearch.getPrincipal( principal );
    }
}
