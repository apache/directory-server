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


import java.util.Set;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetMatchedNameOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.shared.ldap.name.DN;


/**
 * An interface used by the DirectoryService to isolate operations that can be 
 * performed on it.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface OperationManager
{
    /**
     * TODO document after determining if this method should be here.
     */
    ClonedServerEntry getRootDSE( GetRootDSEOperationContext  opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    DN getMatchedName( GetMatchedNameOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    DN getSuffix ( GetSuffixOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    Set<String> listSuffixes( ListSuffixOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    boolean compare( CompareOperationContext opContext) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void delete( DeleteOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void add( AddOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void modify( ModifyOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    EntryFilteringCursor list( ListOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    EntryFilteringCursor search( SearchOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    ClonedServerEntry lookup( LookupOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    boolean hasEntry( EntryOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void rename( RenameOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void move( MoveOperationContext opContext ) throws Exception;


    /**
     * TODO document after determining if this method should be here.
     */
    void moveAndRename( MoveAndRenameOperationContext opContext ) throws Exception;

    
    /**
     * TODO document after determining if this method should be here.
     */
    void bind( BindOperationContext opContext ) throws Exception;

    
    /**
     * TODO document after determining if this method should be here.
     */
    void unbind( UnbindOperationContext opContext ) throws Exception;
}
