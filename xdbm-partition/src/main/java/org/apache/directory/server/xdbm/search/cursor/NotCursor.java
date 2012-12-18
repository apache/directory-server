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

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.Evaluator;
import org.apache.directory.shared.ldap.model.cursor.CursorException;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
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
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_718 );
    private final AllEntriesCursor uuidCursor;
    private final Evaluator<? extends ExprNode> childEvaluator;


    @SuppressWarnings("unchecked")
    public NotCursor( Store store, Evaluator<? extends ExprNode> childEvaluator )
        throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating NotCursor {}", this );
        }
        
        this.childEvaluator = childEvaluator;
        this.uuidCursor = new AllEntriesCursor( store );

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
    public void beforeFirst() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "beforeFirst()" );
        uuidCursor.beforeFirst();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "afterLast()" );
        uuidCursor.afterLast();
        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        while ( uuidCursor.previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?, String> candidate = uuidCursor.get();

            if ( !childEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws LdapException, CursorException, IOException
    {
        while ( uuidCursor.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<?, String> candidate = uuidCursor.get();

            if ( !childEvaluator.evaluate( candidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<V, String> get() throws CursorException, IOException
    {
        checkNotClosed( "get()" );

        if ( available() )
        {
            return ( IndexEntry<V, String> ) uuidCursor.get();
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
            LOG_CURSOR.debug( "Closing NotCursor {}", this );
        }
        
        super.close();
        uuidCursor.close();
    }


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause )
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
        return toString( "" );
    }
}
