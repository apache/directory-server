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


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over a single element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingletonIndexCursor<V, ID> extends AbstractIndexCursor<V, ID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final IndexEntry<V, ID> singleton;


    public SingletonIndexCursor( IndexEntry<V, ID> singleton )
    {
        LOG_CURSOR.debug( "Creating SingletonIndexCursor {}", this );
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


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "()" );
        beforeFirst = true;
        afterLast = false;
        onSingleton = false;
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        afterLast = true;
        onSingleton = false;
    }


    public boolean first() throws Exception
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean last() throws Exception
    {
        checkNotClosed( "()" );
        beforeFirst = false;
        onSingleton = true;
        afterLast = false;
        return true;
    }


    public boolean isFirst() throws Exception
    {
        checkNotClosed( "()" );
        return onSingleton;
    }


    public boolean isLast() throws Exception
    {
        checkNotClosed( "()" );
        return onSingleton;
    }


    public boolean isAfterLast() throws Exception
    {
        checkNotClosed( "()" );
        return afterLast;
    }


    public boolean isBeforeFirst() throws Exception
    {
        checkNotClosed( "()" );
        return beforeFirst;
    }


    public boolean previous() throws Exception
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


    public boolean next() throws Exception
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


    public IndexEntry<V, ID> get() throws Exception
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


    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing SingletonIndexCursor {}", this );
        super.close();
    }


    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing SingletonIndexCursor {}", this );
        super.close( cause );
    }
}
