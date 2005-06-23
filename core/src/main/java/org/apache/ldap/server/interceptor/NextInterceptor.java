/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.interceptor;


import java.util.Iterator;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;


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
     * Calls the next interceptor's {@link Interceptor#getRootDSE(NextInterceptor)}.
     */
    Attributes getRootDSE() throws NamingException; 
    /**
     * Calls the next interceptor's {@link Interceptor#getMatchedDn(NextInterceptor, Name, boolean)}.
     */
    Name getMatchedDn( Name dn, boolean normalized ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#getSuffix(NextInterceptor, Name, boolean)}.
     */
    Name getSuffix( Name dn, boolean normalized ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#listSuffixes(NextInterceptor, boolean)}.
     */
    Iterator listSuffixes( boolean normalized ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#delete(NextInterceptor, Name)}.
     */
    void delete( Name name ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#add(NextInterceptor, String, Name, Attributes)}.
     */
    void add( String upName, Name normName, Attributes entry ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#modify(NextInterceptor, Name, int, Attributes)}.
     */
    void modify( Name name, int modOp, Attributes mods ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#modify(NextInterceptor, Name, ModificationItem[])}.
     */
    void modify( Name name, ModificationItem [] mods ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#list(NextInterceptor, Name)}.
     */
    NamingEnumeration list( Name base ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#search(NextInterceptor, Name, Map, ExprNode, SearchControls)}.
     */
    NamingEnumeration search( Name base, Map env, ExprNode filter,
                              SearchControls searchCtls ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#lookup(NextInterceptor, Name)}.
     */
    Attributes lookup( Name name ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#lookup(NextInterceptor, Name, String[])}.
     */
    Attributes lookup( Name dn, String [] attrIds ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#hasEntry(NextInterceptor, Name)}.
     */
    boolean hasEntry( Name name ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#isSuffix(NextInterceptor, Name)}.
     */
    boolean isSuffix( Name name ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#modifyRn(NextInterceptor, Name, String, boolean)}.
     */
    void modifyRn( Name name, String newRn, boolean deleteOldRn ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#move(NextInterceptor, Name, Name)}.
     */
    void move( Name oriChildName, Name newParentName ) throws NamingException;
    /**
     * Calls the next interceptor's {@link Interceptor#move(NextInterceptor, Name, Name, String, boolean)}.
     */
    void move( Name oriChildName, Name newParentName, String newRn,
               boolean deleteOldRn ) throws NamingException;
}
