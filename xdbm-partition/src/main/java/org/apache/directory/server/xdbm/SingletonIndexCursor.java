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


/**
 * A Cursor over a single element.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SingletonIndexCursor<K, E, ID> extends AbstractIndexCursor<K, E, ID>
{
    private boolean beforeFirst = true;
    private boolean afterLast;
    private boolean onSingleton;
    private final IndexEntry<K, E, ID> singleton;


    public SingletonIndexCursor( IndexEntry<K, E, ID> singleton )
    {
        this.singleton = singleton;
    }


    public boolean available()
    {
        return onSingleton;
    }


    public void before( IndexEntry<K, E, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void beforeValue( ID id, K value ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void afterValue( ID id, K value ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    public void after( IndexEntry<K, E, ID> element ) throws Exception
    {
        throw new UnsupportedOperationException();
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


    public IndexEntry<K, E, ID> get() throws Exception
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
}
