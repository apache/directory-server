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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.CursorException;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
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
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final IndexEntry<V, String> singleton;


    public SingletonIndexCursor( IndexEntry<V, String> singleton )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating SingletonIndexCursor {}", this );
        }
        
        this.singleton = singleton;
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }


    public boolean available()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
        beforeFirst = true;
        afterLast = false;
        onSingleton = false;
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        afterLast = true;
        onSingleton = false;
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isFirst()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLast()
    {
        return onSingleton;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isAfterLast()
    {
        return afterLast;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isBeforeFirst() 
    {
        return beforeFirst;
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
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
    public boolean next() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "()" );
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
    public IndexEntry<V, String> get() throws CursorException, IOException
    {
        checkNotClosed( "()" );

        if ( onSingleton )
        {
            return singleton;
        }

        if ( beforeFirst )
        {
            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_705 ) );
        }
        else
        {
            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_706 ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void close()
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
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing SingletonIndexCursor {}", this );
        }
        
        super.close( cause );
    }
}
