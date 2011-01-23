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


import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * An interface used by the DirectoryService to isolate operations that can be 
 * performed on it.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface OperationManager
{
    /**
     * TODO document after determining if this method should be here.
     */
    Entry getRootDSE( GetRootDSEOperationContext  getRootDseContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    boolean compare( CompareOperationContext compareContext) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void delete( DeleteOperationContext deleteContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void add( AddOperationContext addContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * TODO document after determining if this method should be here.
     */
    EntryFilteringCursor list( ListOperationContext listContext ) throws LdapException;


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
    boolean hasEntry( EntryOperationContext hasEntryContext ) throws LdapException;


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
