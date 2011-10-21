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
package org.apache.directory.server.core.txn;

import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.IndexEntry;

import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.entry.Entry;

import java.util.Iterator;
import java.util.NavigableSet;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class TxnIndexCursor <ID> extends AbstractIndexCursor<Object, Entry, ID>
{
    /** list of changed index entries */
    private NavigableSet<IndexEntry<Object,ID>> changedEntries;
    
    /** forward or reverse index */
    private boolean forwardIndex;
    
    /** whether cursor is explicitly positioned */
    private boolean positioned;
    
    /** whether the moving direction is next */
    private boolean movingNext = true;
    
    /** Iterator to move over the set */
    private Iterator<IndexEntry<Object,ID>> it;
    
    /** currently available value */
    private IndexEntry<Object,ID> availableValue;
    
    /** unsupported operation message */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
    
    /** true if the index is locked down for a key */
    private boolean onlyKey;
    
    /** Lock down key in case of forward index */
    private Object onlyValueKey;
    
    /** Lock down key in case of reverse index */
    private ID onlyIDKey;
    
    /** index entry comparator */
    private IndexComparator<Object,ID> comparator;
   
    
    public TxnIndexCursor( NavigableSet<IndexEntry<Object,ID>> changedEntries, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey, IndexComparator<Object,ID> comparator )
    {
        this.changedEntries = changedEntries;
        this.forwardIndex = forwardIndex;
        this.comparator = comparator;
        
        if ( onlyValueKey != null )
        {
            this.onlyValueKey = onlyValueKey;
            onlyKey = true;
        }
        
        if ( changedEntries.size()  < 1 )
        {
            throw new IllegalArgumentException("TxnIndexCursor should not be constructed with no index  changes");
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<Object, ID> element ) throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        
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
                if ( comparator.getIDComparator().compare( element.getId(), onlyIDKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
        }
        
        boolean skipKey = false;
        
        if  ( forwardIndex )
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
            it = changedEntries.tailSet( element, false ).iterator();
            
            boolean useLastEntry = false;
            IndexEntry<Object,ID> indexEntry = null;
            
            while ( it.hasNext() )
            {
                indexEntry = it.next();
                
                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( indexEntry.getValue(), element.getValue() )  != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
                else
                {
                    if ( comparator.getIDComparator().compare( indexEntry.getId(), element.getId() )  != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
            }
            
            if ( useLastEntry )
            {
                it = changedEntries.tailSet( indexEntry, true ).descendingIterator();
            }
            else
            {
                movingNext = false;
                it = changedEntries.descendingIterator();
            }
        }
        else
        {
            it = changedEntries.tailSet( element, false ).iterator();
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<Object, ID> element ) throws Exception
    {
        positioned = true;
        availableValue = null;
        movingNext = true;
        
        if ( onlyKey )
        {
            if ( forwardIndex )
            {
                if ( comparator.getValueComparator().compare(element.getValue(), onlyValueKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
            else
            {
                if ( comparator.getIDComparator().compare( element.getId(), onlyIDKey ) != 0 )
                {
                    throw new UnsupportedOperationException( I18n.err( I18n.ERR_446 ) );
                }
            }
        }
        
        it = changedEntries.tailSet( element, true ).iterator();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterValue( ID id, Object value ) throws Exception
    {
        ForwardIndexEntry<Object,ID> indexEntry = new ForwardIndexEntry<Object,ID>();
        indexEntry.setId( id );
        indexEntry.setValue( value );
        after( indexEntry );
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( ID id, Object value ) throws Exception
    {
        ForwardIndexEntry<Object,ID> indexEntry = new ForwardIndexEntry<Object,ID>();
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
        
        if ( onlyKey )
        {
            ForwardIndexEntry<Object,ID> indexEntry = new ForwardIndexEntry<Object,ID>();
            
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
        
        if ( onlyKey )
        {
            IndexEntry<Object,ID> indexEntry = new ForwardIndexEntry<Object,ID>();
            
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
                    if ( comparator.getValueComparator().compare( indexEntry.getValue(), onlyValueKey )  != 0 )
                    {
                        useLastEntry = true;
                        break;
                    }
                }
                else
                {
                    if ( comparator.getIDComparator().compare( indexEntry.getId(), onlyIDKey )  != 0 )
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
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();
            
            if ( onlyKey )
            {
                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( availableValue.getValue(), onlyValueKey ) != 0 )
                    {
                        availableValue = null;
                        
                        return false;
                    }
                }
                else
                {
                    if ( comparator.getIDComparator().compare( availableValue.getId(), onlyIDKey ) != 0 )
                    {
                        availableValue = null;
                        
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
            afterLast();
        }
        
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
                it = changedEntries.tailSet( availableValue, false ).descendingIterator();
            }
            
            availableValue = null;
            movingNext = true;
        }

        if ( it.hasNext() )
        {
            availableValue = it.next();
            

            if ( onlyKey )
            {
                if ( forwardIndex )
                {
                    if ( comparator.getValueComparator().compare( availableValue.getValue(), onlyValueKey ) != 0 )
                    {
                        availableValue = null;
                        
                        return false;
                    }
                }
                else
                {
                    if ( comparator.getIDComparator().compare( availableValue.getId(), onlyIDKey ) != 0 )
                    {
                        availableValue = null;
                        
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
    public IndexEntry<Object, ID> get() throws Exception
    {
        if ( availableValue != null )
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
