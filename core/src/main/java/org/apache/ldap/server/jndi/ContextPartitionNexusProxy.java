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
package org.apache.ldap.server.jndi;


import java.util.Iterator;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.event.NamingListener;
import javax.naming.event.EventContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.interceptor.InterceptorChain;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.partition.ContextPartitionNexus;
import org.apache.ldap.server.event.EventService;


/**
 * A decorator that wraps other {@link ContextPartitionNexus} to enable
 * {@link InterceptorChain} and {@link InvocationStack} support.
 * All {@link Invocation}s made to this nexus is automatically pushed to
 * {@link InvocationStack} of the current thread, and popped when
 * the operation ends.  All invocations are filtered by {@link InterceptorChain}.
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
class ContextPartitionNexusProxy extends ContextPartitionNexus
{
    private final Context caller;
    private final ContextFactoryService service;
    private final ContextFactoryConfiguration configuration;

    /**
     * Creates a new instance.
     * 
     * @param caller a JNDI {@link Context} object that will call this proxy
     * @param service a JNDI service
     */
    ContextPartitionNexusProxy( Context caller, ContextFactoryService service )
    {
        this.caller = caller;
        this.service = service;
        this.configuration = service.getConfiguration();
    }
    
    public LdapContext getLdapContext() {
        return this.configuration.getPartitionNexus().getLdapContext();
    }

    public void init( ContextFactoryConfiguration factoryCfg, ContextPartitionConfiguration cfg )
    {
    }

    public void destroy()
    {
    }

    public ContextPartition getSystemPartition()
    {
        return this.configuration.getPartitionNexus().getSystemPartition();
    }

    public Name getSuffix( boolean normalized ) throws NamingException
    {
        return this.configuration.getPartitionNexus().getSuffix( normalized );
    }

    public void sync() throws NamingException {
        this.service.sync();
    }

    public void close() throws NamingException {
        this.service.shutdown();
    }

    public boolean isInitialized() {
        return this.service.isStarted();
    }

    public Name getMatchedName(Name dn, boolean normalized) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "getMatchedDn",
                new Object[] { dn, normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.configuration.getInterceptorChain().getMatchedName( dn, normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public Name getSuffix(Name dn, boolean normalized) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "getSuffix",
                new Object[] { dn, normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.configuration.getInterceptorChain().getSuffix( dn, normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public Iterator listSuffixes(boolean normalized) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "listSuffixes",
                new Object[] { normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.configuration.getInterceptorChain().listSuffixes( normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public void delete(Name name) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "delete",
                new Object[] { name } ) );
        try
        {
            this.configuration.getInterceptorChain().delete( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public void add(String upName, Name normName, Attributes entry) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "add",
                new Object[] { upName, normName, entry } ) );
        try
        {
            this.configuration.getInterceptorChain().add( upName, normName, entry );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modify(Name name, int modOp, Attributes mods) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        // TODO Use predefined modOp Interger constants.
        stack.push( new Invocation(
                caller, "modify",
                new Object[] { name, new Integer( modOp ), mods } ) );
        try
        {
            this.configuration.getInterceptorChain().modify( name, modOp, mods );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modify(Name name, ModificationItem[] mods) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "modify",
                new Object[] { name, mods } ) );
        try
        {
            this.configuration.getInterceptorChain().modify( name, mods );
        }
        finally
        {
            stack.pop();
        }
    }

    public NamingEnumeration list(Name base) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "list",
                new Object[] { base } ) );
        try
        {
            return this.configuration.getInterceptorChain().list( base );
        }
        finally
        {
            stack.pop();
        }
    }

    public NamingEnumeration search(Name base, Map env, ExprNode filter, SearchControls searchCtls) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "search",
                new Object[] { base, env, filter, searchCtls } ) );
        try
        {
            return this.configuration.getInterceptorChain().search( base, env, filter, searchCtls );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes lookup(Name name) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "lookup",
                new Object[] { name } ) );
        try
        {
            return this.configuration.getInterceptorChain().lookup( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes lookup(Name dn, String[] attrIds) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "lookup",
                new Object[] { dn, attrIds } ) );
        try
        {
            return this.configuration.getInterceptorChain().lookup( dn, attrIds );
        }
        finally
        {
            stack.pop();
        }
    }

    public boolean hasEntry(Name name) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "hasEntry",
                new Object[] { name } ) );
        try
        {
            return this.configuration.getInterceptorChain().hasEntry( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public boolean isSuffix(Name name) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "isSuffix",
                new Object[] { name } ) );
        try
        {
            return this.configuration.getInterceptorChain().isSuffix( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modifyRn(Name name, String newRn, boolean deleteOldRn) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "modifyRn",
                new Object[] { name, newRn, deleteOldRn? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            this.configuration.getInterceptorChain().modifyRn( name, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }

    public void move(Name oriChildName, Name newParentName) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "move",
                new Object[] { oriChildName, newParentName } ) );
        try
        {
            this.configuration.getInterceptorChain().move( oriChildName, newParentName );
        }
        finally
        {
            stack.pop();
        }
    }

    public void move(Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn) throws NamingException {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "move",
                new Object[] { oriChildName, newParentName, newRn, deleteOldRn? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            this.configuration.getInterceptorChain().move( oriChildName, newParentName, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes getRootDSE() throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( caller, "getRootDSE" ) );
        try
        {
            return this.configuration.getInterceptorChain().getRootDSE();
        }
        finally
        {
            stack.pop();
        }
    }

    public void addContextPartition( ContextPartitionConfiguration config ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "addContextPartition",
                new Object[] { config } ) );
        try
        {
            this.configuration.getInterceptorChain().addContextPartition( config );
        }
        finally
        {
            stack.pop();
        }
    }

    public void removeContextPartition( Name suffix ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                caller, "removeContextPartition",
                new Object[] { suffix } ) );
        try
        {
            this.configuration.getInterceptorChain().removeContextPartition( suffix );
        }
        finally
        {
            stack.pop();
        }
    }

    private void ensureStarted() throws ServiceUnavailableException {
        if( !service.isStarted() )
        {
            throw new ServiceUnavailableException( "ContextFactoryService is not started." );
        }
    }


    // -----------------------------------------------------------------------
    // EventContext and EventDirContext notification methods
    // -----------------------------------------------------------------------

    /*
     * All listener registration/deregistration methods can be reduced down to
     * the following methods.  Rather then make these actual intercepted methods
     * we use them as out of band methods to interface with the notification
     * interceptor.
     */

    public void addNamingListener( EventContext ctx, Name name, ExprNode filter, SearchControls searchControls,
                                   NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.addNamingListener( ctx, name, filter, searchControls, namingListener );
    }


    public void removeNamingListener( NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.removeNamingListener( namingListener );
    }
}