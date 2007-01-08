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
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Filters invocations on {@link PartitionNexus}.  {@link Interceptor}
 * filters most method calls performed on {@link PartitionNexus} just
 * like Servlet filters do.
 * <p/>
 * <h2>Interceptor Chaining</h2>
 * 
 * Interceptors should usually pass the control
 * of current invocation to the next interceptor by calling an appropriate method
 * on {@link NextInterceptor}.  The flow control is returned when the next 
 * interceptor's filter method returns. You can therefore implement pre-, post-,
 * around- invocation handler by how you place the statement.  Otherwise, you
 * can transform the invocation into other(s).
 * <p/>
 * <h3>Pre-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     System.out.println( "Starting invocation." );
 *     nextInterceptor.delete( name );
 * }
 * </pre>
 * <p/>
 * <h3>Post-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     nextInterceptor.delete( name );
 *     System.out.println( "Invocation ended." );
 * }
 * </pre>
 * <p/>
 * <h3>Around-invocation Filtering</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     long startTime = System.currentTimeMillis();
 *     try
 *     {
 *         nextInterceptor.delete( name );
 *     }
 *     finally
 *     {
 *         long endTime = System.currentTimeMillis();
 *         System.out.println( ( endTime - startTime ) + "ms elapsed." );
 *     }
 * }
 * </pre>
 * <p/>
 * <h3>Transforming invocations</h3>
 * <pre>
 * public void delete( NextInterceptor nextInterceptor, Name name )
 * {
 *     // transform deletion into modification.
 *     Attribute mark = new AttributeImpl( "entryDeleted", "true" );
 *     nextInterceptor.modify( name, DirContext.REPLACE_ATTRIBUTE, mark );
 * }
 * </pre>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 * @see NextInterceptor
 */
public interface Interceptor
{
    /**
     * Intializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is loaded into interceptor chain.
     */
    void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException;


    /**
     * Deinitializes this interceptor.  This is invoked by {@link InterceptorChain}
     * when this intercepter is unloaded from interceptor chain.
     */
    void destroy();


    /**
     * Filters {@link PartitionNexus#getRootDSE()} call.
     */
    Attributes getRootDSE( NextInterceptor next ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getMatchedName(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    LdapDN getMatchedName ( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#getSuffix(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    LdapDN getSuffix ( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#listSuffixes()} call.
     */
    Iterator listSuffixes ( NextInterceptor next ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#addContextPartition(PartitionConfiguration)} call.
     */
    void addContextPartition( NextInterceptor next, PartitionConfiguration cfg ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#removeContextPartition(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    void removeContextPartition( NextInterceptor next, LdapDN suffix ) throws NamingException;


    /**
     * Filters {@link PartitionNexus#compare(org.apache.directory.shared.ldap.name.LdapDN,String,Object)} call.
     */
    boolean compare( NextInterceptor next, LdapDN name, String oid, Object value ) throws NamingException;


    /**
     * Filters {@link Partition#delete(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    void delete( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link Partition#add(org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.Attributes)} call.
     */
    void add( NextInterceptor next, LdapDN name, Attributes entry ) throws NamingException;


    /**
     * Filters {@link Partition#modify(org.apache.directory.shared.ldap.name.LdapDN,int,javax.naming.directory.Attributes)} call.
     */
    void modify( NextInterceptor next, LdapDN name, int modOp, Attributes attributes ) throws NamingException;


    /**
     * Filters {@link Partition#modify(org.apache.directory.shared.ldap.name.LdapDN,javax.naming.directory.ModificationItem[])} call.
     */
    void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] items ) throws NamingException;


    /**
     * Filters {@link Partition#list(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    NamingEnumeration list( NextInterceptor next, LdapDN baseName ) throws NamingException;


    /**
     * Filters {@link Partition#search(org.apache.directory.shared.ldap.name.LdapDN,java.util.Map,org.apache.directory.shared.ldap.filter.ExprNode,javax.naming.directory.SearchControls)} call.
     */
    NamingEnumeration search( NextInterceptor next, LdapDN baseName, Map environment, ExprNode filter,
                              SearchControls searchControls ) throws NamingException;


    /**
     * Filters {@link Partition#lookup(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    Attributes lookup( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link Partition#lookup(org.apache.directory.shared.ldap.name.LdapDN,String[])} call.
     */
    Attributes lookup( NextInterceptor next, LdapDN dn, String[] attrIds ) throws NamingException;


    /**
     * Filters {@link Partition#lookup(org.apache.directory.shared.ldap.name.LdapDN,String[])} call.
     */
    boolean hasEntry( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link Partition#isSuffix(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    boolean isSuffix( NextInterceptor next, LdapDN name ) throws NamingException;


    /**
     * Filters {@link Partition#modifyRn(org.apache.directory.shared.ldap.name.LdapDN,String,boolean)} call.
     */
    void modifyRn( NextInterceptor next, LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException;


    /**
     * Filters {@link Partition#move(org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName ) throws NamingException;


    /**
     * Filters {@link Partition#move(org.apache.directory.shared.ldap.name.LdapDN,org.apache.directory.shared.ldap.name.LdapDN,String,boolean)} call.
     */
    void move( NextInterceptor next, LdapDN oldName, LdapDN newParentName, String newRn, boolean deleteOldRn )
        throws NamingException;


    /**
     * Filters {@link Partition#bind(org.apache.directory.shared.ldap.name.LdapDN,byte[],java.util.List,String)} call.
     */
    void bind( NextInterceptor next, LdapDN bindDn, byte[] credentials, List<String> mechanisms, String saslAuthId )
        throws NamingException;


    /**
     * Filters {@link Partition#unbind(org.apache.directory.shared.ldap.name.LdapDN)} call.
     */
    void unbind( NextInterceptor next, LdapDN bindDn ) throws NamingException;
}
