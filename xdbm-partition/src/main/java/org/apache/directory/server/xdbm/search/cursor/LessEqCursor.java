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
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.AbstractIndexCursor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.server.xdbm.search.evaluator.LessEqEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entry candidates matching a LessEq assertion filter.  This
 * Cursor operates in two modes.  The first is when an index exists for the
 * attribute the assertion is built on.  The second is when the user index for
 * the assertion attribute does not exist.  Different Cursors are used in each
 * of these cases where the other remains null.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LessEqCursor<V> extends AbstractIndexCursor<V>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( Loggers.CURSOR_LOG.getName() );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = LOG_CURSOR.isDebugEnabled();

    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_716 );

    /** An less eq evaluator for candidates */
    private final LessEqEvaluator<V> lessEqEvaluator;

    /** Cursor over attribute entry matching filter: set when index present */
    private final Cursor<IndexEntry<V, String>> userIdxCursor;

    /** NDN Cursor on all entries in  (set when no index on user attribute) */
    private final Cursor<IndexEntry<String, String>> uuidIdxCursor;

    /**
     * Used to store indexEntry from uudCandidate so it can be saved after
     * call to evaluate() which changes the value so it's not referring to
     * the String but to the value of the attribute instead.
     */
    private IndexEntry<String, String> uuidCandidate;


    /**
     * Creates a new instance of an LessEqCursor
     * 
     * @param partitionTxn The transaction to use
     * @param store The store
     * @param lessEqEvaluator The LessEqEvaluator
     * @throws LdapException If the creation failed
     * @throws IndexNotFoundException If the index was not found
     */
    @SuppressWarnings("unchecked")
    public LessEqCursor( PartitionTxn partitionTxn, Store store, LessEqEvaluator<V> lessEqEvaluator ) 
        throws LdapException, IndexNotFoundException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Creating LessEqCursor {}", this );
        }

        this.lessEqEvaluator = lessEqEvaluator;
        this.partitionTxn = partitionTxn;

        AttributeType attributeType = lessEqEvaluator.getExpression().getAttributeType();

        if ( store.hasIndexOn( attributeType ) )
        {
            userIdxCursor = ( ( Index<V, String> ) store.getIndex( attributeType ) ).forwardCursor( partitionTxn );
            uuidIdxCursor = null;
        }
        else
        {
            uuidIdxCursor = new AllEntriesCursor( partitionTxn, store );
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
    @Override
    public void before( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        checkNotClosed();

        if ( userIdxCursor != null )
        {
            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.  If the
             * element's value is greater than this upper bound then we
             * position the userIdxCursor after the last node.
             *
             * If the element's value is equal to this upper bound then we
             * position the userIdxCursor right before the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * before() method of the userIdxCursor.
             */
            int compareValue = lessEqEvaluator.getComparator().compare( element.getKey(),
                lessEqEvaluator.getExpression().getValue().getString() );

            if ( compareValue > 0 )
            {
                afterLast();
                return;
            }
            else if ( compareValue == 0 )
            {
                last();
                previous();
                setAvailable( false );
                return;
            }

            userIdxCursor.before( element );
            setAvailable( false );
        }
        else
        {
            super.before( element );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void after( IndexEntry<V, String> element ) throws LdapException, CursorException
    {
        checkNotClosed();

        if ( userIdxCursor != null )
        {
            int comparedValue = lessEqEvaluator.getComparator().compare( element.getKey(),
                lessEqEvaluator.getExpression().getValue().getString() );

            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the expression node.
             *
             * If the element's value is equal to or greater than this upper
             * bound then we position the userIdxCursor after the last node.
             *
             * If the element's value is smaller, then we delegate to the
             * after() method of the userIdxCursor.
             */
            if ( comparedValue >= 0 )
            {
                afterLast();
                return;
            }

            // Element is in the valid range as specified by assertion
            userIdxCursor.after( element );
            setAvailable( false );
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
        checkNotClosed();
        
        if ( userIdxCursor != null )
        {
            userIdxCursor.beforeFirst();
        }
        else
        {
            uuidIdxCursor.beforeFirst();
            uuidCandidate = null;
        }

        setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws LdapException, CursorException
    {
        checkNotClosed();
        
        if ( userIdxCursor != null )
        {
            IndexEntry<V, String> advanceTo = new IndexEntry<>();
            //noinspection unchecked
            String normalizedKey = lessEqEvaluator.getAttributeType().getEquality().getNormalizer().normalize( 
                lessEqEvaluator.getExpression().getValue().getString() );
            
            advanceTo.setKey( ( V ) normalizedKey );
            userIdxCursor.after( advanceTo );
        }
        else
        {
            uuidIdxCursor.afterLast();
            uuidCandidate = null;
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
    @Override
    public boolean previous() throws LdapException, CursorException
    {
        checkNotClosed();

        if ( userIdxCursor != null )
        {
            /*
             * No need to do the same check that is done in next() since
             * values are decreasing with calls to previous().  We will
             * always have lesser values.
             */
            return setAvailable( userIdxCursor.previous() );
        }

        while ( uuidIdxCursor.previous() )
        {
            checkNotClosed();
            uuidCandidate = uuidIdxCursor.get();

            if ( lessEqEvaluator.evaluate( partitionTxn, uuidCandidate ) )
            {
                return setAvailable( true );
            }
            else
            {
                uuidCandidate = null;
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
        checkNotClosed();

        if ( userIdxCursor != null )
        {
            /*
             * We have to check and make sure the next value complies by
             * being less than or eq to the expression node's value.  We need
             * to do this since values are increasing and we must limit to our
             * upper bound.
             */
            while ( userIdxCursor.next() )
            {
                checkNotClosed();
                IndexEntry<?, String> candidate = userIdxCursor.get();

                if ( lessEqEvaluator.getComparator().compare( candidate.getKey(),
                    lessEqEvaluator.getExpression().getValue().getString() ) <= 0 )
                {
                    return setAvailable( true );
                }
            }

            return setAvailable( false );
        }

        while ( uuidIdxCursor.next() )
        {
            checkNotClosed();
            uuidCandidate = uuidIdxCursor.get();

            if ( lessEqEvaluator.evaluate( partitionTxn, uuidCandidate ) )
            {
                return setAvailable( true );
            }
            else
            {
                uuidCandidate = null;
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

        if ( userIdxCursor != null )
        {
            if ( available() )
            {
                return userIdxCursor.get();
            }

            throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
        }

        if ( available() )
        {
            return ( IndexEntry<V, String> ) uuidCandidate;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException
    {
        if ( IS_DEBUG )
        {
            LOG_CURSOR.debug( "Closing LessEqCursor {}", this );
        }

        super.close();

        if ( userIdxCursor != null )
        {
            userIdxCursor.close();
        }
        else
        {
            uuidIdxCursor.close();
            uuidCandidate = null;
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws IOException
    {
        LOG_CURSOR.debug( "Closing LessEqCursor {}", this );
        super.close( cause );

        if ( userIdxCursor != null )
        {
            userIdxCursor.close( cause );
        }
        else
        {
            uuidIdxCursor.close( cause );
            uuidCandidate = null;
        }
    }


    /**
     * @see Object#toString()
     */
    @Override
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( tabs ).append( "LessEqCursor (" );

        if ( available() )
        {
            sb.append( "available)" );
        }
        else
        {
            sb.append( "absent)" );
        }

        sb.append( "#candidate<" ).append( uuidCandidate ).append( ">:\n" );

        sb.append( tabs + "  >>" ).append( lessEqEvaluator ).append( '\n' );

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