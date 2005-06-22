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
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.invocation.InvocationStack;
import org.apache.ldap.server.partition.ContextPartition;
import org.apache.ldap.server.partition.ContextPartitionNexus;

class ContextPartitionNexusProxy extends ContextPartitionNexus
{
    private final Context target;
    private final ContextFactoryConfiguration provider;

    ContextPartitionNexusProxy( Context target, ContextFactoryConfiguration provider )
    {
        this.target = target;
        this.provider = provider;
    }
    
    public LdapContext getLdapContext() {
        return this.provider.getPartitionNexus().getLdapContext();
    }

    public void init( ContextFactoryConfiguration factoryCfg, ContextPartitionConfiguration cfg ) throws NamingException
    {
        throw new IllegalStateException();
    }

    public void destroy() throws NamingException
    {
        throw new IllegalStateException();
    }

    public ContextPartition getSystemPartition()
    {
        return this.provider.getPartitionNexus().getSystemPartition();
    }

    public Name getSuffix( boolean normalized )
    {
        return this.provider.getPartitionNexus().getSuffix( normalized );
    }

    public void sync() throws NamingException {
        this.provider.sync();
    }

    public void close() throws NamingException {
        this.provider.shutdown();
    }

    public boolean isInitialized() {
        return this.provider.isStarted();
    }

    public Name getMatchedDn(Name dn, boolean normalized) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "getMatchedDn",
                new Object[] { dn, normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.provider.getInterceptorChain().getMatchedDn( dn, normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public Name getSuffix(Name dn, boolean normalized) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "getSuffix",
                new Object[] { dn, normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.provider.getInterceptorChain().getSuffix( dn, normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public Iterator listSuffixes(boolean normalized) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "listSuffixes",
                new Object[] { normalized? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            return this.provider.getInterceptorChain().listSuffixes( normalized );
        }
        finally
        {
            stack.pop();
        }
    }

    public void delete(Name name) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "delete",
                new Object[] { name } ) );
        try
        {
            this.provider.getInterceptorChain().delete( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public void add(String upName, Name normName, Attributes entry) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "add",
                new Object[] { upName, normName, entry } ) );
        try
        {
            this.provider.getInterceptorChain().add( upName, normName, entry );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modify(Name name, int modOp, Attributes mods) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        // TODO Use predefined modOp Interger constants.
        stack.push( new Invocation(
                target, "modify",
                new Object[] { name, new Integer( modOp ), mods } ) );
        try
        {
            this.provider.getInterceptorChain().modify( name, modOp, mods );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modify(Name name, ModificationItem[] mods) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "modify",
                new Object[] { name, mods } ) );
        try
        {
            this.provider.getInterceptorChain().modify( name, mods );
        }
        finally
        {
            stack.pop();
        }
    }

    public NamingEnumeration list(Name base) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "list",
                new Object[] { base } ) );
        try
        {
            return this.provider.getInterceptorChain().list( base );
        }
        finally
        {
            stack.pop();
        }
    }

    public NamingEnumeration search(Name base, Map env, ExprNode filter, SearchControls searchCtls) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "search",
                new Object[] { base, env, filter, searchCtls } ) );
        try
        {
            return this.provider.getInterceptorChain().search( base, env, filter, searchCtls );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes lookup(Name name) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "lookup",
                new Object[] { name } ) );
        try
        {
            return this.provider.getInterceptorChain().lookup( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes lookup(Name dn, String[] attrIds) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "lookup",
                new Object[] { dn, attrIds } ) );
        try
        {
            return this.provider.getInterceptorChain().lookup( dn, attrIds );
        }
        finally
        {
            stack.pop();
        }
    }

    public boolean hasEntry(Name name) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "hasEntry",
                new Object[] { name } ) );
        try
        {
            return this.provider.getInterceptorChain().hasEntry( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public boolean isSuffix(Name name) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "isSuffix",
                new Object[] { name } ) );
        try
        {
            return this.provider.getInterceptorChain().isSuffix( name );
        }
        finally
        {
            stack.pop();
        }
    }

    public void modifyRn(Name name, String newRn, boolean deleteOldRn) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "modifyRn",
                new Object[] { name, newRn, deleteOldRn? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            this.provider.getInterceptorChain().modifyRn( name, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }

    public void move(Name oriChildName, Name newParentName) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "move",
                new Object[] { oriChildName, newParentName } ) );
        try
        {
            this.provider.getInterceptorChain().move( oriChildName, newParentName );
        }
        finally
        {
            stack.pop();
        }
    }

    public void move(Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn) throws NamingException {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation(
                target, "move",
                new Object[] { oriChildName, newParentName, newRn, deleteOldRn? Boolean.TRUE : Boolean.FALSE } ) );
        try
        {
            this.provider.getInterceptorChain().move( oriChildName, newParentName, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }

    public Attributes getRootDSE() throws NamingException
    {
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( target, "getRootDSE" ) );
        try
        {
            return this.provider.getInterceptorChain().getRootDSE();
        }
        finally
        {
            stack.pop();
        }
    }
}