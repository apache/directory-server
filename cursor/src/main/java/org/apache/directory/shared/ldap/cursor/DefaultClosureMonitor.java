/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.cursor;


/**
 * A basic ClosureMonitor that simply uses a boolean for state and a cause 
 * exception.
 * 
 * Note that we consciously chose not to synchronize close() operations with
 * checks to see if the monitor state is closed because it costs to 
 * synchronize and it's OK for the Cursor not to stop immediately when close()
 * is called.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DefaultClosureMonitor implements ClosureMonitor
{
    /** Tells if the monitor is closed or not */
    private boolean closed;
    
    /** If we get an exception, the cause is stored in this variable */
    private Exception cause;

    
    /**
     * {@inheritDoc}
     */
    public final void close()
    {
        // state check needed to "try" not to overwrite exception (lack of 
        // synchronization may still allow overwriting but who cares that much
        if ( ! closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            cause = new CursorClosedException();
            closed = true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public final void close( final String cause )
    {
        // state check needed to "try" not to overwrite exception (lack of 
        // synchronization may still allow overwriting but who cares that much
        if ( ! closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            this.cause = new CursorClosedException( cause );
            closed = true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public final void close( final Exception cause )
    {
        // state check needed to "try" not to overwrite exception (lack of 
        // synchronization may still allow overwriting but who cares that much
        if ( ! closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            this.cause = cause;
            closed = true;
        }
    }


    /**
     * {@inheritDoc}
     */
    public final Exception getCause()
    {
        return cause;
    }


    /**
     * {@inheritDoc}
     */
    public final boolean isClosed()
    {
        return closed;
    }


    /**
     * {@inheritDoc}
     */
    public void checkNotClosed() throws Exception
    {
        // lack of synchronization may cause pass but eventually it will work
        if ( closed )
        {
            throw cause;
        }
    }
}
