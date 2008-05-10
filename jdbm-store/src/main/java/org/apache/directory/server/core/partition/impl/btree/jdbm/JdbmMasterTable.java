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
package org.apache.directory.server.core.partition.impl.btree.jdbm;


import jdbm.RecordManager;
import jdbm.helper.LongSerializer;
import jdbm.helper.StringComparator;

import jdbm.helper.Serializer;
import org.apache.directory.server.xdbm.MasterTable;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.entry.ServerEntrySerializer;
import org.apache.directory.server.schema.SerializableComparator;
import org.apache.directory.server.schema.registries.Registries;

import java.io.IOException;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class JdbmMasterTable<E> extends JdbmTable<Long,E> implements MasterTable<E>
{
    private static final StringComparator STRCOMP = new StringComparator();


    private static final SerializableComparator<Long> LONG_COMPARATOR =
            new SerializableComparator<Long>( "1.3.6.1.4.1.18060.0.4.1.1.2" )
    {
        private static final long serialVersionUID = 4048791282048841016L;


        public int compare( Long o1, Long o2 )
        {
            if ( o1 == null )
            {
                throw new IllegalArgumentException( "Argument 'obj1' is null" );
            } 
            else if ( o2 == null )
            {
                throw new IllegalArgumentException( "Argument 'obj2' is null" );
            }

            long thisVal = ( Long ) o1;
            long anotherVal = ( Long ) o2;
            
            if ( thisVal == anotherVal )
            {
                return 0;
            }
            
            if ( thisVal == anotherVal )
            {
                return 0;
            }
            
            if ( thisVal >= 0 )
            {
                if ( anotherVal >= 0 )
                {
                    return ( thisVal > anotherVal ) ? 1 : -1;
                }
                else
                {
                    return -1;
                }
            }
            else if ( anotherVal >= 0 )
            {
                return 1;
            }
            else
            {
                return ( thisVal < anotherVal ) ? -1 : 1;
            }
        }
    };


    private static final SerializableComparator<String> STRING_COMPARATOR =
            new SerializableComparator<String>( "1.3.6.1.4.1.18060.0.4.1.1.3" )
    {
        private static final long serialVersionUID = 3258689922792961845L;


        public int compare( String o1, String o2 )
        {
            return STRCOMP.compare( o1, o2 );
        }
    };


    private final JdbmTable<String,String> adminTbl;


    /**
     * Creates the master table using JDBM B+Trees for the backing store.
     *
     * @param recMan the JDBM record manager
     * @param serializer the serializer to use for persisting objects
     * @throws Exception if there is an error opening the Db file.
     */
    public JdbmMasterTable( RecordManager recMan, Registries registries ) throws Exception
    {
        super( DBF, recMan, LONG_COMPARATOR, LongSerializer.INSTANCE, new ServerEntrySerializer( registries ) );
        adminTbl = new JdbmTable<String,String>( "admin", recMan, STRING_COMPARATOR, null, null );
        String seqValue = adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
    }


    /**
     * Gets the ServerEntry from this MasterTable.
     *
     * @param id the Long id of the entry to retrieve.
     * @return the ServerEntry with operational attributes and all.
     * @throws NamingException if there is a read error on the underlying Db.
     */
    public E get( Object id ) throws Exception
    {
        return super.get( id );
    }


    /**
     * Puts the ServerEntry into this master table at an index
     * specified by id.  Used both to create new entries and update existing
     * ones.
     *
     * @param entry the ServerEntry w/ operational attributes
     * @param id    the Long id of the entry to put
     * @return the ServerEntry put
     * @throws Exception if there is a write error on the underlying Db.
     */
    public E put( Long id, E entry ) throws Exception
    {
        return super.put( id, entry );
    }


    /**
     * Deletes a ServerEntry from the master table at an index specified by id.
     *
     * @param id the Long id of the entry to delete
     * @return the Attributes of the deleted entry
     * @throws Exception if there is a write error on the underlying Db
     */
    public E delete( Long id ) throws Exception
    {
        return super.remove( id );
    }


    public Long getCurrentId() throws Exception
    {
        Long id;

        synchronized ( adminTbl )
        {
            id = new Long( adminTbl.get( SEQPROP_KEY ) );
        }

        return id;
    }


    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values permanently in memory and on disk.
     * Master table sequence begins at BigInteger.ONE.  The BigInteger.ZERO is
     * used for the fictitious parent of the suffix root entry.
     *
     * @return the current value incremented by one.
     * @throws Exception if the admin table storing sequences cannot be
     *                         read and written to.
     */
    public Long getNextId() throws Exception
    {
        Long nextVal;
        Long lastVal;

        synchronized ( adminTbl )
        {
            lastVal = new Long( adminTbl.get( SEQPROP_KEY ) );
            nextVal = lastVal + 1L;
            adminTbl.put( SEQPROP_KEY, nextVal.toString() );
        }

        return nextVal;
    }


    /**
     * Gets a persistent property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to get the value of
     * @return the value of the property
     * @throws Exception when the underlying admin table cannot be read
     */
    public String getProperty( String property ) throws Exception
    {
        synchronized ( adminTbl )
        {
            return adminTbl.get( property );
        }
    }


    /**
     * Sets a persistent property stored in the admin table of this MasterTable.
     *
     * @param property the key of the property to set the value of
     * @param value    the value of the property
     * @throws Exception when the underlying admin table cannot be writen
     */
    public void setProperty( String property, String value ) throws Exception
    {
        synchronized ( adminTbl )
        {
            adminTbl.put( property, value );
        }
    }
}
