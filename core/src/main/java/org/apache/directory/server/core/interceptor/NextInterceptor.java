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
package org.apache.directory.server.core.interceptor;


import java.util.Iterator;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;

import org.apache.directory.server.core.interceptor.context.AddContextPartitionOperationContext;
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
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.name.LdapDN;

/**
 * Represents the next {@link Interceptor} in the interceptor chain.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see Interceptor
 * @see InterceptorChain
 */
public interface NextInterceptor
{
    /**
     * Calls the next interceptor's {@link Interceptor#compare( NextInterceptor, CompareOperationContext )}.
     */
    boolean compare( CompareOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getRootDSE( NextInterceptor, GetRootDSEOperationContext )}.
     */
    Attributes getRootDSE( GetRootDSEOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getMatchedName( NextInterceptor, GetMatchedNameOperationContext )}.
     */
    LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getSuffix( NextInterceptor, GetSuffixOperationContext )}.
     */
    LdapDN getSuffix( GetSuffixOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#listSuffixes( NextInterceptor, ListSuffixOperationContext )}.
     */
    Iterator listSuffixes( ListSuffixOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#addContextPartition( nextInterceptor, AddContextPartitionOperationContext )}.
     */
    void addContextPartition( AddContextPartitionOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#removeContextPartition( NextInterceptor, RemoveContextPartitionOperationContext )}.
     */
    void removeContextPartition( RemoveContextPartitionOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#delete(NextInterceptor, DeleteOperationContext )}.
     */
    void delete( DeleteOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#add( NextInterceptor, AddOperationContext )}.
     */
    void add( AddOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#modify( NextInterceptor, ModifyOperationContext )}.
     */
    void modify( ModifyOperationContext opContext ) throws NamingException;

    /**
     * Calls the next interceptor's {@link Interceptor#list( NextInterceptor, ListOperationContext )}.
     */
    NamingEnumeration list( ListOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#search( NextInterceptor, SearchOperationContext opContext )}.
     */
    NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
        throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#lookup( NextInterceptor, LookupOperationContext )}.
     */
    Attributes lookup( LookupOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#hasEntry( NextInterceptor, EntryOperationContext )}.
     */
    boolean hasEntry( EntryOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#rename( NextInterceptor, RenameOperationContext )}.
     */
    void rename( RenameOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#move( NextInterceptor, MoveOperationContext )}.
     */
    void move( MoveOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#moveAndRename( NextInterceptor, MoveAndRenameOperationContext )}.
     */
    void moveAndRename( MoveAndRenameOperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#bind( NextInterceptor, BindOperationContext )}
     */
    void bind( BindOperationContext opContext ) throws NamingException;

    /**
     * Calls the next interceptor's {@link Interceptor#unbind( NextInterceptor, UnbindOperationContext )}
     */
    void unbind( UnbindOperationContext opContext ) throws NamingException;
}
