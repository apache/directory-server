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


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.filtering.BaseEntryFilteringCursor;
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
import org.apache.directory.server.core.interceptor.context.RemoveContextPartitionOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.jndi.ServerContext;
import org.apache.directory.shared.ldap.name.LdapDN;

import javax.naming.Context;
import javax.naming.ldap.LdapContext;
import java.util.Iterator;


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
     * Returns {@link LdapPrincipal} of current context.
     * @return the authenticated principal
     */
    public static LdapPrincipal getPrincipal()
    {
        ServerContext ctx = ( ServerContext ) getContext();
        return ctx.getPrincipal();
    }


    /**
     * Returns the current JNDI {@link Context}.
     * @return the context on the invocation stack
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
     * @throws Exception 
     */
    public void init( DirectoryService directoryService ) throws Exception
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

    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        next.add( opContext );
    }


    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        next.delete( opContext );
    }


    public LdapDN getMatchedName ( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws Exception
    {
        return next.getMatchedName( opContext );
    }


    public ClonedServerEntry getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws Exception
    {
        return next.getRootDSE( opContext );
    }


    public LdapDN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws Exception
    {
        return next.getSuffix( opContext );
    }


    public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws Exception
    {
        return next.hasEntry( opContext );
    }


    public BaseEntryFilteringCursor list( NextInterceptor next, ListOperationContext opContext ) throws Exception
    {
        return next.list( opContext );
    }


    public Iterator<String> listSuffixes ( NextInterceptor next, ListSuffixOperationContext opContext ) 
        throws Exception
    {
        return next.listSuffixes( opContext );
    }


    public ClonedServerEntry lookup( NextInterceptor next, LookupOperationContext opContext ) throws Exception
    {
        return next.lookup( opContext );
    }

    
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        next.modify( opContext );
    }


    public void rename( NextInterceptor next, RenameOperationContext opContext ) throws Exception
    {
        next.rename( opContext );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
        throws Exception
    {
        next.moveAndRename( opContext );
    }


    public void move( NextInterceptor next, MoveOperationContext opContext ) throws Exception
    {
        next.move( opContext );
    }


    public BaseEntryFilteringCursor search( NextInterceptor next, SearchOperationContext opContext ) throws Exception
    {
        return next.search( opContext );
    }


    public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext ) throws Exception
    {
        next.addContextPartition( opContext );
    }


    public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws Exception
    {
        next.removeContextPartition( opContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws Exception
    {
        return next.compare( opContext );
    }


    public void bind( NextInterceptor next, BindOperationContext opContext ) throws Exception
    {
        next.bind( opContext );
    }


    public void unbind( NextInterceptor next, UnbindOperationContext opContext ) throws Exception
    {
        next.unbind( opContext );
    }
}
