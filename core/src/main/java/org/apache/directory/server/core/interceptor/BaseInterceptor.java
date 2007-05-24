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

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A easy-to-use implementation of {@link Interceptor}.  All methods are
 * implemented to pass the flow of control to next interceptor by defaults.
 * Please override the methods you have concern in.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class BaseInterceptor implements Interceptor
{
    /**
     * Returns {@link LdapPrincipal} of current context.
     */
    public static LdapPrincipal getPrincipal()
    {
        ServerContext ctx = ( ServerContext ) getContext();
        return ctx.getPrincipal();
    }


    /**
     * Returns the current JNDI {@link Context}.
     */
    public static LdapContext getContext()
    {
        return ( LdapContext ) InvocationStack.getInstance().peek().getCaller();
    }


    /**
     * Creates a new instance.
     */
    protected BaseInterceptor()
    {
    }


    /**
     * This method does nothing by default.
     */
    public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg ) throws NamingException
    {
    }


    /**
     * This method does nothing by default.
     */
    public void destroy()
    {
    }


    // ------------------------------------------------------------------------
    // Interceptor's Invoke Method
    // ------------------------------------------------------------------------

    public void add(NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.add( opContext );
    }


    public void delete( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.delete( opContext );
    }


    public LdapDN getMatchedName ( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.getMatchedName( opContext );
    }


    public Attributes getRootDSE( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.getRootDSE( opContext );
    }


    public LdapDN getSuffix( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.hasEntry( opContext );
    }


    public NamingEnumeration list( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.list( opContext );
    }


    public Iterator listSuffixes ( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.listSuffixes( opContext );
    }


    public Attributes lookup( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.lookup( opContext );
    }

    
    public void modify( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.modify( opContext );
    }


    public void rename( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.rename( opContext );
    }


    public void moveAndRename( NextInterceptor next, OperationContext opContext )
        throws NamingException
    {
        next.moveAndRename( opContext );
    }


    public void move( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.move( opContext );
    }


    public NamingEnumeration<SearchResult> search( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.search( opContext );
    }


    public void addContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.removeContextPartition( opContext );
    }


    public boolean compare( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        return next.compare( opContext );
    }


    public void bind( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.bind( opContext );
    }


    public void unbind( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        next.unbind( opContext );
    }
}
