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


import java.net.URI;
import java.util.Comparator;

import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.Index;
import org.apache.directory.server.core.api.partition.index.IndexCursor;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;

import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.AttributeType;

public class IndexWrapper<ID> implements Index<Object, Entry, ID>
{
    /** wrapped index */ 
    Index<Object,Entry,ID> wrappedIndex;
    
    /** partition the table belongs to */
    private Dn partitionDn;
   

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
    public ID forwardLookup( Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor( attrVal );
        
        try
        {
            cursor.beforeValue( null, attrVal );
            if ( cursor.next() )
            {
                return cursor.get().getId();
            }
            
            return null;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public Object reverseLookup( ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor( id );
        
        try
        {
            cursor.beforeValue( id, null );
            if ( cursor.next() )
            {
                return cursor.get().getValue();
            }
            
            return null;
        }
        finally
        {
            cursor.close();
        }
    }


    /**
     * {@inheritDoc}
     */
    public void add( Object attrVal, ID id ) throws Exception
    {
        wrappedIndex.add( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( ID entryId ) throws Exception
    {
        wrappedIndex.drop( entryId );
    }


    /**
     * {@inheritDoc}
     */
    public void drop( Object attrVal, ID id ) throws Exception
    {
        wrappedIndex.drop( attrVal, id );
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object, Entry, ID> reverseCursor() throws Exception
    {
        IndexCursor<Object, Entry, ID> wrappedCursor;
        IndexCursor<Object, Entry, ID> cursor = wrappedIndex.reverseCursor();
        TxnLogManager<ID> logManager = TxnManagerFactory.<ID>txnLogManagerInstance();
        
        try
        {
            wrappedCursor = logManager.wrap( partitionDn, cursor, wrappedIndex.getReverseIndexEntryComparator(), 
                wrappedIndex.getAttribute().getOid(), false, null, null );
        } catch (Exception e)
        {
            cursor.close( e );
            throw e;
        }
        
        return wrappedCursor;
    }


    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object, Entry, ID> forwardCursor() throws Exception
    {
        IndexCursor<Object, Entry, ID> wrappedCursor;
        IndexCursor<Object, Entry, ID> cursor = wrappedIndex.reverseCursor();
        TxnLogManager<ID> logManager = TxnManagerFactory.<ID>txnLogManagerInstance();
        
        try
        {
            wrappedCursor = logManager.wrap( partitionDn, cursor, wrappedIndex.getForwardIndexEntryComparator(), 
                wrappedIndex.getAttribute().getOid(), true, null, null );
        } catch (Exception e)
        {
            cursor.close( e );
            throw e;
        }
        
        return wrappedCursor;
    }

    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object, Entry, ID> reverseCursor( ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> wrappedCursor;
        IndexCursor<Object, Entry, ID> cursor = wrappedIndex.reverseCursor();
        TxnLogManager<ID> logManager = TxnManagerFactory.<ID>txnLogManagerInstance();
        
        try
        {
            wrappedCursor = logManager.wrap( partitionDn, cursor, wrappedIndex.getReverseIndexEntryComparator(), 
                wrappedIndex.getAttribute().getOid(), false, null, id );
        } catch (Exception e)
        {
            cursor.close( e );
            throw e;
        }
        
        return wrappedCursor;
    }

    /**
     * {@inheritDoc}
     */
    public IndexCursor<Object, Entry, ID> forwardCursor( Object key ) throws Exception
    {
        IndexCursor<Object, Entry, ID> wrappedCursor;
        IndexCursor<Object, Entry, ID> cursor = wrappedIndex.reverseCursor();
        TxnLogManager<ID> logManager = TxnManagerFactory.<ID>txnLogManagerInstance();
        
        try
        {
            wrappedCursor = logManager.wrap( partitionDn, cursor, wrappedIndex.getForwardIndexEntryComparator(), 
                wrappedIndex.getAttribute().getOid(), true, key, null );
        } catch (Exception e)
        {
            cursor.close( e );
            throw e;
        }
        
        return wrappedCursor;
    }

    /**
     * {@inheritDoc}
     */
    public Cursor<Object> reverseValueCursor( ID id ) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Cursor<ID> forwardValueCursor( Object key ) throws Exception
    {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public boolean forward( Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor( attrVal );
        
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
    public boolean forward( Object attrVal, ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor( attrVal );
        Comparator<ID> idComp = wrappedIndex.getForwardIndexEntryComparator().getIDComparator();
        
        try
        {
            cursor.beforeValue( id, attrVal );
            if ( cursor.next() && ( idComp.compare( cursor.get().getId(), id ) == 0 ) )
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
    public boolean reverse( ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor( id );
        
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
    public boolean reverse( ID id, Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor( id );
        Comparator<Object> valueComp = wrappedIndex.getForwardIndexEntryComparator().getValueComparator();
        
        try
        {
            cursor.beforeValue( id, attrVal );
            if ( cursor.next() && ( valueComp.compare( cursor.get().getValue(), attrVal ) == 0 ))
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
    public boolean forwardGreaterOrEq( Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor();
        
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
    public boolean forwardGreaterOrEq( Object attrVal, ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor();
        
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
    public boolean reverseGreaterOrEq( ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor();
        
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
    public boolean reverseGreaterOrEq( ID id, Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor();
        
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

        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor();
        
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
    public boolean forwardLessOrEq( Object attrVal, ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.forwardCursor();
        
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
    public boolean reverseLessOrEq( ID id ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor();
        
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
    public boolean reverseLessOrEq( ID id, Object attrVal ) throws Exception
    {
        IndexCursor<Object, Entry, ID> cursor = this.reverseCursor();
        
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
    public ForwardIndexComparator<Object,ID> getForwardIndexEntryComparator()
    {
        return wrappedIndex.getForwardIndexEntryComparator();
    }

    
    /**
     * {@inheritDoc}
     */
    public ReverseIndexComparator<Object,ID> getReverseIndexEntryComparator()
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
