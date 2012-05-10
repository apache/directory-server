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
import org.apache.directory.server.xdbm.ForwardIndexEntry;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Cursor over entry candidates matching a GreaterEq assertion filter.  This
 * Cursor operates in two modes.  The first is when an index exists for the
 * attribute the assertion is built on.  The second is when the user index for
 * the assertion attribute does not exist.  Different Cursors are used in each
 * of these cases where the other remains null.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class GreaterEqCursor<V, ID extends Comparable<ID>> extends AbstractIndexCursor<V, Entry, ID>
{
    /** A dedicated log for cursors */
    private static final Logger LOG_CURSOR = LoggerFactory.getLogger( "CURSOR" );

    private static final String UNSUPPORTED_MSG = "GreaterEqCursors only support positioning by element when a user index exists on the asserted attribute.";

    /** An greater eq evaluator for candidates */
    private final GreaterEqEvaluator<V, ID> greaterEqEvaluator;

    /** Cursor over attribute entry matching filter: set when index present */
    private final IndexCursor<V, Entry, ID> userIdxCursor;

    /** NDN Cursor on all entries in  (set when no index on user attribute) */
    private final IndexCursor<String, Entry, ID> uuidIdxCursor;

    /**
     * Used to store indexEntry from uuidCandidate so it can be saved after
     * call to evaluate() which changes the value so it's not referring to
     * the NDN but to the value of the attribute instead.
     */
    private IndexEntry<String, ID> uuidCandidate;


    /**
     * Creates a new instance of an GreaterEqCursor
     * @param db The store
     * @param equalityEvaluator The GreaterEqEvaluator
     * @throws Exception If the creation failed
     */
    @SuppressWarnings("unchecked")
    public GreaterEqCursor( Store<Entry, ID> db, GreaterEqEvaluator<V, ID> greaterEqEvaluator ) throws Exception
    {
        LOG_CURSOR.debug( "Creating GreaterEqCursor {}", this );
        this.greaterEqEvaluator = greaterEqEvaluator;

        AttributeType attributeType = greaterEqEvaluator.getExpression().getAttributeType();

        if ( db.hasIndexOn( attributeType ) )
        {
            userIdxCursor = ( ( Index<V, Entry, ID> ) db.getIndex( attributeType ) ).forwardCursor();
            uuidIdxCursor = null;
        }
        else
        {
            uuidIdxCursor = db.getEntryUuidIndex().forwardCursor();
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
    public void beforeValue( ID id, V value ) throws Exception
    {
        checkNotClosed( "beforeValue()" );

        if ( userIdxCursor != null )
        {
            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the node.  If it is smaller or
             * equal to this lower bound then we simply position the
             * userIdxCursor before the first element.  Otherwise we let the
             * underlying userIdx Cursor position the element.
             */
            if ( greaterEqEvaluator.getComparator()
                .compare( value, greaterEqEvaluator.getExpression().getValue().getValue() ) <= 0 )
            {
                beforeFirst();
                return;
            }

            userIdxCursor.beforeValue( id, value );
            setAvailable( false );
        }
        else
        {
            super.beforeValue( id, value );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( ID id, V value ) throws Exception
    {
        checkNotClosed( "afterValue()" );

        if ( userIdxCursor != null )
        {
            int comparedValue = greaterEqEvaluator.getComparator().compare( value,
                greaterEqEvaluator.getExpression().getValue().getValue() );

            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the node.  If it is equal to this
             * lower bound then we simply position the userIdxCursor after
             * this first node.  If it is less than this value then we
             * position the Cursor before the first entry.
             */
            if ( comparedValue == 0 )
            {
                userIdxCursor.afterValue( id, value );
                setAvailable( false );

                return;
            }
            else if ( comparedValue < 0 )
            {
                beforeFirst();

                return;
            }

            // Element is in the valid range as specified by assertion
            userIdxCursor.afterValue( id, value );
            setAvailable( false );
        }
        else
        {
            super.afterValue( id, value );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<V, ID> element ) throws Exception
    {
        checkNotClosed( "before()" );

        if ( userIdxCursor != null )
        {
            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the node.  If it is smaller or
             * equal to this lower bound then we simply position the
             * userIdxCursor before the first element.  Otherwise we let the
             * underlying userIdx Cursor position the element.
             */
            if ( greaterEqEvaluator.getComparator().compare( element.getKey(),
                greaterEqEvaluator.getExpression().getValue().getValue() ) <= 0 )
            {
                beforeFirst();
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
    public void after( IndexEntry<V, ID> element ) throws Exception
    {
        checkNotClosed( "after()" );

        if ( userIdxCursor != null )
        {
            int comparedValue = greaterEqEvaluator.getComparator().compare( element.getKey(),
                greaterEqEvaluator.getExpression().getValue().getValue() );

            /*
             * First we need to check and make sure this element is within
             * bounds as mandated by the assertion node.  To do so we compare
             * it's value with the value of the node.  If it is equal to this
             * lower bound then we simply position the userIdxCursor after
             * this first node.  If it is less than this value then we
             * position the Cursor before the first entry.
             */
            if ( comparedValue == 0 )
            {
                userIdxCursor.after( element );
                setAvailable( false );

                return;
            }

            if ( comparedValue < 0 )
            {
                beforeFirst();

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
    @SuppressWarnings("unchecked")
    public void beforeFirst() throws Exception
    {
        checkNotClosed( "beforeFirst()" );

        if ( userIdxCursor != null )
        {
            IndexEntry<V, ID> advanceTo = new ForwardIndexEntry<V, ID>();
            advanceTo.setKey( ( V ) greaterEqEvaluator.getExpression().getValue().getValue() );
            userIdxCursor.before( advanceTo );
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
    public void afterLast() throws Exception
    {
        checkNotClosed( "afterLast()" );

        if ( userIdxCursor != null )
        {
            userIdxCursor.afterLast();
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
    public boolean first() throws Exception
    {
        beforeFirst();

        return next();
    }


    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        afterLast();

        return previous();
    }


    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        checkNotClosed( "previous()" );

        if ( userIdxCursor != null )
        {
            /*
             * We have to check and make sure the previous value complies by
             * being greater than or eq to the expression node's value
             */
            while ( userIdxCursor.previous() )
            {
                checkNotClosed( "previous()" );
                IndexEntry<?, ID> candidate = userIdxCursor.get();

                if ( greaterEqEvaluator.getComparator().compare( candidate.getKey(),
                    greaterEqEvaluator.getExpression().getValue().getValue() ) >= 0 )
                {
                    return setAvailable( true );
                }
            }

            return setAvailable( false );
        }

        while ( uuidIdxCursor.previous() )
        {
            checkNotClosed( "previous()" );
            uuidCandidate = uuidIdxCursor.get();

            if ( greaterEqEvaluator.evaluate( uuidCandidate ) )
            {
                return setAvailable( true );
            }
        }

        return setAvailable( false );
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        checkNotClosed( "next()" );

        if ( userIdxCursor != null )
        {
            /*
             * No need to do the same check that is done in previous() since
             * values are increasing with calls to next().
             */
            return setAvailable( userIdxCursor.next() );
        }

        while ( uuidIdxCursor.next() )
        {
            checkNotClosed( "next()" );
            uuidCandidate = uuidIdxCursor.get();

            if ( greaterEqEvaluator.evaluate( uuidCandidate ) )
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
    public IndexEntry<V, ID> get() throws Exception
    {
        checkNotClosed( "get()" );

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
            return ( IndexEntry<V, ID> ) uuidCandidate;
        }

        throw new InvalidCursorPositionException( I18n.err( I18n.ERR_708 ) );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        LOG_CURSOR.debug( "Closing GreaterEqCursor {}", this );
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
    public void close( Exception cause ) throws Exception
    {
        LOG_CURSOR.debug( "Closing GreaterEqCursor {}", this );
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
}