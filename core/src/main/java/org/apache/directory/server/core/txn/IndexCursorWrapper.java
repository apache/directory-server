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

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.server.core.api.partition.index.AbstractIndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.IndexEntry;
import org.apache.directory.server.core.api.partition.index.IndexComparator;
import org.apache.directory.server.i18n.I18n;

import org.apache.directory.shared.ldap.model.cursor.InvalidCursorPositionException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.entry.Entry;;

public class IndexCursorWrapper<ID> extends AbstractIndexCursor<Object, Entry, ID>
{
    /** Cursors to merge */
    private ArrayList<IndexCursor<Object, Entry, ID>> cursors;
    
    /** list of values available per cursor */
    private ArrayList<IndexEntry<Object,ID>> values;
    
    /** index get should get the value from */
    private int getIndex = -1;
    
    /** Dn of the partition */
    private Dn partitionDn;
    
    /** Index attribute oid */
    private String attributeOid;
    
    /** whether this is a cursor on forward or reverse index */
    private boolean forwardIndex;
    
    /** List of txns that this cursor depends on */
    private ArrayList<ReadWriteTxn<ID>> txns;
    
    /** True if cursor is positioned */
    private boolean positioned;
    
    /** direction of the move */
    boolean movingNext = true;
    
    /** Comparator used to order the index entries */
    private IndexComparator<Object,ID> comparator;
    
