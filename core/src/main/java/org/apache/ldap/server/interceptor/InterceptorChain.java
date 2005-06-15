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


import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.naming.NamingException;

import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.invocation.Invocation;


/**
 * Manages the chain of {@link Interceptor}s.  <tt>InterceptorChain</tt>
 * is also an {@link Interceptor}, and thus you can create hiararchical
 * interceptor structure to break down complex interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private InterceptorContext ctx;


        public void init( InterceptorContext context )
        {
            ctx = context;
        }


        public void destroy()
        {
            // unused
        }


        public void process( NextInterceptor nextInterceptor, Invocation call ) throws NamingException
        {
            if ( parent == null )
            {
                // execute the actual backend operation only when this chain is root.

                call.execute( ctx.getRootNexus() );
            }
        }
    };

    private InterceptorChain parent;

    private final Map name2entry = new HashMap();

    private final Map interceptor2entry = new IdentityHashMap();

    private final Entry tail = new Entry( null, null, "end", FINAL_INTERCEPTOR );

    private Entry head = tail;


    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
        this( new ArrayList() );
    }
    
    /**
     * Creates a new interceptor chain 
     * @param configurations
     */
    public InterceptorChain( List configurations )
    {
        Iterator it = configurations.iterator();
        while( it.hasNext() )
        {
            InterceptorConfiguration cfg = ( InterceptorConfiguration ) it.next();
            this.addLast( cfg.getName(), cfg.getInterceptor() );
        }
    }


    /**
     * Initializes all interceptors this chain contains.
     */
    public synchronized void init( InterceptorContext ctx ) throws NamingException
    {
        ListIterator it = getAll().listIterator();

        Interceptor interceptor = null;

        try
        {
            while ( it.hasNext() )
            {
                interceptor = ( Interceptor ) it.next();
                interceptor.init( ctx );
            }
        }
        catch ( Throwable t )
        {
            while ( it.hasPrevious() )
            {
                Interceptor i = ( Interceptor ) it.previous();

                try
                {
                    i.destroy();
                }
                catch ( Throwable t2 )
                {
                    t2.printStackTrace();
                }
            }

            if ( t instanceof NamingException )
            {
                throw ( NamingException ) t;
            }
            else
            {
                throw new InterceptorException( interceptor, null, "Failed to initialize interceptor chain.", t );
            }
        }
    }


    /**
     * Deinitializes all interceptors this chain contains.
     */
    public synchronized void destroy()
    {
        ListIterator it = getAllReversed().listIterator();

        while ( it.hasNext() )
        {
            Interceptor interceptor = ( Interceptor ) it.next();

            try
            {
                interceptor.destroy();
            }
            catch ( Throwable t )
            {
                t.printStackTrace();
            }
        }
    }


    /**
     * Returns the interceptor with the specified <code>name</code>.
     *
     * @return <code>null</code> if there is no interceptor with the specified <code>name</code>.
     */
    public Interceptor get( String name )
    {
        Entry e = ( Entry ) name2entry.get( name );

        if ( e == null )
        {
            return null;
        }

        return e.interceptor;
    }


    /**
     * Adds the specified interceptor with the specified name at the beginning of this chain.
     */
    public synchronized void addFirst( String name,
                                       Interceptor interceptor )
    {
        checkAddable( name, interceptor );
        register( name, interceptor, head );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      Interceptor interceptor )
    {
        checkAddable( name, interceptor );
        register( name, interceptor, tail );
    }


    /**
     * Adds the specified interceptor with the specified name just before the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName, String name, Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, interceptor );
        register( name, interceptor, baseEntry );
    }


    /**
     * Adds the specified interceptor with the specified name just after the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName, String name, Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, interceptor );
        register( name, interceptor, baseEntry );
    }


    /**
     * Removes the interceptor with the specified name from this chain.
     */
    public synchronized void remove( String name )
    {
        Entry entry = checkOldName( name );

        Entry prevEntry = entry.prevEntry;

        Entry nextEntry = entry.nextEntry;

        if ( prevEntry == null )
        {
            nextEntry.prevEntry = null;

            head = entry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;

            nextEntry.prevEntry = prevEntry;
        }

        name2entry.remove( name );

        Interceptor interceptor = entry.interceptor;

        interceptor2entry.remove( interceptor );

        if ( interceptor instanceof InterceptorChain )
        {
            ( ( InterceptorChain ) interceptor ).parent = null;
        }
    }


    /**
     * Removes all interceptors added to this chain.
     */
    public synchronized void clear()
    {
        Iterator it = new ArrayList( name2entry.keySet() ).iterator();

        while ( it.hasNext() )
        {
            this.remove( ( String ) it.next() );
        }
    }


    private void register( String name, Interceptor interceptor, Entry nextEntry )
    {
        Entry newEntry;
        if( nextEntry == head )
        {
            newEntry = new Entry( null, head, name, interceptor );
            head.prevEntry = newEntry;
            head = newEntry;
        }
        else if( head == tail )
        {
            newEntry = new Entry( null, tail, name, interceptor );
            tail.prevEntry = newEntry;
            head = newEntry;
        }
        else
        {
            newEntry = new Entry( nextEntry.prevEntry, nextEntry, name, interceptor );
            nextEntry.prevEntry.nextEntry = newEntry;
            nextEntry.prevEntry = newEntry;
        }
        
        name2entry.put( name, newEntry );
        interceptor2entry.put( newEntry.interceptor, newEntry );
    }


    /**
     * Throws an exception when the specified interceptor name is not registered in this chain.
     *
     * @return An interceptor entry with the specified name.
     */
    private Entry checkOldName( String baseName )
    {
        Entry e = ( Entry ) name2entry.get( baseName );

        if ( e == null )
        {
            throw new IllegalArgumentException( "Unknown interceptor name:" + baseName );
        }

        return e;
    }


    /**
     * Checks the specified interceptor name is already taken and throws an exception if already taken.
     */
    private void checkAddable( String name, Interceptor interceptor )
    {
        if ( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException( "Other interceptor is using name '" + name + "'" );
        }

        if ( interceptor instanceof InterceptorChain )
        {
            if ( ( ( InterceptorChain ) interceptor ).parent != null )
            {
                throw new IllegalArgumentException( "This interceptor chain has its parent already." );
            }
        }
    }


    /**
     * Start invocation chain with the specified invocation.
     *
     * @throws NamingException if invocation failed
     */
    public void process( Invocation invocation ) throws NamingException
    {
        Entry head = this.head;

        try
        {
            head.interceptor.process( head.nextInterceptor, invocation );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throw new InterceptorException( head.interceptor, invocation, "Unexpected exception.", e );
        }
    }


    /**
     * Returns the list of interceptors this chain in the order of evaluation.
     */
    public List getAll()
    {
        List list = new ArrayList();

        Entry e = head;

        do
        {
            list.add( e.interceptor );

            e = e.nextEntry;
        }
        while ( e != null );

        return list;
    }


    /**
     * Returns the list of interceptors this chain in the reversed order of evaluation.
     */
    public List getAllReversed()
    {
        List list = new ArrayList();

        Entry e = tail;

        do
        {
            list.add( e.interceptor );

            e = e.prevEntry;
        }

        while ( e != null );

        return list;
    }


    /**
     * Represents an internal entry of this chain.
     */
    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final Interceptor interceptor;

        private final NextInterceptor nextInterceptor;


        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, Interceptor interceptor )
        {
            if ( interceptor == null )
            {
                throw new NullPointerException( "interceptor" );
            }
            if ( name == null )
            {
                throw new NullPointerException( "name" );
            }

            this.prevEntry = prevEntry;

            this.nextEntry = nextEntry;

            this.interceptor = interceptor;

            this.nextInterceptor = new NextInterceptor()
            {
                public void process( Invocation call ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.interceptor;

                    try
                    {
                        interceptor.process( Entry.this.nextEntry.nextInterceptor, call );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throw new InterceptorException( interceptor, call, "Unexpected exception.", e );
                    }
                }
            };
        }
    }
}
