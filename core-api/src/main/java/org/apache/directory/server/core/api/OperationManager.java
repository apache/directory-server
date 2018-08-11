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


import java.util.concurrent.locks.ReadWriteLock;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDseOperationContext;
import org.apache.directory.server.core.api.interceptor.context.HasEntryOperationContext;
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
     * 
     * @param compareContext The Compare operation context
     * @return <tt>true</tt> if the comparison is successful
     * @throws LdapException If the compare failed
     */
    boolean compare( CompareOperationContext compareContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param deleteContext The Delete operation context
     * @throws LdapException If the delete failed
     */
    void delete( DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param modifyContext The Modify operation context
     * @throws LdapException If the modify failed
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param searchContext The Search operation context
     * @return The cursor on the found entries
     * @throws LdapException If the search failed
     */
    EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param lookupContext The Lookup operation context
     * @return The found entry
     * @throws LdapException If the lookup failed
     */
    Entry lookup( LookupOperationContext lookupContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param hasEntryContext The HasEntry operation context
     * @return <tt>true</tt> if the entry exists
     * @throws LdapException If the hasEntry failed
     */
    boolean hasEntry( HasEntryOperationContext hasEntryContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param renameContext The Rename operation context
     * @throws LdapException If the rename failed
     */
    void rename( RenameOperationContext renameContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param moveContext The Move operation context
     * @throws LdapException If the move failed
     */
    void move( MoveOperationContext moveContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param moveAndRenameContext The MoveAndRename operation context
     * @throws LdapException If the moveAndRename failed
     */
    void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param bindContext The Bind operation context
     * @throws LdapException If the bind failed
     */
    void bind( BindOperationContext bindContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     * 
     * @param unbindContext The Unbind operation context
     * @throws LdapException If the unbind failed
     */
    void unbind( UnbindOperationContext unbindContext ) throws LdapException;


    /**
     * Acquires a WriteLock
     */
    void lockWrite();


    /**
     * Releases a WriteLock
     */
    void unlockWrite();


    /**
     * Acquires a ReadLock
     */
    void lockRead();


    /**
     * Releases a ReadLock
     */
    void unlockRead();


    /**
     * @return the OperationManager R/W lock
     */
    ReadWriteLock getRWLock();
}
