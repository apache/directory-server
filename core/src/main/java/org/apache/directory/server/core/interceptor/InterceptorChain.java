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
package org.apache.directory.server.core.interceptor;


import java.util.*;

import javax.naming.ConfigurationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.DirectoryPartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.DirectoryPartitionNexus;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages the chain of {@link Interceptor}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private static final Logger log = LoggerFactory.getLogger( InterceptorChain.class );

    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private DirectoryPartitionNexus nexus;


        public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg )
        {
            this.nexus = factoryCfg.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        public boolean compare( NextInterceptor next, LdapDN name, String oid, Object value ) throws NamingException
        {
            return nexus.compare( name, oid, value );
        }


        public Attributes getRootDSE( NextInterceptor next ) throws NamingException
        {
            return nexus.getRootDSE();
        }


        public LdapDN getMatchedName ( NextInterceptor next, LdapDN dn ) throws NamingException
        {
            return ( LdapDN ) nexus.getMatchedName( dn ).clone();
        }


        public LdapDN getSuffix ( NextInterceptor next, LdapDN dn ) throws NamingException
        {
            return ( LdapDN ) nexus.getSuffix( dn ).clone();
        }


        public Iterator listSuffixes ( NextInterceptor next ) throws NamingException
        {
            return nexus.listSuffixes();
        }


        public void delete( NextInterceptor next, LdapDN name ) throws NamingException
        {
            nexus.delete( name );
        }


        public void add(NextInterceptor next, LdapDN normName, Attributes entry) throws NamingException
        {
            nexus.add( normName, entry );
        }


        public void modify( NextInterceptor next, LdapDN name, int modOp, Attributes mods ) throws NamingException
        {
            nexus.modify( name, modOp, mods );
        }


        public void modify( NextInterceptor next, LdapDN name, ModificationItem[] mods ) throws NamingException
        {
            nexus.modify( name, mods );
        }


        public NamingEnumeration list( NextInterceptor next, LdapDN base ) throws NamingException
        {
            return nexus.list( base );
        }


        public NamingEnumeration search( NextInterceptor next, LdapDN base, Map env, ExprNode filter,
            SearchControls searchCtls ) throws NamingException
        {
            return nexus.search( base, env, filter, searchCtls );
        }


        public Attributes lookup( NextInterceptor next, LdapDN name ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( name ).clone();
        }


        public Attributes lookup( NextInterceptor next, LdapDN dn, String[] attrIds ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( dn, attrIds ).clone();
        }


        public boolean hasEntry( NextInterceptor next, LdapDN name ) throws NamingException
        {
            return nexus.hasEntry( name );
        }


        public boolean isSuffix( NextInterceptor next, LdapDN name ) throws NamingException
        {
            return nexus.isSuffix( name );
        }


        public void modifyRn( NextInterceptor next, LdapDN name, String newRn, boolean deleteOldRn )
            throws NamingException
        {
            nexus.modifyRn( name, newRn, deleteOldRn );
        }


        public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName ) throws NamingException
        {
            nexus.move( oriChildName, newParentName );
        }


        public void move( NextInterceptor next, LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn )
            throws NamingException
        {
            nexus.move( oriChildName, newParentName, newRn, deleteOldRn );
        }


        public void addContextPartition( NextInterceptor next, DirectoryPartitionConfiguration cfg )
            throws NamingException
        {
            nexus.addContextPartition( cfg );
        }


        public void removeContextPartition( NextInterceptor next, LdapDN suffix ) throws NamingException
        {
            nexus.removeContextPartition( suffix );
        }


        public void bind( NextInterceptor next, LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId )
            throws NamingException
        {
            nexus.bind( bindDn, credentials, mechanisms, saslAuthId );
        }


        public void unbind( NextInterceptor next, LdapDN bindDn ) throws NamingException
        {
            nexus.unbind( bindDn );
        }
    };

    private final Map name2entry = new HashMap();

    private final Entry tail;

    private Entry head;

    private DirectoryServiceConfiguration factoryCfg;


    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
        MutableInterceptorConfiguration tailCfg = new MutableInterceptorConfiguration();
        tailCfg.setName( "tail" );
        tailCfg.setInterceptor( FINAL_INTERCEPTOR );
        tail = new Entry( null, null, tailCfg );
        head = tail;
    }


    /**
     * Initializes and registers all interceptors according to the specified
     * {@link DirectoryServiceConfiguration}.
     */
    public synchronized void init( DirectoryServiceConfiguration factoryCfg ) throws NamingException
    {
        this.factoryCfg = factoryCfg;

        // Initialize tail first.
        FINAL_INTERCEPTOR.init( factoryCfg, null );

        // And register and initialize all interceptors
        ListIterator i = factoryCfg.getStartupConfiguration().getInterceptorConfigurations().listIterator();
        Interceptor interceptor = null;
        try
        {
            while ( i.hasNext() )
            {
                InterceptorConfiguration cfg = ( InterceptorConfiguration ) i.next();

                if ( log.isDebugEnabled() )
                {
                    log.debug( "Adding interceptor " + cfg.getName() );
                }

                register( cfg );
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
                throw new InterceptorException( interceptor, "Failed to initialize interceptor chain.", t );
            }
        }
    }


    /**
     * Deinitializes and deregisters all interceptors this chain contains.
     */
    public synchronized void destroy()
    {
        List entries = new ArrayList();
        Entry e = tail;
        do
        {
            entries.add( e );
            e = e.prevEntry;
        }
        while ( e != null );

        Iterator i = entries.iterator();
        while ( i.hasNext() )
        {
            e = ( Entry ) i.next();
            if ( e != tail )
            {
                try
                {
                    deregister( e.configuration.getName() );
                }
                catch ( Throwable t )
                {
                    log.warn( "Failed to deregister an interceptor: " + e.configuration.getName(), t );
                }
            }
        }
    }


    /**
     * Returns the registered interceptor with the specified name.
     * @return <tt>null</tt> if the specified name doesn't exist.
     */
    public Interceptor get( String interceptorName )
    {
        Entry e = ( Entry ) name2entry.get( interceptorName );
        if ( e == null )
        {
            return null;
        }

        return e.configuration.getInterceptor();
    }


    /**
     * Returns the list of all registered interceptors.
     */
    public synchronized List getAll()
    {
        List result = new ArrayList();
        Entry e = head;
        do
        {
            result.add( e.configuration.getInterceptor() );
            e = e.nextEntry;
        }
        while ( e != tail );

        return result;
    }


    public synchronized void addFirst( InterceptorConfiguration cfg ) throws NamingException
    {
        register0( cfg, head );
    }


    public synchronized void addLast( InterceptorConfiguration cfg ) throws NamingException
    {
        register0( cfg, tail );
    }


    public synchronized void addBefore( String nextInterceptorName, InterceptorConfiguration cfg )
        throws NamingException
    {
        Entry e = ( Entry ) name2entry.get( nextInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + nextInterceptorName );
        }
        register0( cfg, e );
    }


    public synchronized InterceptorConfiguration remove( String interceptorName ) throws NamingException
    {
        return deregister( interceptorName );
    }


    public synchronized void addAfter( String prevInterceptorName, InterceptorConfiguration cfg )
        throws NamingException
    {
        Entry e = ( Entry ) name2entry.get( prevInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + prevInterceptorName );
        }
        register0( cfg, e.nextEntry );
    }


    /**
     * Adds and initializes an interceptor with the specified configuration.
     */
    private void register( InterceptorConfiguration cfg ) throws NamingException
    {
        checkAddable( cfg );
        register0( cfg, tail );
    }


    /**
     * Removes and deinitializes the interceptor with the specified name.
     */
    private InterceptorConfiguration deregister( String name ) throws ConfigurationException
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
        entry.configuration.getInterceptor().destroy();

        return entry.configuration;
    }


    private void register0( InterceptorConfiguration cfg, Entry nextEntry ) throws NamingException
    {
        String name = cfg.getName();
        Interceptor interceptor = cfg.getInterceptor();
        interceptor.init( factoryCfg, cfg );

        Entry newEntry;
        if ( nextEntry == head )
        {
            newEntry = new Entry( null, head, cfg );
            head.prevEntry = newEntry;
            head = newEntry;
        }
        else if ( head == tail )
        {
            newEntry = new Entry( null, tail, cfg );
            tail.prevEntry = newEntry;
            head = newEntry;
        }
        else
        {
            newEntry = new Entry( nextEntry.prevEntry, nextEntry, cfg );
            nextEntry.prevEntry.nextEntry = newEntry;
            nextEntry.prevEntry = newEntry;
        }

        name2entry.put( name, newEntry );
    }


    /**
     * Throws an exception when the specified interceptor name is not registered in this chain.
     *
     * @return An interceptor entry with the specified name.
     */
    private Entry checkOldName( String baseName ) throws ConfigurationException
    {
        Entry e = ( Entry ) name2entry.get( baseName );

        if ( e == null )
        {
            throw new ConfigurationException( "Unknown interceptor name:" + baseName );
        }

        return e;
    }


    /**
     * Checks the specified interceptor name is already taken and throws an exception if already taken.
     */
    private void checkAddable( InterceptorConfiguration cfg ) throws ConfigurationException
    {
        if ( name2entry.containsKey( cfg.getName() ) )
        {
            throw new ConfigurationException( "Other interceptor is using name '" + cfg.getName() + "'" );
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

        if ( invocation.isBypassed( DirectoryPartitionNexusProxy.BYPASS_ALL ) )
        {
            return tail;
        }

        Entry next = head;
        while ( next != tail )
        {
            if ( invocation.isBypassed( next.configuration.getName() ) )
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


    public Attributes getRootDSE() throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.getRootDSE( next );
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


    public LdapDN getMatchedName( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.getMatchedName( next, name );
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


    public LdapDN getSuffix ( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.getSuffix( next, name );
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


    public boolean compare( LdapDN name, String oid, Object value ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.compare( next, name, oid, value );
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


    public Iterator listSuffixes() throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.listSuffixes( next );
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


    public void addContextPartition( DirectoryPartitionConfiguration cfg ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.addContextPartition( next, cfg );
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


    public void removeContextPartition( LdapDN suffix ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.removeContextPartition( next, suffix );
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


    public void delete( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.delete( next, name );
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


    public void add( LdapDN normName, Attributes entry ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        try
        {
            head.add( next, normName, entry );
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


    public void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        try
        {
            head.bind( next, bindDn, credentials, mechanisms, saslAuthId );
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


    public void unbind( LdapDN bindDn ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        try
        {
            head.unbind( next, bindDn );
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


    public void modify( LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.modify( next, name, modOp, mods );
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


    public void modify( LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.modify( next, name, mods );
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


    public NamingEnumeration list( LdapDN base ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.list( next, base );
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


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.search( next, base, env, filter, searchCtls );
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


    public Attributes lookup( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.lookup( next, name );
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


    public Attributes lookup( LdapDN dn, String[] attrIds ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.lookup( next, dn, attrIds );
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


    public boolean hasEntry( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.hasEntry( next, name );
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


    public boolean isSuffix( LdapDN name ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.isSuffix( next, name );
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


    public void modifyRn( LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.modifyRn( next, name, newRn, deleteOldRn );
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


    public void move( LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.move( next, oriChildName, newParentName );
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


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.move( next, oriChildName, newParentName, newRn, deleteOldRn );
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
        private Entry prevEntry;

        private Entry nextEntry;

        private final InterceptorConfiguration configuration;

        private final NextInterceptor nextInterceptor;


        private Entry(Entry prevEntry, Entry nextEntry, InterceptorConfiguration configuration)
        {
            if ( configuration == null )
            {
                throw new NullPointerException( "configuration" );
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.configuration = configuration;
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
                        if ( invocation.isBypassed( next.configuration.getName() ) )
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


                public boolean compare( LdapDN name, String oid, Object value ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.compare( next.nextInterceptor, name, oid, value );
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


                public Attributes getRootDSE() throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getRootDSE( next.nextInterceptor );
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


                public LdapDN getMatchedName ( LdapDN dn ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getMatchedName( next.nextInterceptor, dn );
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


                public LdapDN getSuffix ( LdapDN dn ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getSuffix( next.nextInterceptor, dn );
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


                public Iterator listSuffixes () throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.listSuffixes( next.nextInterceptor );
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


                public void delete( LdapDN name ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.delete( next.nextInterceptor, name );
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


                public void add( LdapDN normName, Attributes entry ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.add( next.nextInterceptor, normName, entry );
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


                public void modify( LdapDN name, int modOp, Attributes mods ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( next.nextInterceptor, name, modOp, mods );
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


                public void modify( LdapDN name, ModificationItem[] mods ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( next.nextInterceptor, name, mods );
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


                public NamingEnumeration list( LdapDN base ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.list( next.nextInterceptor, base );
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


                public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.search( next.nextInterceptor, base, env, filter, searchCtls );
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


                public Attributes lookup( LdapDN name ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( next.nextInterceptor, name );
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


                public Attributes lookup( LdapDN dn, String[] attrIds ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( next.nextInterceptor, dn, attrIds );
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


                public boolean hasEntry( LdapDN name ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.hasEntry( next.nextInterceptor, name );
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


                public boolean isSuffix( LdapDN name ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.isSuffix( next.nextInterceptor, name );
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


                public void modifyRn( LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modifyRn( next.nextInterceptor, name, newRn, deleteOldRn );
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


                public void move( LdapDN oriChildName, LdapDN newParentName ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( next.nextInterceptor, oriChildName, newParentName );
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


                public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( next.nextInterceptor, oriChildName, newParentName, newRn, deleteOldRn );
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


                public void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.bind( next.nextInterceptor, bindDn, credentials, mechanisms, saslAuthId );
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


                public void unbind( LdapDN bindDn ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.unbind( next.nextInterceptor, bindDn );
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


                public void addContextPartition( DirectoryPartitionConfiguration cfg ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.addContextPartition( next.nextInterceptor, cfg );
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


                public void removeContextPartition( LdapDN suffix ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.removeContextPartition( next.nextInterceptor, suffix );
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
