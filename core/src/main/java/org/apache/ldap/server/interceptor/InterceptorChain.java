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


import org.apache.ldap.server.authn.AuthenticationService;
import org.apache.ldap.server.invocation.Invocation;
import org.apache.ldap.server.authz.AuthorizationService;

import javax.naming.NamingException;
import java.util.*;


/**
 * Manages the chain of {@link Interceptor}s.  <tt>InterceptorChain</tt>
 * is also an {@link Interceptor}, and thus you can create hiararchical
 * interceptor structure to break down complex interceptors.
 * <p/>
 * {@link org.apache.ldap.server.jndi.JndiProvider#invoke(Invocation)}
 * redirects {@link Invocation}s to {@link#process(Invocation)} and the
 * chain starts.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain implements Interceptor
{
    /**
     * The name of default interceptor that passes its control to the
     * next interceptor in parent chain.
     */
    public static final String NEXT_INTERCEPTOR = "nextInterceptor";


    /**
     * Returns a new chain of default interceptors required to run core.
     */
    public static InterceptorChain newDefaultChain()
    {
        InterceptorChain chain = new InterceptorChain();
        chain.addBefore( NEXT_INTERCEPTOR, "authenticator", new AuthenticationService() );
        chain.addBefore( NEXT_INTERCEPTOR, "authorizer", new AuthorizationService() );
        chain.addBefore( NEXT_INTERCEPTOR, "validator", new ExceptionService() );
        chain.addBefore( NEXT_INTERCEPTOR, "schemaManager", new SchemaService() );
        chain.addBefore( NEXT_INTERCEPTOR, "operationalAttributeInterceptor", new OperationalAttributeInterceptor() );
        return chain;
    }

    private final Interceptor NEXT_INTERCEPTOR0 = new Interceptor()
    {
        public void init( InterceptorContext context )
        {
        }

        public void destroy()
        {
        }

        public void process( NextInterceptor nextInterceptor, Invocation invocation ) throws NamingException
        {
            if( parent != null )
            {
                Entry e = ( Entry ) parent.interceptor2entry.get( InterceptorChain.this );
                e.nextInterceptor.process( invocation );
            }
            nextInterceptor.process( invocation );
        }
    };

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


        public void process( NextInterceptor nextInterceptor, Invocation call )
                throws NamingException
        {
            if ( parent == null )
            {
                // execute the actual backend operation only when this chain
                // is root.
                call.execute( ctx.getRootNexus() );
            }
        }
    };

    private InterceptorChain parent;

    private final Map name2entry = new HashMap();

    private final Map interceptor2entry = new IdentityHashMap();

    private Entry head = new Entry( null, null, NEXT_INTERCEPTOR, NEXT_INTERCEPTOR0 );

    private final Entry tail = new Entry( null, null, "end", FINAL_INTERCEPTOR );


    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
        head.nextEntry = tail;
        tail.prevEntry = head;
        register( NEXT_INTERCEPTOR, head );
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
                String name = getName( interceptor );
                InterceptorContext newCtx = new InterceptorContext( ctx.getEnvironment(), ctx.getSystemPartition(),
                        ctx.getGlobalRegistries(), ctx.getRootNexus(),
                        InterceptorConfigBuilder.build( ctx.getConfig(), ( name == null ) ? "" : name ) );

                interceptor.init( newCtx );
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
                throw new InterceptorException( interceptor, null,
                        "Failed to initialize interceptor chain.", t );
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


    private String getName( Interceptor interceptor )
    {
        Entry e = ( Entry ) interceptor2entry.get( interceptor );
        if ( e == null )
        {
            return null;
        }
        return e.name;
    }


    /**
     * Adds the specified interceptor with the specified name at the beginning of this chain.
     */
    public synchronized void addFirst( String name,
                                       Interceptor interceptor )
    {
        checkAddable( name, interceptor );

        Entry newEntry = new Entry( null, head, name, interceptor );
        head.prevEntry = newEntry;
        head = newEntry;

        register( name, newEntry );
    }


    /**
     * Adds the specified interceptor with the specified name at the end of this chain.
     */
    public synchronized void addLast( String name,
                                      Interceptor interceptor )
    {
        checkAddable( name, interceptor );

        Entry newEntry = new Entry( tail.prevEntry, tail, name, interceptor );
        if ( tail.prevEntry != null )
        {
            tail.prevEntry.nextEntry = newEntry;
        }
        else
        {
            head = newEntry;
        }
        tail.prevEntry = newEntry;

        register( name, newEntry );
    }


    /**
     * Adds the specified interceptor with the specified name just before the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName,
                                        String name,
                                        Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, interceptor );

        Entry prevEntry = baseEntry.prevEntry;
        Entry newEntry = new Entry( prevEntry, baseEntry, name, interceptor );
        if ( prevEntry == null )
        {
            baseEntry.prevEntry = newEntry;
            head = newEntry;
            
        }
        else
        {
            baseEntry.prevEntry = newEntry;
            prevEntry.nextEntry = newEntry;
        }

        register( name, newEntry );
    }


    /**
     * Adds the specified interceptor with the specified name just after the interceptor whose name is
     * <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName,
                                       String name,
                                       Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkAddable( name, interceptor );

        Entry nextEntry = baseEntry.nextEntry;
        Entry newEntry = new Entry( baseEntry, nextEntry, name, interceptor );
        if ( nextEntry == null )
        {
            throw new IllegalStateException();
        }

        nextEntry.prevEntry.nextEntry = newEntry;
        nextEntry.prevEntry = newEntry;
        register( name, newEntry );
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


    private void register( String name, Entry newEntry )
    {
        Interceptor interceptor = newEntry.interceptor;
        name2entry.put( name, newEntry );
        interceptor2entry.put( newEntry.interceptor, newEntry );
        if ( interceptor instanceof InterceptorChain )
        {
            ( ( InterceptorChain ) interceptor ).parent = this;
        }
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
            throw new IllegalArgumentException( "Unknown interceptor name:" +
                    baseName );
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
    public void process( NextInterceptor nextInterceptor, Invocation invocation ) throws NamingException
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
            throw new InterceptorException( head.interceptor, invocation,
                    "Unexpected exception.", e );
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

        private final String name;

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
            this.name = name;
            this.interceptor = interceptor;
            this.nextInterceptor = new NextInterceptor()
            {
                public void process( Invocation call )
                        throws NamingException
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
                        throw new InterceptorException( interceptor, call,
                                "Unexpected exception.", e );
                    }
                }
            };
        }
    }
}
