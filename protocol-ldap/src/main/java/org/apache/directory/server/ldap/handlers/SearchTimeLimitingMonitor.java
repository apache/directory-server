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
package org.apache.directory.server.ldap.handlers;


import java.util.concurrent.TimeUnit;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.ClosureMonitor;
import org.apache.directory.shared.ldap.model.cursor.CursorClosedException;
import org.apache.directory.shared.ldap.model.exception.LdapTimeLimitExceededException;


/**
 * A ClosureMonitor implementation which takes into account a time limit.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchTimeLimitingMonitor implements ClosureMonitor
{
    private final long startTime = System.currentTimeMillis();
    private final long millisToLive;

    private boolean closed;
    private Exception cause;


    /**
     * Creates a new instance of SearchTimeLimitingMonitor.
     *
     * @param timeToLive the time before changing state to closed.
     * @param unit the time units for the timeToLive parameter
     * @see TimeUnit
     */
    public SearchTimeLimitingMonitor( long timeToLive, TimeUnit unit )
    {
        switch ( unit )
        {
            case MICROSECONDS:
                this.millisToLive = timeToLive / 1000;
                break;
            case MILLISECONDS:
                this.millisToLive = timeToLive;
                break;
            case SECONDS:
                this.millisToLive = timeToLive * 1000;
                break;
            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_687, unit ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void checkNotClosed() throws CursorClosedException
    {
        if ( ( System.currentTimeMillis() > startTime + millisToLive ) && !closed )
        {
            // state check needed to "try" not to overwrite exception (lack of 
            // synchronization may still allow overwriting but who cares that 
            // much
            // not going to sync because who cares if it takes a little 
            // longer to stop but we need to set cause before toggling 
            // closed state or else check for closure can throw null cause 
            cause = new LdapTimeLimitExceededException();
            closed = true;
        }

        if ( closed )
        {
            throw new CursorClosedException( cause.getMessage(), cause );
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.ClosureMonitor#close()
     */
    public void close()
    {
        if ( !closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            cause = new CursorClosedException();
            closed = true;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.ClosureMonitor#close(java.lang.String)
     */
    public void close( String cause )
    {
        if ( !closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            this.cause = new CursorClosedException( cause );
            closed = true;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.ClosureMonitor#close(java.lang.Exception)
     */
    public void close( Exception cause )
    {
        if ( !closed )
        {
            // not going to sync because who cares if it takes a little longer 
            // to stop but we need to set cause before toggling closed state 
            // or else check for closure can throw null cause 
            this.cause = cause;
            closed = true;
        }
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.ClosureMonitor#getCause()
     */
    public Exception getCause()
    {
        return cause;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.directory.server.core.cursor.ClosureMonitor#isClosed()
     */
    public boolean isClosed()
    {
        if ( System.currentTimeMillis() > startTime + millisToLive )
        {
            // set cause first always
            cause = new LdapTimeLimitExceededException();
            closed = true;
        }

        return closed;
    }
}
