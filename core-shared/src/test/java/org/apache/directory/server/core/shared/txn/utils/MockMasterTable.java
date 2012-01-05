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
package org.apache.directory.server.core.shared.txn.utils;


import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.cursor.Tuple;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * A Mock master table
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MockMasterTable implements MasterTable
{
    private Map<UUID, Entry> entries = new HashMap<UUID, Entry>();


    @Override
    public Comparator<UUID> getKeyComparator()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Comparator<Entry> getValueComparator()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getName()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public boolean isDupsEnabled()
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean has( UUID key ) throws Exception
    {
        return entries.containsKey( key );
    }


    @Override
    public boolean has( UUID key, Entry value ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean hasGreaterOrEqual( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean hasLessOrEqual( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean hasGreaterOrEqual( UUID key, Entry val ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public boolean hasLessOrEqual( UUID key, Entry val ) throws Exception
    {
        // TODO Auto-generated method stub
        return false;
    }


    @Override
    public Entry get( UUID key ) throws Exception
    {
        return entries.get( key );
    }


    @Override
    public void put( UUID key, Entry value ) throws Exception
    {
        entries.put( key, value );
    }


    @Override
    public void remove( UUID key ) throws Exception
    {
        entries.remove( key );
    }


    @Override
    public void remove( UUID key, Entry value ) throws Exception
    {
        // TODO Auto-generated method stub
    }


    @Override
    public Cursor<Tuple<UUID, Entry>> cursor() throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Cursor<Tuple<UUID, Entry>> cursor( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Cursor<Entry> valueCursor( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int count() throws Exception
    {
        return entries.size();
    }


    @Override
    public int count( UUID key ) throws Exception
    {
        if ( entries.containsKey( key ) )
        {
            return 1;
        }
        else
        {
            return 0;
        }
    }


    @Override
    public int greaterThanCount( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public int lessThanCount( UUID key ) throws Exception
    {
        // TODO Auto-generated method stub
        return 0;
    }


    @Override
    public void close() throws Exception
    {
        // TODO Auto-generated method stub

    }


    @Override
    public UUID getNextId( Entry entry ) throws Exception
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void resetCounter() throws Exception
    {
        // TODO Auto-generated method stub
    }
}
