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
package org.apache.directory.server.core.shared.txn;


import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.UUID;


/**
 * Provides a cursor over the index entries added by a transaction
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnIndexCursor extends AbstractIndexCursor<Object>
{
    /** list of changed index entries */
    private NavigableSet<IndexEntry<Object>> changedEntries;

    /** forward or reverse index */
    private boolean forwardIndex;

    /** whether cursor is explicitly positioned */
    private boolean positioned;

    /** whether the moving direction is next */
    private boolean movingNext = true;

    /** Iterator to move over the set */
    private Iterator<IndexEntry<Object>> it;

    /** currently available value */
    private IndexEntry<Object> availableValue;

    /** unsupported operation message */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );

    /** true if the index is locked down for a key */
    private boolean onlyKey;

    /** True if past the only key */
    private boolean pastOnlyKey;

    /** Lock down key in case of forward index */
    private Object onlyValueKey;

    /** Lock down key in case of reverse index */
    private UUID onlyIDKey;

    /** index entry comparator */
    private IndexComparator<Object> comparator;


    public TxnIndexCursor( NavigableSet<IndexEntry<Object>> changedEntries, boolean forwardIndex,
        Object onlyValueKey, UUID onlyIDKey, IndexComparator<Object> comparator )
    {
        this.changedEntries = changedEntries;
        this.forwardIndex = forwardIndex;
        this.comparator = comparator;

        if ( onlyValueKey != null )
        {
            this.onlyValueKey = onlyValueKey;
            onlyKey = true;
        }

        if ( changedEntries.size() < 1 )
        {
            throw new IllegalArgumentException( "TxnIndexCursor should not be constructed with no index  changes" );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<Object> element ) throws Exception
    {
        availableValue = null;

        // If the cursor is locked down by a key, check if the key is equal to the only key we have.
        if ( onlyKey )
        {
            if ( forwardIndex )
            {
                if ( comparator.getValueComparator().compare( element.getValue(), onlyValueKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
            else
            {
                if ( UUIDComparator.INSTANCE.compare( element.getId(), onlyIDKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
        }

        positioned = true;
        movingNext = true;
        pastOnlyKey = false;

        /*
         * If (key, null) is given as the element to position after, then skip all 
         * the index elements with the given key.
         */
        boolean skipKey = false;

        if ( forwardIndex )
        {
            if ( element.getId() == null )
            {
                skipKey = true;
            }
        }
        else
        {
            if ( element.getValue() == null )
            {
                skipKey = true;
            }
        }

        if ( skipKey )
        {
            /*
             *  After this call, the iterator will be position on the first value for the given key if a value for the key exists. Otherwise it
             *  is positioned at the key past the given key.
             */
            it = changedEntries.tailSet( element, false ).iterator();

            boolean useLastEntry = false;
            IndexEntry<Object> indexEntry = null;

            while ( it.hasNext() )
            {
                indexEntry = it.next();

                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( indexEntry.getValue(), element.getValue() ) != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
                else
                {
                    if ( UUIDComparator.INSTANCE.compare( indexEntry.getId(), element.getId() ) != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
            }

            if ( useLastEntry )
            {
                // Position the iterator on the first element past the given key.
                it = changedEntries.tailSet( indexEntry, true ).iterator();
            }
            else
            {
                // There is no more elements after the given key, change the iterator direction
                movingNext = false;
                it = changedEntries.descendingIterator();
            }
        }
        else
        {
            // Simply position the iterator past the given element.
            it = changedEntries.tailSet( element, false ).iterator();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<Object> element ) throws Exception
    {
        availableValue = null;

        // If the cursor is locked down by a key, check if the key is equal to the only key we have.
        if ( onlyKey )
        {
            if ( forwardIndex )
            {
                if ( comparator.getValueComparator().compare( element.getValue(), onlyValueKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
            else
            {
                if ( UUIDComparator.INSTANCE.compare( element.getId(), onlyIDKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
        }

        positioned = true;
        movingNext = true;
        pastOnlyKey = false;

        /*
         * Position the iterator on the given element. A call to next will return this element if it exists. If 
         * (key,null) is supplied as the element, then this will position the iterator on the first value for the
         * given key if any value for the given key exists.
         */
        it = changedEntries.tailSet( element, true ).iterator();
    }


    /**
     * {@inheritDoc}
     */
    public void afterValue( UUID id, Object value ) throws Exception
    {
        ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( value );
        after( indexEntry );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( UUID id, Object value ) throws Exception
    {
        ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( value );
        before( indexEntry );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        pastOnlyKey = false;

        if ( onlyKey )
        {
            // If locked down by a key, position the iterator on the first value for the given key. 
            ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();

            if ( forwardIndex )
            {
                indexEntry.setValue( onlyValueKey );
            }
            else
            {
                indexEntry.setId( onlyIDKey );
            }

            it = changedEntries.tailSet( indexEntry, false ).iterator();
        }
        else
        {
            it = changedEntries.iterator();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = false;
        pastOnlyKey = false;

        if ( onlyKey )
        {

            /*
             * If we are locked down by only key, then position the iterator right past the key.
             */

            IndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();

            if ( forwardIndex )
            {
                indexEntry.setValue( onlyValueKey );
            }
            else
            {
                indexEntry.setId( onlyIDKey );
            }

            it = changedEntries.tailSet( indexEntry, false ).iterator();

            boolean useLastEntry = false;

            while ( it.hasNext() )
            {
                indexEntry = it.next();

                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( indexEntry.getValue(), onlyValueKey ) != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
                else
                {
                    if ( UUIDComparator.INSTANCE.compare( indexEntry.getId(), onlyIDKey ) != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
            }

            if ( useLastEntry )
            {
                it = changedEntries.headSet( indexEntry, false ).descendingIterator();
            }
            else
            {
                it = changedEntries.descendingIterator();
            }
        }
        else
        {
            it = changedEntries.descendingIterator();
        }
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
        if ( positioned == false )
        {
            afterLast();
        }

        // If currently moving in the next() direction, then get a descending iterator using the last availableValue
        if ( movingNext == true )
        {
            if ( availableValue == null )
            {
                if ( it.hasNext() )
                {
                    availableValue = it.next();
                }
            }

            if ( availableValue == null )
            {
                it = changedEntries.descendingIterator();
            }
            else
            {
                it = changedEntries.headSet( availableValue, false ).descendingIterator();
            }

            availableValue = null;
            movingNext = false;
            pastOnlyKey = false;
        }

        if ( pastOnlyKey )
        {
            return false;
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();

            /*
             * If only key and past the only key, do not make the available value null. 
             * Repeated calls the previous will not advance iterator either. If the user
             * calls next after this point, the available value will be used to
             * reverse the iterator.
             */
            if ( onlyKey )
            {
                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( availableValue.getValue(), onlyValueKey ) != 0 )
                    {
                        pastOnlyKey = true;

                        return false;
                    }
                }
                else
                {
                    if ( UUIDComparator.INSTANCE.compare( availableValue.getId(), onlyIDKey ) != 0 )
                    {
                        pastOnlyKey = true;

                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            availableValue = null;

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        if ( positioned == false )
        {
            beforeFirst();
        }

        // If currently moving in the previous() direction, then get a increasing iterator using the last availableValue
        if ( movingNext == false )
        {
            if ( availableValue == null )
            {
                if ( it.hasNext() )
                {
                    availableValue = it.next();
                }
            }

            if ( availableValue == null )
            {
                it = changedEntries.iterator();
            }
            else
            {
                it = changedEntries.tailSet( availableValue, false ).iterator();
            }

            availableValue = null;
            movingNext = true;
            pastOnlyKey = false;
        }

        if ( pastOnlyKey )
        {
            return false;
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();

            /*
             * If only key and past the only key, do not make the available value null. 
             * Repeated calls the next will not advance iterator either. If the user
             * calls previous after this point, the available value will be used to
             * reverse the iterator.
             */
            if ( onlyKey )
            {
                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( availableValue.getValue(), onlyValueKey ) != 0 )
                    {
                        pastOnlyKey = true;

                        return false;
                    }
                }
                else
                {
                    if ( UUIDComparator.INSTANCE.compare( availableValue.getId(), onlyIDKey ) != 0 )
                    {
                        pastOnlyKey = true;

                        return false;
                    }
                }
            }

            return true;
        }
        else
        {
            availableValue = null;

            return false;
        }
    }


    /**
     * {@inheritDoc}
     */
    public IndexEntry<Object> get() throws Exception
    {
        if ( availableValue != null && !pastOnlyKey )
        {
            return availableValue;
        }

        throw new InvalidCursorPositionException();
    }


    /**
     * {@inheritDoc}
     */
    protected String getUnsupportedMessage()
    {
        return UNSUPPORTED_MSG;
    }
}
