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


import java.io.IOException;

import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor returning candidates satisfying a logical negation expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NotCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_49025_NOT_CURSOR_NOT_ORDERED );
    private final AllEntriesCursor uuidCursor;
    private final Evaluator<? extends ExprNode> childEvaluator;


    /**
     * Creates a new instance of an NotCursor
     * 
     * @param partitionTxn The transaction to use
     * @param store The store
     * @param childEvaluator The inner evaluator
     * @throws LdapException If the creation failed
     */
    public NotCursor( PartitionTxn partitionTxn, Store store, Evaluator<? extends ExprNode> childEvaluator ) 
            throws LdapException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating NotCursor {}", this );
        }

        this.childEvaluator = childEvaluator;
        this.partitionTxn = partitionTxn;
        this.uuidCursor = new AllEntriesCursor( partitionTxn, store );

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
    public void beforeFirst() throws LdapException, CursorException
    {
        checkNotClosed();
        uuidCursor.beforeFirst();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        uuidCursor.afterLast();
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
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        while ( uuidCursor.previous() )
        {
            checkNotClosed();
            IndexEntry<?, String> candidate = uuidCursor.get();

            if ( !childEvaluator.evaluate( partitionTxn, candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean next() throws LdapException, CursorException
    {
        while ( uuidCursor.next() )
        {
            checkNotClosed();
            IndexEntry<?, String> candidate = uuidCursor.get();

            if ( !childEvaluator.evaluate( partitionTxn, candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, String> get() throws CursorException
    {
        checkNotClosed();

        if ( available() )
        {
            return ( IndexEntry<V, String> ) uuidCursor.get();
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_49021_CURSOR_NOT_POSITIONED ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing NotCursor {}", this );
        }

        super.close();
        uuidCursor.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing NotCursor {}", this );
        }

        super.close( cause );
        uuidCursor.close( cause );
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "NotCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( tabs + "  >>" ).append( childEvaluator ).append( '\n' );

        sb.append( uuidCursor.toString( tabs + "    " ) );

        return sb.toString();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "";
    }
}
