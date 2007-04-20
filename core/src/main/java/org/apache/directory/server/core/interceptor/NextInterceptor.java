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
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.filter.ExprNode;
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
     * Calls the next interceptor's {@link Interceptor#compare( NextInterceptor, OperationContext )}.
     */
    boolean compare( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getRootDSE( NextInterceptor, OperationContext )}.
     */
    Attributes getRootDSE( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getMatchedName( NextInterceptor, OperationContext )}.
     */
    LdapDN getMatchedName( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getSuffix( NextInterceptor, OperationContext )}.
     */
    LdapDN getSuffix( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#listSuffixes( NextInterceptor, OperationContext )}.
     */
    Iterator listSuffixes( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#addContextPartition( nextInterceptor, OperationContext )}.
     */
    void addContextPartition( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#removeContextPartition( NextInterceptor, OperationContext )}.
     */
    void removeContextPartition( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#delete(NextInterceptor, OperationContext )}.
     */
    void delete( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#add( NextInterceptor, OperationContext )}.
     */
    void add( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#modify( NextInterceptor, OperationContext )}.
     */
    void modify( OperationContext opContext ) throws NamingException;

    /**
     * Calls the next interceptor's {@link Interceptor#list( NextInterceptor, OperationContext )}.
     */
    NamingEnumeration list( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#search( NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,java.util.Map,org.apache.directory.shared.ldap.filter.ExprNode,javax.naming.directory.SearchControls)}.
     */
    NamingEnumeration search( LdapDN baseName, Map environment, ExprNode filter, SearchControls searchControls )
        throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#lookup( NextInterceptor, OperationContext )}.
     */
    Attributes lookup( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#hasEntry( NextInterceptor, OperationContext )}.
     */
    boolean hasEntry( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#rename( NextInterceptor, OperationContext )}.
     */
    void rename( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#move( NextInterceptor, OperationContext )}.
     */
    void move( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#moveAndRename( NextInterceptor, OperationContext )}.
     */
    void moveAndRename( OperationContext opContext ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#bind( NextInterceptor, OperationContext )}
     */
    void bind( OperationContext opContext ) throws NamingException;

    /**
     * Calls the next interceptor's {@link Interceptor#unbind( NextInterceptor, OperationContext )}
     */
    void unbind( OperationContext opContext ) throws NamingException;
}
