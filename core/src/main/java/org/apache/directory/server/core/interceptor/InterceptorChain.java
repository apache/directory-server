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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.directory.server.core.DirectoryService;
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
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.ConfigurationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchResult;


/**
 * Manages the chain of {@link Interceptor}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private static final Logger LOG = LoggerFactory.getLogger( InterceptorChain.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private PartitionNexus nexus;


        public String getName()
        {
            return "FINAL";
        }

        public void init( DirectoryService directoryService )
        {
            this.nexus = directoryService.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        public boolean compare( NextInterceptor next, CompareOperationContext opContext ) throws NamingException
        {
            return nexus.compare( opContext );
        }


        public Attributes getRootDSE( NextInterceptor next, GetRootDSEOperationContext opContext ) throws NamingException
        {
            return nexus.getRootDSE( opContext );
        }


        public LdapDN getMatchedName( NextInterceptor next, GetMatchedNameOperationContext opContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getMatchedName( opContext ).clone();
        }


        public LdapDN getSuffix( NextInterceptor next, GetSuffixOperationContext opContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getSuffix( opContext ).clone();
        }


        public Iterator<String> listSuffixes( NextInterceptor next, ListSuffixOperationContext opContext ) throws NamingException
        {
            return nexus.listSuffixes( opContext );
        }


        public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws NamingException
        {
            nexus.delete( opContext );
        }


        public void add( NextInterceptor next, AddOperationContext opContext ) throws NamingException
        {
            nexus.add( opContext );
        }


        public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws NamingException
        {
            nexus.modify( opContext );
        }


        public NamingEnumeration<SearchResult> list( NextInterceptor next, ListOperationContext opContext ) throws NamingException
        {
            return nexus.list( opContext );
        }


        public NamingEnumeration<SearchResult> search( NextInterceptor next, SearchOperationContext opContext ) throws NamingException
        {
            return nexus.search( opContext );
        }


        public Attributes lookup( NextInterceptor next, LookupOperationContext opContext ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( opContext ).clone();
        }


        public boolean hasEntry( NextInterceptor next, EntryOperationContext opContext ) throws NamingException
        {
            return nexus.hasEntry( opContext );
        }


        public void rename( NextInterceptor next, RenameOperationContext opContext )
            throws NamingException
        {
            nexus.rename( opContext );
        }


        public void move( NextInterceptor next, MoveOperationContext opContext ) throws NamingException
        {
            nexus.move( opContext );
        }


        public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opContext )
            throws NamingException
        {
            nexus.moveAndRename( opContext );
        }


        public void addContextPartition( NextInterceptor next, AddContextPartitionOperationContext opContext )
            throws NamingException
        {
            nexus.addContextPartition( opContext );
        }


        public void removeContextPartition( NextInterceptor next, RemoveContextPartitionOperationContext opContext ) throws NamingException
        {
            nexus.removeContextPartition( opContext );
        }


        public void bind( NextInterceptor next, BindOperationContext opContext )  throws NamingException
        {
            nexus.bind( opContext );
        }


        public void unbind( NextInterceptor next, UnbindOperationContext opContext ) throws NamingException
        {
            nexus.unbind( opContext );
        }
    };

    private final Map<String, Entry> name2entry = new HashMap<String, Entry>();

    private final Entry tail;

    private Entry head;

    private DirectoryService directoryService;


    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
        tail = new Entry( "tail", null, null, FINAL_INTERCEPTOR );
        head = tail;
    }


    /**
     * Initializes and registers all interceptors according to the specified
     * {@link DirectoryService}.
     * @throws javax.naming.NamingException if an interceptor cannot be initialized.
     * @param directoryService the directory core
     */
    public synchronized void init( DirectoryService directoryService ) throws NamingException
    {
        // Initialize tail first.
        this.directoryService = directoryService;
        FINAL_INTERCEPTOR.init( directoryService );

        // And register and initialize all interceptors
        try
        {
            for ( Interceptor interceptor: directoryService.getInterceptors() )
            {
                if ( IS_DEBUG )
                {
                    LOG.debug( "Adding interceptor " + interceptor.getName() );
                }

                register( interceptor );
            }
        }
        catch ( Throwable t )
        {
            // destroy if failed to initialize all interceptors.
            destroy();

            if ( t instanceof NamingException )
            {
                throw ( NamingException ) t;
            }
            else
            {
                throw new InterceptorException( null, "Failed to initialize interceptor chain.", t );
            }
        }
    }


    /**
     * Deinitializes and deregisters all interceptors this chain contains.
     */
    public synchronized void destroy()
    {
        List<Entry> entries = new ArrayList<Entry>();
        Entry e = tail;

        do
        {
            entries.add( e );
            e = e.prevEntry;
        }
        while ( e != null );

        for ( Entry entry:entries )
        {
            if ( entry != tail )
            {
                try
                {
                    deregister( entry.getName() );
                }
                catch ( Throwable t )
                {
                    LOG.warn( "Failed to deregister an interceptor: " + entry.getName(), t );
                }
            }
        }
    }


    /**
     * Returns the registered interceptor with the specified name.
     * @param interceptorName name of the interceptor to look for
     * @return <tt>null</tt> if the specified name doesn't exist.
     */
    public Interceptor get( String interceptorName )
    {
        Entry e = name2entry.get( interceptorName );
        if ( e == null )
        {
            return null;
        }

        return e.interceptor;
    }


    /**
     * Returns the list of all registered interceptors.
     * @return a list of all the registered interceptors.
     */
    public synchronized List<Interceptor> getAll()
    {
        List<Interceptor> result = new ArrayList<Interceptor>();
        Entry e = head;

        do
        {
            result.add( e.interceptor );
            e = e.nextEntry;
        }
        while ( e != tail );

        return result;
    }


    public synchronized void addFirst( Interceptor interceptor ) throws NamingException
    {
        register0( interceptor, head );
    }


    public synchronized void addLast( Interceptor interceptor ) throws NamingException
    {
        register0( interceptor, tail );
    }


    public synchronized void addBefore( String nextInterceptorName, Interceptor interceptor )
        throws NamingException
    {
        Entry e = name2entry.get( nextInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + nextInterceptorName );
        }
        register0( interceptor, e );
    }


    public synchronized String remove( String interceptorName ) throws NamingException
    {
        return deregister( interceptorName );
    }


    public synchronized void addAfter( String prevInterceptorName, Interceptor interceptor )
        throws NamingException
    {
        Entry e = name2entry.get( prevInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + prevInterceptorName );
        }
        register0( interceptor, e.nextEntry );
    }


    /**
     * Adds and initializes an interceptor with the specified configuration.
     * @param interceptor interceptor to add to end of chain
     * @throws javax.naming.NamingException if there is already an interceptor of this name or the interceptor
     * cannot be initialized.
     */
    private void register( Interceptor interceptor ) throws NamingException
    {
        checkAddable( interceptor );
        register0( interceptor, tail );
    }


    /**
     * Removes and deinitializes the interceptor with the specified name.
     * @param name name of interceptor to remove
     * @return name of interceptor removed, if any
     * @throws javax.naming.ConfigurationException if no interceptor registered under that name
     */
    private String deregister( String name ) throws ConfigurationException
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;

        if ( nextEntry == null )
        {
            // Don't deregister tail
            return null;
        }

        if ( prevEntry == null )
        {
            nextEntry.prevEntry = null;
            head = nextEntry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;
            nextEntry.prevEntry = prevEntry;
        }

        name2entry.remove( name );
        entry.interceptor.destroy();

        return entry.getName();
    }

    private void register0( Interceptor interceptor, Entry nextEntry ) throws NamingException
    {
        String name = interceptor.getName();

        interceptor.init( directoryService );
        Entry newEntry;
        if ( nextEntry == head )
        {
            newEntry = new Entry( interceptor.getName(), null, head, interceptor );
            head.prevEntry = newEntry;
            head = newEntry;
        }
        else if ( head == tail )
        {
            newEntry = new Entry( interceptor.getName(), null, tail, interceptor );
            tail.prevEntry = newEntry;
            head = newEntry;
        }
        else
        {
            newEntry = new Entry( interceptor.getName(), nextEntry.prevEntry, nextEntry, interceptor );
            nextEntry.prevEntry.nextEntry = newEntry;
            nextEntry.prevEntry = newEntry;
        }

        name2entry.put( name, newEntry );
    }


    /**
     * Throws an exception when the specified interceptor name is not registered in this chain.
     *
     * @param name name of interceptor to look for
     * @return An interceptor entry with the specified name.
     * @throws javax.naming.ConfigurationException if no interceptor has that name
     */
    private Entry checkOldName( String name ) throws ConfigurationException
    {
        Entry e = name2entry.get( name );

        if ( e == null )
        {
            throw new ConfigurationException( "Unknown interceptor name:" + name );
        }

        return e;
    }


    /**
     * Checks the specified interceptor name is already taken and throws an exception if already taken.
     * @param interceptor interceptor to check
     * @throws javax.naming.ConfigurationException if interceptor name is already registered
     */
    private void checkAddable( Interceptor interceptor ) throws ConfigurationException
    {
        if ( name2entry.containsKey( interceptor.getName() ) )
        {
            throw new ConfigurationException( "Other interceptor is using name '" + interceptor.getName() + "'" );
        }
    }


    /**
     * Gets the InterceptorEntry to use first with bypass information considered.
     *
     * @return the first entry to use.
     */
    private Entry getStartingEntry()
    {
        if ( InvocationStack.getInstance().isEmpty() )
        {
            return head;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        if ( !invocation.hasBypass() )
        {
            return head;
        }

        if ( invocation.isBypassed( PartitionNexusProxy.BYPASS_ALL ) )
        {
            return tail;
        }

        Entry next = head;
        while ( next != tail )
        {
            if ( invocation.isBypassed( next.getName() ) )
            {
                next = next.nextEntry;
            }
            else
            {
                return next;
            }
        }

        return tail;
    }


    public Attributes getRootDSE( GetRootDSEOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.getRootDSE( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.getMatchedName( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.getSuffix( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public boolean compare( CompareOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.compare( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public Iterator<String> listSuffixes( ListSuffixOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.listSuffixes( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void addContextPartition( AddContextPartitionOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.addContextPartition( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void removeContextPartition( RemoveContextPartitionOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.removeContextPartition( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void delete( DeleteOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.delete( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void add( AddOperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.interceptor;
        NextInterceptor next = node.nextInterceptor;

        try
        {
            head.add( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void bind( BindOperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.interceptor;
        NextInterceptor next = node.nextInterceptor;

        try
        {
            head.bind( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void unbind( UnbindOperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.interceptor;
        NextInterceptor next = node.nextInterceptor;

        try
        {
            head.unbind( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void modify( ModifyOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.modify( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public NamingEnumeration<SearchResult> list( ListOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.list( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
        throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.search( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public Attributes lookup( LookupOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.lookup( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public boolean hasEntry( EntryOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.hasEntry( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void rename( RenameOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.rename( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void move( MoveOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.move( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void moveAndRename( MoveAndRenameOperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.interceptor;
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            head.moveAndRename( next, opContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }

    /**
     * Represents an internal entry of this chain.
     */
    private class Entry
    {
        private volatile Entry prevEntry;

        private volatile Entry nextEntry;

        private final String name;

        private final Interceptor interceptor;

        private final NextInterceptor nextInterceptor;


        private String getName()
        {
            return name;
        }


        private Entry( String name, Entry prevEntry, Entry nextEntry, Interceptor interceptor )
        {
            this.name = name;

            if ( interceptor == null )
            {
                throw new NullPointerException( "interceptor" );
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.interceptor = interceptor;
            this.nextInterceptor = new NextInterceptor()
            {
                private Entry getNextEntry()
                {
                    if ( InvocationStack.getInstance().isEmpty() )
                    {
                        return Entry.this.nextEntry;
                    }

                    Invocation invocation = InvocationStack.getInstance().peek();
                    if ( !invocation.hasBypass() )
                    {
                        return Entry.this.nextEntry;
                    }

                    //  I don't think we really need this since this check is performed by the chain when
                    //  getting the interceptor head to use.
                    //
                    //                    if ( invocation.isBypassed( DirectoryPartitionNexusProxy.BYPASS_ALL ) )
                    //                    {
                    //                        return tail;
                    //                    }

                    Entry next = Entry.this.nextEntry;
                    while ( next != tail )
                    {
                        if ( invocation.isBypassed( next.getName() ) )
                        {
                            next = next.nextEntry;
                        }
                        else
                        {
                            return next;
                        }
                    }

                    return next;
                }


                public boolean compare( CompareOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.compare( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Attributes getRootDSE( GetRootDSEOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.getRootDSE( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public LdapDN getMatchedName( GetMatchedNameOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.getMatchedName( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public LdapDN getSuffix( GetSuffixOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.getSuffix( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Iterator<String> listSuffixes( ListSuffixOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.listSuffixes( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void delete( DeleteOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.delete( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void add( AddOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.add( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void modify( ModifyOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.modify( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }

                
                public NamingEnumeration<SearchResult> list( ListOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.list( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public NamingEnumeration<SearchResult> search( SearchOperationContext opContext )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.search( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Attributes lookup( LookupOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.lookup( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public boolean hasEntry( EntryOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        return interceptor.hasEntry( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void rename( RenameOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.rename( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void move( MoveOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.move( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void moveAndRename( MoveAndRenameOperationContext opContext )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.moveAndRename( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void bind( BindOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;
    
                    try
                    {
                        interceptor.bind( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void unbind( UnbindOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.unbind( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void addContextPartition( AddContextPartitionOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.addContextPartition( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void removeContextPartition( RemoveContextPartitionOperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.interceptor;

                    try
                    {
                        interceptor.removeContextPartition( next.nextInterceptor, opContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }
            };
        }
    }


    private static void throwInterceptorException( Interceptor interceptor, Throwable e ) throws InterceptorException
    {
        throw new InterceptorException( interceptor, "Unexpected exception.", e );
    }
}
