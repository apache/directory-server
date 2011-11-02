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

import java.util.Comparator;

import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.txn.TxnLogManager;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;

import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;

/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MasterTableWrapper<ID> implements MasterTable<ID, Entry>
{
    /** Wrapped master table */
    private MasterTable<ID, Entry> wrappedTable;
    
    /** partition the table belongs to */
    private Dn partitionDn;
    
    /**
     * {@inheritDoc}
     */
    public ID getNextId( Entry entry ) throws Exception
    {
        return wrappedTable.getNextId( entry );
    }


    /**
     * {@inheritDoc}
     */
    public void resetCounter() throws Exception
    {
        wrappedTable.resetCounter();
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Comparator<ID> getKeyComparator()
    {
        return wrappedTable.getKeyComparator();
    }


    /**
     * {@inheritDoc}
     */
    public Comparator<Entry> getValueComparator()
    {
        return wrappedTable.getValueComparator();
    }


    /**
     * {@inheritDoc}
     */
    public String getName()
    {
       return wrappedTable.getName(); 
    }


    /**
     * {@inheritDoc}
     */
    public boolean isDupsEnabled()
    {
        return wrappedTable.isDupsEnabled();
    }


    /**
     * {@inheritDoc}
     */
    public boolean has( ID key ) throws Exception
    {
        return ( get( key ) != null );
    }


    /**
     * {@inheritDoc}
     */
    public boolean has( ID key, Entry value ) throws Exception
    {
        Entry stored = get( key );
        
        return ( ( stored != null ) && stored.equals( value ) );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasGreaterOrEqual( ID key ) throws Exception
    {
        return wrappedTable.hasGreaterOrEqual( key );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasLessOrEqual( ID key ) throws Exception
    {
        return wrappedTable.hasLessOrEqual( key );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasGreaterOrEqual( ID key, Entry val ) throws Exception
    {
        return wrappedTable.hasGreaterOrEqual( key, val );
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasLessOrEqual( ID key, Entry val ) throws Exception
    {
        return wrappedTable.hasLessOrEqual( key, val );
    }


    /**
     * {@inheritDoc}
     */
    public Entry get( ID key ) throws Exception
    {
        if ( key == null )
        {
            return null;
        }
        
        TxnLogManager<ID> logManager = TxnManagerFactory.<ID>txnLogManagerInstance();
        Entry entry = wrappedTable.get( key );
        entry = logManager.mergeUpdates( partitionDn, key, entry );
        
        return entry;
    }


    /**
     * {@inheritDoc}
     */
    public void put( ID key, Entry value ) throws Exception
    {
        wrappedTable.put( key, value ); 
    }


    /**
     * {@inheritDoc}
     */
    public void remove( ID key ) throws Exception
    {
        wrappedTable.remove( key ); 
    }


    /**
     * {@inheritDoc}
     */
    public void remove( ID key, Entry value ) throws Exception
    {
        wrappedTable.remove( key, value );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<ID, Entry>> cursor() throws Exception
    {
        return wrappedTable.cursor();
    }
    

    /**
     * {@inheritDoc}
     */
    public Cursor<Tuple<ID, Entry>> cursor( ID key ) throws Exception
    {
        return wrappedTable.cursor( key );
    }


    /**
     * {@inheritDoc}
     */
    public Cursor<Entry> valueCursor( ID key ) throws Exception
    {
        return wrappedTable.valueCursor( key );
    }


    /**
     * {@inheritDoc}
     */
    public int count() throws Exception
    {
        return wrappedTable.count();
    }


    /**
     * {@inheritDoc}
     */
    public int count( ID key ) throws Exception
    {
        return wrappedTable.count( key );
    }
    

    /**
     * {@inheritDoc}
     */
    public int greaterThanCount( ID key ) throws Exception
    {
        return wrappedTable.greaterThanCount( key );
    }


    /**
     * {@inheritDoc}
     */
    public int lessThanCount( ID key ) throws Exception
    {
        return wrappedTable.lessThanCount( key );
    }


    /**
     * {@inheritDoc}
     */
    public void close() throws Exception
    {
        wrappedTable.close();
    }
}
