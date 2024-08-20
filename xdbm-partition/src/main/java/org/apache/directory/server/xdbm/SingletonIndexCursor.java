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
package org.apache.directory.server.xdbm;


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over a single element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingletonIndexCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final IndexEntry<V, String> singleton;


    public SingletonIndexCursor( PartitionTxn partitionTxn, IndexEntry<V, String> singleton )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating SingletonIndexCursor {}", this );
        }

        this.singleton = singleton;
        this.partitionTxn = partitionTxn;
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean available()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        beforeFirst = true;
        afterLast = false;
        onSingleton = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        beforeFirst = false;
        afterLast = true;
        onSingleton = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean first() throws LdapException, CursorException
    {
        checkNotClosed();
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean last() throws LdapException, CursorException
    {
        checkNotClosed();
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isFirst()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLast()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAfterLast()
    {
        return afterLast;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isBeforeFirst()
    {
        return beforeFirst;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();
        if ( beforeFirst )
        {
            return false;
        }

        if ( afterLast )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }

        // must be on the singleton
        beforeFirst = true;
        onSingleton = false;
        afterLast = false;
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() throws LdapException, CursorException
    {
        checkNotClosed();
        
        if ( beforeFirst )
        {
            beforeFirst = false;
            onSingleton = true;
            afterLast = false;
            return true;
        }

        if ( afterLast )
        {
            return false;
        }

        // must be on the singleton
        beforeFirst = false;
        onSingleton = false;
        afterLast = true;
        return false;
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, String> get() throws CursorException
    {
        checkNotClosed();

        if ( onSingleton )
        {
            return singleton;
        }

        if ( beforeFirst )
        {
            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_49032_CANNOT_ACCESS_ELEMENT_BEFORE_FIRST ) );
        }
        else
        {
            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_49033_CANNOT_ACCESS_ELEMENT_AFTER_LAST ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing SingletonIndexCursor {}", this );
        }

        super.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing SingletonIndexCursor {}", this );
        }

        super.close( cause );
    }
}
