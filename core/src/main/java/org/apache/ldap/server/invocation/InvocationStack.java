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
package org.apache.ldap.server.invocation;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.Context;


/**
 * Keeps track of recursive {@link Invocation}s.  This stack assumes an invocation
 * occurs in the same thread since it is called first, so we manages stacks
 * for each invocation in {@link ThreadLocal}-like manner.  You can just use
 * {@link #getInstance()} to get current invocation stack.
 * <p>
 * Using {@link InvocationStack}, you can find out current effective JNDI
 * {@link Context} or detect infinite recursions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class InvocationStack
{
    // I didn't use ThreadLocal to release contexts explicitly.
    // It seems like JDK 1.5 supports explicit release by introducing
    // <tt>ThreadLocal.remove()</tt>, but we're still targetting 1.4.
    private static final Map stacks = new IdentityHashMap();
    
    /**
     * Returns the invocation stack of current thread.
     */
    public static InvocationStack getInstance()
    {
        Thread currentThread = Thread.currentThread();
        InvocationStack ctx;
        synchronized( stacks )
        {
            ctx = ( InvocationStack ) stacks.get( currentThread );
            if( ctx == null )
            {
                ctx = new InvocationStack();
            }
        }
        return ctx;
    }

    private final Thread thread;
    private final List stack = new ArrayList();

    private InvocationStack()
    {
        Thread currentThread = Thread.currentThread();
        this.thread = currentThread;
        // This operation is already synchronized from getInstance()
        stacks.put( currentThread, this );
    }
    
    /**
     * Returns an array of {@link Invocation}s.  0th element is the
     * latest invocation.
     */
    public Invocation[] toArray()
    {
        Invocation[] result = new Invocation[ stack.size() ];
        result = ( Invocation[] ) stack.toArray( result );
        return result;
    }
    
    /**
     * Returns the latest invocation.
     */
    public Invocation peek()
    {
        return ( Invocation ) this.stack.get( 0 );
    }

    /**
     * Returns true if the stack is empty false otherwise.
     */
    public boolean isEmpty()
    {
        return this.stack.isEmpty();
    }

    /**
     * Pushes the specified invocation to this stack.
     */
    public void push( Invocation invocation )
    {
        this.stack.add( 0, invocation );
    }
    
    /**
     * Pops the latest invocation from this stack.  This stack is released
     * automatically if you pop all items from this stack.
     */
    public Invocation pop()
    {
        Invocation invocation = ( Invocation ) this.stack.remove( 0 );
        if( this.stack.size() == 0 )
        {
            synchronized( stacks )
            {
                stacks.remove( thread );
            }
        }

        return invocation;
    }
}
