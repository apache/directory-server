/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.cursor;


import java.util.Iterator;


/**
 * Simple class that contains often used Cursor code.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractCursor<E> implements Cursor<E>
{
    /** The default associated monitor */
    private ClosureMonitor monitor = new DefaultClosureMonitor();

    /**
     * {@inheritDoc}
     */
    public final void setClosureMonitor( ClosureMonitor monitor )
    {
        if ( monitor == null )
        {
            throw new NullPointerException( "monitor" );
        }
        
        this.monitor = monitor;
    }
    

    /**
     * Check that the cursor is not closed.
     * 
     * @param operation
     * @throws Exception
     */
    protected final void checkNotClosed( String operation ) throws Exception
    {
        monitor.checkNotClosed();
    }


    /**
     * {@inheritDoc}
     */
    public final boolean isClosed()
    {
        return monitor.isClosed();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause ) throws Exception
    {
        monitor.close( cause );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        monitor.close();
    }


    /**
     * {@inheritDoc}
     */
    public Iterator<E> iterator()
    {
        return new CursorIterator<E>( this );
    }
}
