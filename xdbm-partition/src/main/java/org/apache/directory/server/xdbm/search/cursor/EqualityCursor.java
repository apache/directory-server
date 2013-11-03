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
package org.apache.directory.server.xdbm.search.cursor;


import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.evaluator.EqualityEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entry candidates matching an equality assertion filter.  This
 * Cursor operates in two modes.  The first is when an index exists for the
 * attribute the equality assertion is built on.  The second is when the user
 * index for the assertion attribute does not exist.  Different Cursors are
 * used in each of these cases where the other remains null.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EqualityCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    /** The message for unsupported operations */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_714 );

    /** An equality evaluator for candidates */
    private final EqualityEvaluator<V> equalityEvaluator;

    /** Cursor over attribute entry matching filter: set when index present */
    private final Cursor<IndexEntry<V, String>> userIdxCursor;

    /** NDN Cursor on all entries in  (set when no index on user attribute) */
    private final Cursor<IndexEntry<String, String>> uuidIdxCursor;


    /**
     * Creates a new instance of an EqualityCursor
     * @param store The store
     * @param equalityEvaluator The EqualityEvaluator
     * @throws Exception If the creation failed
     */
    @SuppressWarnings("unchecked")
    public EqualityCursor( Store store, EqualityEvaluator<V> equalityEvaluator ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating EqualityCursor {}", this );
        }

        this.equalityEvaluator = equalityEvaluator;

        AttributeType attributeType = equalityEvaluator.getExpression().getAttributeType();
        Value<V> value = equalityEvaluator.getExpression().getValue();

        if ( store.hasIndexOn( attributeType ) )
        {
            Index<V, String> userIndex = ( Index<V, String> ) store.getIndex( attributeType );
            userIdxCursor = userIndex.forwardCursor( value.getValue() );
            uuidIdxCursor = null;
        }
        else
        {
            uuidIdxCursor = new AllEntriesCursor( store );
            userIdxCursor = null;
        }
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
    public boolean available()
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.available();
        }

        return super.available();
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        checkNotClosed( "before()" );

        if ( userIdxCursor != null )
        {
            userIdxCursor.before( element );
        }
        else
        {
            super.before( element );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        checkNotClosed( "after()" );

        if ( userIdxCursor != null )
        {
            userIdxCursor.after( element );
        }
        else
        {
            super.after( element );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed( "beforeFirst()" );

        if ( userIdxCursor != null )
        {
            userIdxCursor.beforeFirst();
        }
        else
        {
            uuidIdxCursor.beforeFirst();
        }

        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed( "afterLast()" );

        if ( userIdxCursor != null )
        {
            userIdxCursor.afterLast();
        }
        else
        {
            uuidIdxCursor.afterLast();
        }

        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException
    {
        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.previous();
        }

        while ( uuidIdxCursor.previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?, String> candidate = uuidIdxCursor.get();

            if ( equalityEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException
    {
        if ( userIdxCursor != null )
        {
            return userIdxCursor.next();
        }

        while ( uuidIdxCursor.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<?, String> candidate = uuidIdxCursor.get();

            if ( equalityEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public IndexEntry<V, String> get() throws CursorException
    {
        checkNotClosed( "get()" );

        if ( userIdxCursor != null )
        {
            return userIdxCursor.get();
        }

        if ( available() )
        {
            return ( IndexEntry<V, String> ) uuidIdxCursor.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EqualityCursor {}", this );
        }

        super.close();

        if ( userIdxCursor != null )
        {
            userIdxCursor.close();
        }
        else
        {
            uuidIdxCursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing EqualityCursor {}", this );
        }

        super.close( cause );

        if ( userIdxCursor != null )
        {
            userIdxCursor.close( cause );
        }
        else
        {
            uuidIdxCursor.close( cause );
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "EqualityCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( " :\n" );

        sb.append( tabs + "  >>" ).append( equalityEvaluator );

        if ( userIdxCursor != null )
        {
            sb.append( tabs + "  <user>\n" );
            sb.append( userIdxCursor.toString( tabs + "    " ) );
        }

        if ( uuidIdxCursor != null )
        {
            sb.append( tabs + "  <uuid>\n" );
            sb.append( uuidIdxCursor.toString( tabs + "  " ) );
        }

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return toString( "" );
    }
}
