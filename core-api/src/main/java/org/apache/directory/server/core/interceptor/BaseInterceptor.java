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


import java.util.Set;

import javax.naming.Context;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.interceptor.context.GetSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.interceptor.context.ListSuffixOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A easy-to-use implementation of {@link Interceptor}.  All methods are
 * implemented to pass the flow of control to next interceptor by defaults.
 * Please override the methods you have concern in.
 *
 * @org.apache.xbean.XBean
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class BaseInterceptor implements Interceptor
{
    /**
     * default interceptor name is its class, preventing accidental duplication of interceptors by naming
     * instances differently
     * @return (default, class name) interceptor name
     */
    public String getName()
    {
        return getClass().getName();
    }
    
    /**
     * TODO delete this since it uses static access
     * Returns {@link LdapPrincipal} of current context.
     * @return the authenticated principal
     */
    public static LdapPrincipal getPrincipal()
    {
        return getContext().getSession().getEffectivePrincipal();
    }


    /**
     * TODO delete this since it uses static access
     * Returns the current JNDI {@link Context}.
     * @return the context on the invocation stack
     */
    public static OperationContext getContext()
    {
        return InvocationStack.getInstance().peek();
    }


    /**
     * Creates a new instance.
     */
    protected BaseInterceptor()
    {
    }


    /**
     * This method does nothing by default.
     * @throws Exception 
     */
    public void init( DirectoryService directoryService ) throws LdapException
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

    public void add( NextInterceptor next, AddOperationContext opContext ) throws LdapException
    {
        next.add( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws LdapException
    {
        next.delete( opContext );
    }


    public Entry getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws LdapException
    {
        return next.getRootDSE( opContext );
    }


    public DN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws LdapException
    {
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws LdapException
    {
        return next.hasEntry( opContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext opContext ) throws LdapException
    {
        return next.list( opContext );
    }


    public Set<String> listSuffixes ( NextInterceptor next, ListSuffixOperationContext opContext ) 
        throws LdapException
    {
        return next.listSuffixes( opContext );
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext opContext ) throws LdapException
    {
        return next.lookup( opContext );
    }

    
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws LdapException
    {
        next.modify( opContext );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws LdapException
    {
        next.moveAndRename( opContext );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext )
        throws LdapException
    {
        next.rename( opContext );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws LdapException
    {
        next.move( opContext );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws LdapException
    {
        return next.search( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws LdapException
    {
        return next.compare( opContext );
    }


    public void bind( NextInterceptor next, BindOperationContext opContext ) throws LdapException
    {
        next.bind( opContext );
    }


    public void unbind( NextInterceptor next, UnbindOperationContext opContext ) throws LdapException
    {
        next.unbind( opContext );
    }
}
