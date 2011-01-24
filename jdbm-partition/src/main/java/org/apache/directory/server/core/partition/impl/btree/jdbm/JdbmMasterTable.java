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
import jdbm.helper.Serializer;
import jdbm.helper.StringComparator;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.MasterTable;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmMasterTable<E> extends JdbmTable<Long,E> implements MasterTable<Long, E>
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
                throw new IllegalArgumentException( I18n.err( I18n.ERR_525 ) );
            } 
            else if ( o2 == null )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_526 ) );
            }

            if ( o1 == ( long ) o2 )
            {
                return 0;
            }
            
            if ( o1 == ( long ) o2 )
            {
                return 0;
            }
            
            if ( o1 >= 0 )
            {
                if ( o2 >= 0 )
                {
                    return ( o1 > ( long ) o2 ) ? 1 : -1;
                }
                else
                {
                    return -1;
                }
            }
            else if ( o2 >= 0 )
            {
                return 1;
            }
            else
            {
                return ( o1 < ( long ) o2 ) ? -1 : 1;
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


    protected final JdbmTable<String,String> adminTbl;


    /**
     * Creates the master table using JDBM B+Trees for the backing store.
     *
     * @param recMan the JDBM record manager
     * @param schemaManager the schema mamanger
     * @throws Exception if there is an error opening the Db file.
     */
    public JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager, DBF, recMan, LONG_COMPARATOR, LongSerializer.INSTANCE, new ServerEntrySerializer( schemaManager ) );
        adminTbl = new JdbmTable<String,String>( schemaManager, "admin", recMan, STRING_COMPARATOR, null, null );
        String seqValue = adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
        
        LONG_COMPARATOR.setSchemaManager( schemaManager );
        STRING_COMPARATOR.setSchemaManager( schemaManager );
    }


    protected JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager, String dbName, Serializer serializer ) throws Exception
    {
        super( schemaManager, DBF, recMan, LONG_COMPARATOR, LongSerializer.INSTANCE, serializer );
        adminTbl = new JdbmTable<String,String>( schemaManager, dbName, recMan, STRING_COMPARATOR, null, null );
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
     * @throws Exception if there is a read error on the underlying Db.
     */
    public E get( Long id ) throws Exception
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
     * @throws Exception if there is a write error on the underlying Db.
     */
    public void put( Long id, E entry ) throws Exception
    {
        super.put( id, entry );
    }


    /**
     * Deletes a ServerEntry from the master table at an index specified by id.
     *
     * @param id the Long id of the entry to delete
     * @throws Exception if there is a write error on the underlying Db
     */
    public void delete( Long id ) throws Exception
    {
        super.remove( id );
    }


    public Long getCurrentId( E entry ) throws Exception
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
    public Long getNextId( E entry ) throws Exception
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
