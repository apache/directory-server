package org.apache.ldap.server.jndi.call.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.server.jndi.call.Call;

/**
 * Manages {@link Interceptor} stack.  The first {@link Interceptor} is
 * invoked and then invocation chain starts.
 * 
 * TODO imeplement me.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private static final Interceptor FINAL_PROCESSOR = new Interceptor()
    {
        public void init(Properties config) throws NamingException
        {
            // do nothing
        }

        public void destroy()
        {
            // do nothing
        }

        public void process(NextInterceptor nextProcessor, Call request)
                throws NamingException
        {
            // do nothing
        }
    };

    private final Map name2entry = new HashMap();
    private Entry head = new Entry( null, null, "", FINAL_PROCESSOR );
    private final Entry tail = head;

    /**
     * Create a new processor chain.
     */
    public InterceptorChain()
    {
    }

    /**
     * Returns the processor with the specified <code>name</code>.
     * 
     * @return <code>null</code> if there is no processor with the specified
     *         <code>name</code>.
     */
    public Interceptor get( String name )
    {
        Entry e = ( Entry ) name2entry.get( name );
        if( e == null )
        {
            return null;
        }
        return e.processor;
    }

    /**
     * Adds the specified processor with the specified name at the beginning
     * of this chain.
     */
    public synchronized void addFirst( String name,
                                       Interceptor processor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( null, head, name, processor );
        head.prevEntry = newEntry;
        head = newEntry;
    }

    /**
     * Adds the specified processor with the specified name at the end
     * of this chain.
     */
    public synchronized void addLast( String name,
                                      Interceptor processor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( tail.prevEntry, tail, name, processor );
        tail.prevEntry.nextEntry = newEntry;
        tail.prevEntry = newEntry;
    }

    /**
     * Adds the specified processor with the specified name just before
     * the processor whose name is <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName,
                                        String name,
                                        Interceptor processor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkNewName( name );

        Entry prevEntry = baseEntry.prevEntry;
        Entry newEntry = new Entry( prevEntry, baseEntry, name, processor );
        if( prevEntry == null )
        {
            head = newEntry;
        }
        else
        {
            prevEntry.nextEntry.prevEntry = newEntry;
            prevEntry.nextEntry = newEntry;
        }
        
        name2entry.put( name, newEntry );
    }
    
    /**
     * Adds the specified processor with the specified name just after
     * the processor whose name is <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName,
                                       String name,
                                       Interceptor processor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkNewName(name);

        Entry nextEntry = baseEntry.nextEntry;
        Entry newEntry = new Entry( baseEntry, nextEntry, name, processor );
        if( nextEntry == null )
        {
            throw new IllegalStateException();
        }

        nextEntry.prevEntry.nextEntry = newEntry;
        nextEntry.prevEntry = newEntry;
        name2entry.put( name, newEntry );
    }
    
    /**
     * Removes the processor with the specified name from this chain.
     */
    public synchronized void remove( String name )
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        if( prevEntry == null )
        {
            nextEntry.prevEntry = null;
            head = entry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;
            nextEntry.prevEntry = prevEntry;
        }
    }

    /**
     * Removed all processors added to this chain.
     */
    public synchronized void clear()
    {
        tail.prevEntry = null;
        tail.nextEntry = null;
        head = tail;
    }

    private Entry checkOldName( String baseName )
    {
        Entry e = ( Entry ) name2entry.get( baseName );
        if( e == null )
        {
            throw new IllegalArgumentException( "Unknown processor name:" +
                                                baseName );
        }
        return e;
    }

    private void checkNewName( String name )
    {
        if( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException(
                    "Other processor is using name '" + name + "'" );
        }
    }
    
    /**
     * Start invocation chain with the specified invocation.
     * @throws NamingException if invocation failed
     */
    public void process( Call request ) throws NamingException
    {
        Entry head = this.head;
        try
        {
            head.processor.process(
                    head.nextProcessor, request );
        }
        catch( NamingException ne )
        {
            throw ne;
        }
        catch( Throwable e )
        {
            throw new InterceptorException( head.processor, request,
                                            "Unexpected exception.", e );
        }
    }

    /**
     * Returns the list of processors this chain contains in the order of
     * evaluation.
     */
    public List getAll()
    {
        List list = new ArrayList();
        Entry e = head;
        do
        {
            list.add( e.processor );
            e = e.nextEntry;
        }
        while( e != null );

        return list;
    }

    /**
     * Returns the list of processors this chain contains in the reversed
     * order of evaluation.
     */
    public List getAllReversed()
    {
        List list = new ArrayList();
        Entry e = tail;
        do
        {
            list.add( e.processor );
            e = e.prevEntry;
        }
        while( e != null );

        return list;
    }

    private class Entry
    {
        private Entry prevEntry;
        private Entry nextEntry;
        //private final String name;
        private final Interceptor processor;
        private final NextInterceptor nextProcessor;

        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, Interceptor processor )
        {
            if( processor == null )
            {
                throw new NullPointerException( "processor" );
            }
            if( name == null )
            {
                throw new NullPointerException( "name" );
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            //this.name = name;
            this.processor = processor;
            this.nextProcessor = new NextInterceptor()
            {
                public void process(Call request)
                        throws NamingException {
                    Interceptor processor = Entry.this.nextEntry.processor;
                    try
                    {
                        processor.process(
                                Entry.this.nextEntry.nextProcessor, request );
                    }
                    catch( NamingException ne )
                    {
                        throw ne;
                    }
                    catch( Throwable e )
                    {
                        throw new InterceptorException( processor, request,
                                                             "Unexpected exception.", e );
                    }
                }
            };
        }
    }
}
