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
import java.util.List;
import java.util.Map;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.configuration.PartitionConfiguration;
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
     * Calls the next interceptor's {@link Interceptor#compare(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,String,Object)}.
     */
    boolean compare( LdapDN name, String oid, Object value ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getRootDSE(NextInterceptor)}.
     */
    Attributes getRootDSE() throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getMatchedName(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    LdapDN getMatchedName ( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#getSuffix(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    LdapDN getSuffix ( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#listSuffixes(NextInterceptor)}.
     */
    Iterator listSuffixes () throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#addContextPartition(PartitionConfiguration)}.
     */
    void addContextPartition( PartitionConfiguration cfg ) throws NamingException;


    /**
     * Calls the next interceptor's {@link PartitionNexus#removeContextPartition(org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    void removeContextPartition( LdapDN suffix ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#delete(NextInterceptor, org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    void delete( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#add(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.Attributes)}.
     */
    void add(LdapDN normName, Attributes entry) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#modify(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,int,javax.naming.directory.Attributes)}.
     */
    void modify( LdapDN name, int modOp, Attributes attributes ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#modify(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.ModificationItem[])}.
     */
    void modify( LdapDN name, ModificationItem[] items ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#list(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    NamingEnumeration list( LdapDN baseName ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#search(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,java.util.Map,org.apache.directory.shared.ldap.filter.ExprNode,javax.naming.directory.SearchControls)}.
     */
    NamingEnumeration search( LdapDN baseName, Map environment, ExprNode filter, SearchControls searchControls )
        throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#lookup(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    Attributes lookup( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#lookup(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,String[])}.
     */
    Attributes lookup( LdapDN name, String[] attrIds ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#hasEntry(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    boolean hasEntry( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#isSuffix(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    boolean isSuffix( LdapDN name ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#modifyRn(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,String,boolean)}.
     */
    void modifyRn( LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#move(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN)}.
     */
    void move( LdapDN oldName, LdapDN newParentName ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#move(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN,String,boolean)}.
     */
    void move( LdapDN oldName, LdapDN newParentName, String newRn, boolean deleteOldRn ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#bind(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN,byte[],java.util.List,String)
     */
    void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException;


    /**
     * Calls the next interceptor's {@link Interceptor#unbind(NextInterceptor,org.apache.directory.shared.ldap.name.LdapDN)
     */
    void unbind( LdapDN bindDn ) throws NamingException;
}
