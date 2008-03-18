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
import org.apache.directory.server.core.partition.impl.btree.MasterTable;
import org.apache.directory.server.schema.SerializableComparator;

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
            } else if ( o2 == null )
            {
                throw new IllegalArgumentException( "Argument 'obj2' is null" );
            }

            return o1.compareTo( o2 );
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
    public JdbmMasterTable( RecordManager recMan, Serializer serializer ) throws Exception
    {
        super( DBF, recMan, LONG_COMPARATOR, LongSerializer.INSTANCE, serializer );
        adminTbl = new JdbmTable<String,String>( "admin", recMan, STRING_COMPARATOR, null, null );
        String seqValue = adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
    }


    public E get( Long id ) throws IOException
    {
        return super.get( id );
    }


    public E put( Long id, E entry ) throws Exception
    {
        return super.put( id, entry );
    }


    public E delete( Long id ) throws IOException
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


    public String getProperty( String property ) throws IOException
    {
        synchronized ( adminTbl )
        {
            return adminTbl.get( property );
        }
    }


    public void setProperty( String property, String value ) throws Exception
    {
        synchronized ( adminTbl )
        {
            adminTbl.put( property, value );
        }
    }
}
