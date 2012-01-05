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


import java.net.URI;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.ForwardIndexEntry;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;
import org.apache.directory.server.core.api.partition.index.ReverseIndexEntry;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexWrapper implements Index<Object>
{
    /** wrapped index */
    Index<Object> wrappedIndex;

    /** partition the table belongs to */
    private Dn partitionDn;

    /** Txn log manager */
    private TxnLogManager txnLogManager;


    public IndexWrapper( TxnManagerFactory txnManagerFactory, Dn partitionDn, Index<Object> wrappedIndex )
    {
        this.partitionDn = partitionDn;
        this.wrappedIndex = wrappedIndex;
        txnLogManager = txnManagerFactory.txnLogManagerInstance();
    }


    /**
     * {@inheritDoc}
     */
    public String getAttributeId()
    {
        return wrappedIndex.getAttributeId();
    }


    /**
     * {@inheritDoc}
     */
    public void setAttributeId( String attributeId )
    {
        wrappedIndex.setAttributeId( attributeId );
    }


    /**
     * {@inheritDoc}
     */
    public int getCacheSize()
    {
        return wrappedIndex.getCacheSize();
    }


    /**
     * {@inheritDoc}
     */
    public void setCacheSize( int cacheSize )
    {
        wrappedIndex.setCacheSize( cacheSize );
    }


    /**
     * {@inheritDoc}
     */
    public void setWkDirPath( URI wkDirPath )
    {
        wrappedIndex.setWkDirPath( wkDirPath );
    }


    /**
     * {@inheritDoc}
     */
    public URI getWkDirPath()
    {
        return wrappedIndex.getWkDirPath();
    }


    /**
     * {@inheritDoc}
     */
    public AttributeType getAttribute()
    {
        return wrappedIndex.getAttribute();
    }


    /**
     * {@inheritDoc}
     */
    public Object getNormalized( Object attrVal ) throws Exception
    {
        return wrappedIndex.getNormalized( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return wrappedIndex.count();
    }


    /**
     * {@inheritDoc}
     */
    public int count( Object attrVal ) throws Exception
    {
        return wrappedIndex.count( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( Object attrVal ) throws Exception
    {
        return wrappedIndex.greaterThanCount( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public int lessThanCount( Object attrVal ) throws Exception
    {
        return wrappedIndex.lessThanCount( attrVal );
    }


    /**
     * {@inheritDoc}
     */
    public UUID forwardLookup( Object attrVal ) throws Exception
    {
        //        IndexCursor<Object> cursor = forwardCursor( attrVal );
        //
        //        try
        //        {
        //
        //            if ( cursor.next() )
        //            {
        //                return cursor.get().getId();
        //            }
        //
        //            return null;
        //        }
        //        finally
        //        {
        //            cursor.close();
        //        }
        // Find the UUID from the underlying index 
        UUID curId = wrappedIndex.forwardLookup( attrVal );

        curId = txnLogManager.mergeForwardLookup( partitionDn, wrappedIndex.getAttributeId(), attrVal, curId,
            wrappedIndex.getForwardIndexEntryComparator().getValueComparator() );

        return curId;
    }


    /**
     * {@inheritDoc}
     */
    public Object reverseLookup( UUID id ) throws Exception
    {
        //        IndexCursor<Object> cursor = reverseCursor( id );
        //        
        //        try
        //        {
        //            
        //            if ( cursor.next() )
        //            {
        //                return cursor.get().getValue();
        //            }
        //            
        //            return null;
        //        }
        //        finally
        //        {
        //            cursor.close();
        //        }

        Object curVal = wrappedIndex.reverseLookup( id );

        curVal = txnLogManager.mergeReversLookup( partitionDn, wrappedIndex.getAttribute().getOid(), id, curVal );

        return curVal;
    }


    /**
     * {@inheritDoc}
     */
    public void add( Object attrVal, UUID id ) throws Exception
    {
        wrappedIndex.add( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( UUID entryId ) throws Exception
    {
        wrappedIndex.drop( entryId );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( Object attrVal, UUID id ) throws Exception
    {
        wrappedIndex.drop( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object> reverseCursor() throws Exception
    {
        IndexCursor<Object> wrappedCursor;
        IndexCursor<Object> cursor = wrappedIndex.reverseCursor();

        try
        {
            wrappedCursor = ( IndexCursor<Object> ) txnLogManager.wrap( partitionDn, cursor,
                wrappedIndex.getReverseIndexEntryComparator(),
                wrappedIndex.getAttribute().getOid(), false, null, null );
        }
        catch ( Exception e )
        {
            cursor.close( e );
            throw e;
        }

        return wrappedCursor;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object> forwardCursor() throws Exception
    {
        IndexCursor<Object> wrappedCursor;
        IndexCursor<Object> cursor = wrappedIndex.forwardCursor();

        try
        {
            wrappedCursor = ( IndexCursor<Object> ) txnLogManager.wrap( partitionDn, cursor,
                wrappedIndex.getForwardIndexEntryComparator(),
                wrappedIndex.getAttribute().getOid(), true, null, null );
        }
        catch ( Exception e )
        {
            cursor.close( e );
            throw e;
        }

        return wrappedCursor;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object> reverseCursor( UUID id ) throws Exception
    {
        IndexCursor<Object> wrappedCursor;
        IndexCursor<Object> cursor = wrappedIndex.reverseCursor( id );

        try
        {
            wrappedCursor = ( IndexCursor<Object> ) txnLogManager.wrap( partitionDn, cursor,
                wrappedIndex.getReverseIndexEntryComparator(),
                wrappedIndex.getAttribute().getOid(), false, null, id );
        }
        catch ( Exception e )
        {
            cursor.close( e );
            throw e;
        }

        return wrappedCursor;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object> forwardCursor( Object key ) throws Exception
    {
        IndexCursor<Object> wrappedCursor;
        IndexCursor<Object> cursor = wrappedIndex.forwardCursor( key );

        try
        {
            wrappedCursor = ( IndexCursor<Object> ) txnLogManager.wrap( partitionDn, cursor,
                wrappedIndex.getForwardIndexEntryComparator(),
                wrappedIndex.getAttribute().getOid(), true, key, null );
        }
        catch ( Exception e )
        {
            cursor.close( e );
            throw e;
        }

        return wrappedCursor;
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Object> reverseValueCursor( UUID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<UUID> forwardValueCursor( Object key ) throws Exception
    {
        throw new UnsupportedOperationException();
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( Object attrVal ) throws Exception
    {
        IndexCursor<Object> cursor = forwardCursor( attrVal );

        try
        {
            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forward( Object attrVal, UUID id ) throws Exception
    {
        boolean currentlyExists = wrappedIndex.forward( attrVal, id );

        ForwardIndexEntry<Object> indexEntry = new ForwardIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( attrVal );
        return txnLogManager.mergeExistence( partitionDn, wrappedIndex.getAttribute().getOid(), indexEntry,
            currentlyExists );
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( UUID id ) throws Exception
    {
        IndexCursor<Object> cursor = reverseCursor( id );

        try
        {
            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverse( UUID id, Object attrVal ) throws Exception
    {
        boolean currentlyExists = wrappedIndex.reverse( id, attrVal );

        ReverseIndexEntry<Object> indexEntry = new ReverseIndexEntry<Object>();
        indexEntry.setId( id );
        indexEntry.setValue( attrVal );
        return txnLogManager.mergeExistence( partitionDn, wrappedIndex.getAttribute().getOid(), indexEntry,
            currentlyExists );
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( Object attrVal ) throws Exception
    {
        IndexCursor<Object> cursor = forwardCursor();

        try
        {
            cursor.beforeValue( null, attrVal );

            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardGreaterOrEq( Object attrVal, UUID id ) throws Exception
    {
        // Lock down the index by the key
        IndexCursor<Object> cursor = forwardCursor( attrVal );

        try
        {
            cursor.beforeValue( id, attrVal );

            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( UUID id ) throws Exception
    {
        IndexCursor<Object> cursor = reverseCursor();

        try
        {
            cursor.beforeValue( id, null );

            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseGreaterOrEq( UUID id, Object attrVal ) throws Exception
    {
        // lock down the index by the key
        IndexCursor<Object> cursor = reverseCursor( id );

        try
        {
            cursor.beforeValue( id, attrVal );

            if ( cursor.next() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( Object attrVal ) throws Exception
    {
        IndexCursor<Object> cursor = forwardCursor();

        try
        {
            cursor.afterValue( null, attrVal );

            if ( cursor.previous() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean forwardLessOrEq( Object attrVal, UUID id ) throws Exception
    {
        IndexCursor<Object> cursor = forwardCursor( attrVal );

        try
        {
            cursor.afterValue( id, attrVal );

            if ( cursor.previous() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( UUID id ) throws Exception
    {
        IndexCursor<Object> cursor = reverseCursor();

        try
        {
            cursor.afterValue( id, null );

            if ( cursor.previous() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public boolean reverseLessOrEq( UUID id, Object attrVal ) throws Exception
    {
        IndexCursor<Object> cursor = reverseCursor( id );

        try
        {
            cursor.afterValue( id, attrVal );

            if ( cursor.previous() )
            {
                return true;
            }

            return false;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public ForwardIndexComparator<Object> getForwardIndexEntryComparator()
    {
        return wrappedIndex.getForwardIndexEntryComparator();
    }


    /**
     * {@inheritDoc}
     */
    public ReverseIndexComparator<Object> getReverseIndexEntryComparator()
    {
        return wrappedIndex.getReverseIndexEntryComparator();
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        wrappedIndex.close();
    }


    /**
     * {@inheritDoc}
     */
    public void sync() throws Exception
    {
        wrappedIndex.sync();
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return wrappedIndex.isDupsEnabled();
    }
}