    /** unsupported operation message */
    private static final String UNSUPPORTED_MSG = I18n.err( I18n.ERR_722 );
    
    
    public IndexCursorWrapper( Dn partitionDn, IndexCursor<Object, Entry, ID> wrappedCursor, IndexComparator<Object,ID> comparator, String attributeOid, boolean forwardIndex, Object onlyValueKey, ID onlyIDKey )
    {
        
        this.partitionDn = partitionDn;
        this.forwardIndex = forwardIndex;
        this.attributeOid = attributeOid;
        this.comparator = comparator;
        
        
        TxnManagerInternal<ID> txnManager = TxnManagerFactory.<ID>txnManagerInternalInstance();      
        Transaction<ID> curTxn = txnManager.getCurTxn();  
        List<ReadWriteTxn<ID>> toCheck = curTxn.getTxnsToCheck(); 
        
        cursors = new ArrayList<IndexCursor<Object, Entry, ID>>( toCheck.size() + 1 );
        cursors.add( ( IndexCursor<Object, Entry, ID> )wrappedCursor );
        
        if ( toCheck.size() > 0 )
        {
            txns = new ArrayList<ReadWriteTxn<ID>>( toCheck.size() );
            
            ReadWriteTxn<ID> dependentTxn;
            
            for ( int idx = 0; idx < toCheck.size(); idx++ )
            {
                dependentTxn = toCheck.get( idx );
                
                if ( dependentTxn.hasDeletesFor( partitionDn, attributeOid ) )
                {
                    txns.add( dependentTxn );
                }
                else
                {
                    txns.add( null );
                }
                
                cursors.add( dependentTxn.getCursorFor( partitionDn, attributeOid, forwardIndex, onlyValueKey, onlyIDKey, comparator ) );
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void after( IndexEntry<Object, ID> element ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "after()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
            {
             cursor.after( element );
            }
        }
        
        getIndex = -1;
    }
    
    /**
     * {@inheritDoc}
     */
    public void before( IndexEntry<Object, ID> element ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "before()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
            {
                cursor.before( element );
            }
        }
        
        getIndex = -1;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void afterValue( ID id, Object value ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "afterValue()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
            {
                cursor.afterValue( id, value );
            }
        }
        
        getIndex = -1;
    }


    /**
     * {@inheritDoc}
     */
    public void beforeValue( ID id, Object value ) throws Exception
    {
        int idx;
        positioned = true;
        movingNext = true;
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "beforeValue()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
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
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "beforeFirst()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
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
        IndexCursor<Object,Entry,ID> cursor;
        
        checkNotClosed( "afterLast()" );
        
        for ( idx = 0; idx < values.size(); idx++ )
        {
            values.set( idx, null );
            cursor = cursors.get( idx );
            if( cursor != null )
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
        this.beforeFirst();
        return this.next();
    }

    /**
     * {@inheritDoc}
     */
    public boolean last() throws Exception
    {
        this.afterLast();
        return this.previous();
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean next() throws Exception
    {
        IndexCursor<Object,Entry,ID> cursor;
        IndexEntry<Object,ID> minValue;
        IndexEntry<Object,ID> value;
        
        checkNotClosed( "next()" );
        
        IndexEntry<Object,ID> lastValue = null;
        if ( getIndex >= 0 )
        {
            lastValue = values.get( getIndex );
        }
        
        int idx;
        if ( positioned == false )
        {
            afterLast();
        }
        
        if ( movingNext == false || ( getIndex < 0 ) )
        {
            minValue = null;
            getIndex = -1;
            for ( idx = 0; idx < values.size(); idx++ )
            {
                cursor = cursors.get( idx );
                if ( cursor != null && cursor.next() )
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
           this.recomputeMinimum();
        }
        
        int txnIdx;
        ReadWriteTxn<ID> curTxn;
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
            for ( ; txnIdx < txns.size(); txnIdx++ )
            {
                curTxn = txns.get( txnIdx );
                
                // TODO check for index entry delete here
                if ( curTxn!= null)
                {
                    valueDeleted = true;
                    break;
                }
            }
            
            if ( valueDeleted == false && ( lastValue == null || ( comparator.compare( value, lastValue ) > 0 ) ) )
            {
                break;
            }
            
            // Recompute minimum
            this.recomputeMinimum();
            
        } while ( true );
        
        return ( getIndex >= 0 );

    } 
    
    
    /**
     * {@inheritDoc}
     */
    public boolean previous() throws Exception
    {
        IndexCursor<Object,Entry,ID> cursor;
        IndexEntry<Object,ID> maxValue;
        IndexEntry<Object,ID> value;
        
        checkNotClosed( "previous()" );
        
        IndexEntry<Object,ID> lastValue = null;
        if ( getIndex >= 0 )
        {
            lastValue = values.get( getIndex );
        }
        
        int idx;
        if ( positioned == false )
        {
            afterLast();
        }
        
        if ( movingNext == false || ( getIndex < 0 ) )
        {
            maxValue = null;
            getIndex = -1;
            for ( idx = 0; idx < values.size(); idx++ )
            {
                cursor = cursors.get( idx );
                if ( cursor != null && cursor.next() )
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
           this.recomputeMaximum();
        }
        
        int txnIdx;
        ReadWriteTxn<ID> curTxn;
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
            for ( ; txnIdx < txns.size(); txnIdx++ )
            {
                curTxn = txns.get( txnIdx );
                
                // TODO check for index entry delete here
                if ( curTxn!= null)
                {
                    valueDeleted = true;
                    break;
                }
            }
            
            if ( valueDeleted == false && ( lastValue == null || ( comparator.compare( value, lastValue ) < 0 ) ) )
            {
                break;
            }
            
            // Recompute maximum
            this.recomputeMaximum();
            
        } while ( true );
        
        return ( getIndex >= 0 );

    }
    
    
    
    /**
     * {@inheritDoc}
     */
    public IndexEntry<Object, ID> get() throws Exception
    {
        checkNotClosed( "get()" );
        
        if ( getIndex >= 0 )
        {
            IndexEntry<Object,ID> value = values.get( getIndex );
            
            if ( value == null )
            {
                throw new IllegalStateException( "getIndex points to a null value" );
            }
            
            return value;
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
        
        IndexCursor<Object,Entry,ID> cursor;
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
        
        IndexCursor<Object,Entry,ID> cursor;
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

    
    private void recomputeMinimum() throws Exception
    {
        IndexCursor<Object,Entry,ID> cursor;
        IndexEntry<Object,ID> minValue;
        IndexEntry<Object,ID> value;
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
    
    private void recomputeMaximum() throws Exception
    {
        IndexCursor<Object,Entry,ID> cursor;
        IndexEntry<Object,ID> maxValue;
        IndexEntry<Object,ID> value;
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

}
