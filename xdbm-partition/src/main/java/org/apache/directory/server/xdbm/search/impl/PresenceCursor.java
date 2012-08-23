/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.xdbm.search.impl;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A returning candidates satisfying an attribute presence expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PresenceCursor<ID extends Comparable<ID>> extends AbstractIndexCursor<String, ID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_724 );
    private final IndexCursor<String, ID> uuidCursor;
    private final IndexCursor<String, ID> presenceCursor;
    private final PresenceEvaluator<ID> presenceEvaluator;

    /** The prefetched entry, if it's a valid one */
    private IndexEntry<String, ID> prefetched;


    public PresenceCursor( Store<Entry, ID> store, PresenceEvaluator<ID> presenceEvaluator ) throws Exception
    {
        LOG_CURSOR.debug( "Creating PresenceCursor {}", this );
        this.presenceEvaluator = presenceEvaluator;
        AttributeType type = presenceEvaluator.getAttributeType();

        // we don't maintain a presence index for objectClass, entryUUID, and entryCSN
        // as it doesn't make sense because every entry has such an attribute
        // instead for those attributes and all un-indexed attributes we use the ndn index
        if ( store.hasUserIndexOn( type ) )
        {
            presenceCursor = store.getPresenceIndex().forwardCursor( type.getOid() );
            uuidCursor = null;
        }
        else
        {
            presenceCursor = null;
            uuidCursor = store.getEntryUuidIndex().forwardCursor();
        }
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
        if ( presenceCursor != null )
        {
            return presenceCursor.available();
        }

        return super.available();
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( ID id, String value ) throws Exception
    {
        checkNotClosed( "beforeValue()" );

        if ( presenceCursor != null )
        {
            presenceCursor.beforeValue( id, value );

            return;
        }

        super.beforeValue( id, value );
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<String, ID> element ) throws Exception
    {
        checkNotClosed( "before()" );

        if ( presenceCursor != null )
        {
            presenceCursor.before( element );

            return;
        }

        super.before( element );
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( ID id, String value ) throws Exception
    {
        checkNotClosed( "afterValue()" );

        if ( presenceCursor != null )
        {
            presenceCursor.afterValue( id, value );

            return;
        }

        super.afterValue( id, value );
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<String, ID> element ) throws Exception
    {
        checkNotClosed( "after()" );

        if ( presenceCursor != null )
        {
            presenceCursor.after( element );

            return;
        }

        super.after( element );
    }


    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );

        if ( presenceCursor != null )
        {
            presenceCursor.beforeFirst();

            return;
        }

        uuidCursor.beforeFirst();
        setAvailable( false );
    }


    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );

        if ( presenceCursor != null )
        {
            presenceCursor.afterLast();
            return;
        }

        uuidCursor.afterLast();
        setAvailable( false );
    }


    public boolean first() throws Exception
    {
        checkNotClosed( "first()" );
        if ( presenceCursor != null )
        {
            return presenceCursor.first();
        }

        beforeFirst();
        return next();
    }


    public boolean last() throws Exception
    {
        checkNotClosed( "last()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.last();
        }

        afterLast();

        return previous();
    }


    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.previous();
        }

        while ( uuidCursor.previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?, ID> candidate = uuidCursor.get();

            if ( presenceEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.next();
        }

        while ( uuidCursor.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<String, ID> candidate = uuidCursor.get();

            if ( presenceEvaluator.evaluate( candidate ) )
            {
                prefetched = candidate;

                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    public IndexEntry<String, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

        if ( presenceCursor != null )
        {
            if ( presenceCursor.available() )
            {
                return presenceCursor.get();
            }

            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
        }

        if ( available() )
        {
            if ( prefetched == null )
            {
                prefetched = uuidCursor.get();
            }

            /*
             * The value of NDN indices is the normalized dn and we want the
             * value to be the value of the attribute in question.  So we will
             * set that accordingly here.
             */
            prefetched.setKey( presenceEvaluator.getAttributeType().getOid() );

            return prefetched;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing PresenceCursor {}", this );
        super.close();

        if ( presenceCursor != null )
        {
            presenceCursor.close();
        }
        else
        {
            uuidCursor.close();
        }
    }


    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing PresenceCursor {}", this );
        super.close( cause );

        if ( presenceCursor != null )
        {
            presenceCursor.close( cause );
        }
        else
        {
            uuidCursor.close( cause );
        }
    }
}
