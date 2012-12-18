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
import org.apache.directory.server.xdbm.search.evaluator.PresenceEvaluator;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.CursorException;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A returning candidates satisfying an attribute presence expression.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PresenceCursor extends AbstractIndexCursor<String>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_724 );
    private final Cursor<IndexEntry<String, String>> uuidCursor;
    private final Cursor<IndexEntry<String, String>> presenceCursor;
    private final PresenceEvaluator presenceEvaluator;

    /** The prefetched entry, if it's a valid one */
    private IndexEntry<String, String> prefetched;


    public PresenceCursor( Store store, PresenceEvaluator presenceEvaluator ) throws Exception
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating PresenceCursor {}", this );
        }
        
        this.presenceEvaluator = presenceEvaluator;
        AttributeType type = presenceEvaluator.getAttributeType();

        // we don't maintain a presence index for objectClass, and entryCSN
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
            uuidCursor = new AllEntriesCursor( store );
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
    public void before( IndexEntry<String, String> element ) throws LdapException, CursorException, IOException
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
    public void after( IndexEntry<String, String> element ) throws LdapException, CursorException, IOException
    {
        checkNotClosed( "after()" );

        if ( presenceCursor != null )
        {
            presenceCursor.after( element );

            return;
        }

        super.after( element );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws LdapException, CursorException, IOException
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


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException, IOException
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


    /**
     * {@inheritDoc}
     */
    public boolean first() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "first()" );
        if ( presenceCursor != null )
        {
            return presenceCursor.first();
        }

        beforeFirst();
        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "last()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.last();
        }

        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws LdapException, CursorException, IOException
    {
        checkNotClosed( "previous()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.previous();
        }

        while ( uuidCursor.previous() )
        {
            checkNotClosed( "previous()" );
            IndexEntry<?, String> candidate = uuidCursor.get();

            if ( presenceEvaluator.evaluate( candidate ) )
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
        checkNotClosed( "next()" );

        if ( presenceCursor != null )
        {
            return presenceCursor.next();
        }

        while ( uuidCursor.next() )
        {
            checkNotClosed( "next()" );
            IndexEntry<String, String> candidate = uuidCursor.get();

            if ( presenceEvaluator.evaluate( candidate ) )
            {
                prefetched = candidate;

                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<String, String> get() throws CursorException, IOException
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


    /**
     * {@inheritDoc}
     */
    public void close()
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing PresenceCursor {}", this );
        }
        
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


    /**
     * {@inheritDoc}
     */
    public void close( Exception cause )
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing PresenceCursor {}", this );
        }
        
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


    /**
     * @see Object#toString()
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "PresenceCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( " :\n" );

        sb.append( tabs + "  >>" ).append( presenceEvaluator ).append( '\n' );

        if ( presenceCursor != null )
        {
            sb.append( tabs + "  <presence>\n" );
            sb.append( presenceCursor.toString( tabs + "    " ) );
        }

        if ( uuidCursor != null )
        {
            sb.append( tabs + "  <uuid>\n" );
            sb.append( uuidCursor.toString( tabs + "  " ) );
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
