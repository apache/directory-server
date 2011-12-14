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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.server.i18n.I18n;

import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.entry.Entry;;

/**
 * Wraps an index's cursor and provides a transactionally consistent view.
 * 
 * Each transaction that this txn depends exposes a TxnIndexCursor if it
 * has adds for the wrapped cursor's index. This cursor wraps them as well. 
 * Whenever the cursor is positioned, all wrapped cursors are positioned and
 * available values for each cursor is reset to null. Whenever a next or 
 * previous call is made:
 *  *If the call is after a positioning call, we do a next or prev on all wrapped
 *  cursors
 *  * Otherwise we do a next or prev on the last cursor we go the value from.
 *  
 *  After the above step, we recompute the minimum. The new index is the value 
 *  we will get the value from.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexCursorWrapper extends AbstractIndexCursor<Object>
{
    /** Cursors to merge */
    private ArrayList<IndexCursor<Object>> cursors;
    
    /** list of values available per cursor */
    private ArrayList<IndexEntry<Object>> values;
    
    /** index get should get the value from */
    private int getIndex = -1;
    
    /** Dn of the partition */
    private Dn partitionDn;
    
    /** Index attribute oid */
    private String attributeOid;
    
    /** whether this is a cursor on forward or reverse index */
    private boolean forwardIndex;
    
    /** Index deletes by txns that this cursor depends on */
    private ArrayList<NavigableSet<IndexEntry<Object>>> deletes;
    
    /** True if cursor is positioned */
    private boolean positioned;
    
    /** direction of the move */
    private boolean movingNext = true;
    
    /** Comparator used to order the index entries */
    private IndexComparator<Object> comparator;
    
    /** unsupported operation message */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
    
    /** Last value returned: here to keep memory overhead low */
    private ForwardIndexEntry<Object> lastValue = new ForwardIndexEntry<Object>();
    
    /** Txn Manager */
    TxnManagerInternal txnManager;
     
    public IndexCursorWrapper( TxnManagerFactory txnManagerFactory, Dn partitionDn, 
            IndexCursor<Object> wrappedCursor, IndexComparator<Object> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey, UUID onlyIDKey )
    {
        this.partitionDn = partitionDn;
        this.forwardIndex = forwardIndex;
        this.attributeOid = attributeOid;
        this.comparator = comparator;
        
        txnManager = txnManagerFactory.txnManagerInternalInstance();      
        Transaction curTxn = txnManager.getCurTxn();  
        List<ReadWriteTxn> toCheck = curTxn.getTxnsToCheck(); 
        
        cursors = new ArrayList<IndexCursor<Object>>( toCheck.size() + 1 );
        values = new ArrayList<IndexEntry<Object>>( toCheck.size() + 1 );
        cursors.add( ( IndexCursor<Object> )wrappedCursor );
        values.add( null );
        
        if ( toCheck.size() > 0 )
        {
            deletes = new ArrayList<NavigableSet<IndexEntry<Object>>>( toCheck.size() );
            
            ReadWriteTxn dependentTxn;
            
            for ( int idx = 0; idx < toCheck.size(); idx++ )
            {
                dependentTxn = toCheck.get( idx );
                
                if ( curTxn == dependentTxn )
                {
                    NavigableSet<IndexEntry<Object>> txnDeletes = dependentTxn.getDeletesFor( partitionDn, attributeOid );
                    
                    if ( txnDeletes != null )
                    {
                        TreeSet<IndexEntry<Object>> clonedDeletes = new TreeSet<IndexEntry<Object>>( comparator );
                        clonedDeletes.addAll( txnDeletes );
                        deletes.add( clonedDeletes );
                    }
                    else
                    {
                        deletes.add( null );
                    }
                }
                else
                {
                    deletes.add( dependentTxn.getDeletesFor( partitionDn, attributeOid ) );
                }
                
                values.add( null );
                
                // This adds a null to the array if the txn does not have any changes for the index
                cursors.add( getCursorFor( dependentTxn, partitionDn, attributeOid, forwardIndex, onlyValueKey, onlyIDKey, comparator ) );
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<Object> element ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "after()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.after( element );
            }
        }
        
        getIndex = -1;
    }
    
    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<Object> element ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "before()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.before( element );
            }
        }
        
        getIndex = -1;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterValue( UUID id, Object value ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "afterValue()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.afterValue( id, value );
            }
        }
        
        getIndex = -1;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( UUID id, Object value ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "beforeValue()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.beforeValue( id, value );
            }
        }
        
        getIndex = -1;
    }
   
    
    /**
     * {@inheritDoc}
     */
    public void beforeFirst() throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "beforeFirst()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.beforeFirst();
            }
        }
        
        getIndex = -1;
    }


    /**
     * {@inheritDoc}
     */
    public void afterLast() throws Exception
    {
        int idx;
        positioned = true;
        movingNext = false;
        IndexCursor<Object> cursor;
        
        checkNotClosed( "afterLast()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.afterLast( );
            }
            
        }
        
        getIndex = -1;
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
    public boolean next() throws Exception
    {
        IndexCursor<Object> cursor;
        IndexEntry<Object> minValue;
        IndexEntry<Object> value;
        
        checkNotClosed( "next()" );
        
        
        if ( getIndex >= 0 )
        {
            IndexEntry<Object> indexEntry = values.get( getIndex );
            lastValue.setId( indexEntry.getId() );
            lastValue.setValue( indexEntry.getValue() );
        }
        else
        {
            lastValue.setId( null );
        }
        
        int idx;
        
        if ( positioned == false )
        {
            beforeFirst();
        }
        
        /*
         *  If called right after positioning the cursor or changing direction, then do a next call
         *  on every wrapped cursor and recompute the min value.
         */
        if ((  movingNext == false ) || ( getIndex < 0 ) )
        {
            minValue = null;
            getIndex = -1;
            movingNext = true;
            
            for ( idx = 0; idx < values.size(); idx++ )
            {
                cursor = cursors.get( idx );
                
                if ( ( cursor != null ) && cursor.next() )
                {
                    value = cursor.get();
                    
                    if ( ( getIndex < 0 ) || ( comparator.compare( value, minValue ) < 0 ) )
                    {
                        minValue = value;
                        getIndex = idx;
                    }
                    
                    values.set( idx, value );
                }
                else
                {
                    values.set( idx, null );
                }
            }
        }
        else
        {
            // Move the last cursor we did a get from and recompute minimum
           recomputeMinimum();
        }
        
        int txnIdx;
        NavigableSet<IndexEntry<Object>> txnDeletes;
        boolean valueDeleted;
        
        do
        {
            if ( getIndex < 0 )
            {
                break;
            }
            
            value = values.get( getIndex );
            
            txnIdx = getIndex;
            
            if ( txnIdx > 0 )
            {
                txnIdx--;
            }
            
            valueDeleted = false;
            
            for ( ; txnIdx < deletes.size(); txnIdx++ )
            {
                txnDeletes = deletes.get( txnIdx );
                
                if ( ( txnDeletes != null ) && ( txnDeletes.contains( value ) ) )
                {
                    valueDeleted = true;
                    break;
                }
            }
            
            // If the value we get is not deleted and greater than the last value we returned, then we are done
            if ( ( valueDeleted == false ) && ( ( lastValue.getId() == null ) || ( comparator.compare( value, lastValue ) > 0 ) ) )
            {
                break;
            }
            
            // Recompute minimum
            recomputeMinimum();
            
        } while ( true );
        
        return ( getIndex >= 0 );
    } 
    
    
    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        IndexCursor<Object> cursor;
        IndexEntry<Object> maxValue;
        IndexEntry<Object> value;
        
        checkNotClosed( "previous()" );
        
        if ( getIndex >= 0 )
        {
            IndexEntry<Object> indexEntry = values.get( getIndex );
            lastValue.setId( indexEntry.getId() );
            lastValue.setValue( indexEntry.getValue() );
        }
        else
        {
            lastValue.setId( null );
        }
        
        int idx;
        
        if ( positioned == false )
        {
            afterLast();
        }
        
        
        /*
         *  If called right after positioning the cursor or changing direction, then do a previous call
         *  on every wrapped cursor and recompute the max value.
         */
        if ( ( movingNext == true ) || ( getIndex < 0 ) )
        {
            maxValue = null;
            getIndex = -1;
            movingNext = false;
            
            for ( idx = 0; idx < values.size(); idx++ )
            {
                cursor = cursors.get( idx );
                
                if ( ( cursor != null ) && cursor.previous() )
                {
                    value = cursor.get();
                    
                    if ( ( getIndex < 0 ) || ( comparator.compare( value, maxValue ) > 0 ) )
                    {
                        maxValue = value;
                        getIndex = idx;
                    }
                    
                    values.set( idx, value );
                }
                else
                {
                    values.set( idx, null );
                }
            }
        }
        else
        {
            // Move the last cursor we did a get from and recompute maximum
           recomputeMaximum();
        }
        
        int txnIdx;
        NavigableSet<IndexEntry<Object>> txnDeletes;
        boolean valueDeleted;
        
        do
        {
            if ( getIndex < 0 )
            {
                break;
            }
            
            value = values.get( getIndex );
            
            txnIdx = getIndex;
            
            if ( txnIdx > 0 )
            {
                txnIdx--;
            }
            
            valueDeleted = false;
            
            for ( ; txnIdx < deletes.size(); txnIdx++ )
            {
                txnDeletes = deletes.get( txnIdx );
                
                if ( txnDeletes!= null && txnDeletes.contains( value ) )
                {
                    valueDeleted = true;
                    break;
                }
            }
            
            // If the value we get is not deleted and less than the last value we returned, then we are done
            if ( ( valueDeleted == false ) && ( ( lastValue.getId() == null ) || ( comparator.compare( value, lastValue ) < 0 ) ) )
            {
                break;
            }
            
            // Recompute maximum
            recomputeMaximum();
            
        } while ( true );
        
        return ( getIndex >= 0 );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public IndexEntry<Object> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( getIndex >= 0 )
        {
            IndexEntry<Object> value = values.get( getIndex );
            
            if ( value == null )
            {
                throw new IllegalStateException( "getIndex points to a null value" );
            }
            
            /*
             * TODO fixme:
             * Upper layers might change the index entry we return. To work around this.
             * we create a new idex entry here. This code should be removed when
             * search engine is changed to avoid modifying index entries.
             */
            IndexEntry<Object> indexEntry;
            
            if ( forwardIndex )
            {
                indexEntry = new ForwardIndexEntry<Object>();
            }
            else
            {
                indexEntry = new ReverseIndexEntry<Object>();
            }
            
            indexEntry.setId( value.getId() );
            indexEntry.setValue( value.getValue() );
            
            return indexEntry;
        }

        throw new InvalidCursorPositionException();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception
    {
        super.close();
        
        IndexCursor<Object> cursor;
        int idx;
        
        for ( idx = 0; idx < cursors.size(); idx++ )
        {
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.close();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void close( Exception cause ) throws Exception
    {
        super.close( cause );
        
        IndexCursor<Object> cursor;
        int idx;
        
        for ( idx = 0; idx < cursors.size(); idx++ )
        {
            cursor = cursors.get( idx );
            
            if ( cursor != null )
            {
                cursor.close( cause );
            }
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
     * Do a get on the last cursor we got the value from and recompute the minimum
     *
     * @throws Exception
     */
    private void recomputeMinimum() throws Exception
    {
        IndexCursor<Object> cursor;
        IndexEntry<Object> minValue;
        IndexEntry<Object> value;
        int idx;
        
        cursor = cursors.get( getIndex );
        
        if ( cursor.next() )
        {
            values.set( getIndex , cursor.get() );
        }
        else
        {
            values.set( getIndex, null );
        }
        
        
        minValue = null;
        getIndex = -1;
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            value = values.get( idx );
            
            if ( value != null )
            {
                if ( ( getIndex < 0 ) || ( comparator.compare( value, minValue ) < 0 ) )
                {
                    minValue = value;
                    getIndex = idx;
                }
            }
        }
    }
    
    /**
     * Do a previous we got the value from and recompute the maximum.
     *
     * @throws Exception
     */
    private void recomputeMaximum() throws Exception
    {
        IndexCursor<Object> cursor;
        IndexEntry<Object> maxValue;
        IndexEntry<Object> value;
        int idx;
        
        cursor = cursors.get( getIndex );
        
        if ( cursor.previous() )
        {
            values.set( getIndex , cursor.get() );
        }
        else
        {
            values.set( getIndex, null );
        }
        
        
        maxValue = null;
        getIndex = -1;
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            value = values.get( idx );
            
            if ( value != null )
            {
                if ( ( getIndex < 0 ) || ( comparator.compare( value, maxValue ) > 0 ) )
                {
                    maxValue = value;
                    getIndex = idx;
                }
            }
        }
    }
    
    /**
     * Returns a cursor over the changes made by the given txn on the index identified by partitionDn+attributeOid. 
     *
     * @param txn for which the cursor will be built.
     * @param partitionDn dn of the partition
     * @param attributeOid oid of the indexed attribute
     * @param forwardIndex true if forward index and reverse if reverse index
     * @param onlyValueKey set if the cursor should be locked down by a key ( should be non null only for forward indices )
     * @param onlyIDKey  set if the cursor should be locked down by a key ( should be non null only for reverse indices )
     * @param comparator comparator that will be used to order index entries.
     * @return
     */
    private IndexCursor<Object> getCursorFor( ReadWriteTxn txn, Dn partitionDn, String attributeOid, boolean forwardIndex,
        Object onlyValueKey, UUID onlyIDKey, IndexComparator<Object> comparator )
    {
        NavigableSet<IndexEntry<Object>> changes; 
        TxnIndexCursor txnIndexCursor = null;

        if ( forwardIndex )
        {
            changes = txn.getForwardIndexChanges( partitionDn, attributeOid );
        }
        else
        {
            changes = txn.getReverseIndexChanges( partitionDn, attributeOid );
        }
        
        if ( changes == null || ( changes.size() == 0 ) )
        {
            return null;
        }
        
        Transaction curTxn = txnManager.getCurTxn();
        
        if ( txn == curTxn )
        {
            NavigableSet<IndexEntry<Object>> originalChanges = changes;
            changes = new TreeSet<IndexEntry<Object>>( comparator );
            changes.addAll( originalChanges );
        }
        
        return new TxnIndexCursor( changes, forwardIndex, onlyValueKey, onlyIDKey, comparator );
    }

}
