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
package org.apache.directory.server.core.api;


import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;


/**
 * An interface used by the DirectoryService to isolate operations that can be
 * performed on it.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface OperationManager
{
    /**
     * Add an entry into the backend, going through the interceptor chain
     * 
     * @param addContext The context containing the information to process the addition
     * @throws LdapException If the addition can't be processed successfully
     */
    void add( AddOperationContext addContext ) throws LdapException;


    /**
     * Get the RooDse entry.
     * 
     * @param getRootDseContext The getRootDse() context
     * @return The rootDse if found
     * @throws LdapException If we can't get back the rootDse entry
     */
    Entry getRootDse( GetRootDseOperationContext getRootDseContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    boolean compare( CompareOperationContext compareContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void delete( DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    Entry lookup( LookupOperationContext lookupContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void rename( RenameOperationContext renameContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void move( MoveOperationContext moveContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void bind( BindOperationContext bindContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void unbind( UnbindOperationContext unbindContext ) throws LdapException;
}
