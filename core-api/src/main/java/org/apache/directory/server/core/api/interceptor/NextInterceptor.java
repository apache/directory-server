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
package org.apache.directory.server.core.api.interceptor;


import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * Represents the next {@link Interceptor} in the interceptor chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @see Interceptor
 * @see InterceptorChain
 */
public interface NextInterceptor
{
    /**
     * Calls the next interceptor's {@link Interceptor#add( NextInterceptor, AddOperationContext )}.
     */
    void add( AddOperationContext addContext ) throws LdapException;


    /**
     * Calls the next interceptor's {@link Interceptor#modify( NextInterceptor, ModifyOperationContext )}.
     */
    void modify( ModifyOperationContext modifyContext ) throws LdapException;


    /**
     * Calls the next interceptor's {@link Interceptor#move( NextInterceptor, MoveOperationContext )}.
     */
    void move( MoveOperationContext moveContext ) throws LdapException;


    /**
     * Calls the next interceptor's {@link Interceptor#moveAndRename( NextInterceptor, MoveAndRenameOperationContext )}.
     */
    void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException;


    /**
     * Calls the next interceptor's {@link Interceptor#search( NextInterceptor, SearchOperationContext searchContext )}.
     */
    EntryFilteringCursor search( SearchOperationContext searchContext ) throws LdapException;
}
