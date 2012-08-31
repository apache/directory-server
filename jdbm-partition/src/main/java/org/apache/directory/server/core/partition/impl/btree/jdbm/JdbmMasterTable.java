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


import java.util.UUID;

import jdbm.RecordManager;
import jdbm.helper.Serializer;
import jdbm.helper.StringComparator;

import org.apache.directory.server.xdbm.MasterTable;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.comparators.SerializableComparator;
import org.apache.directory.shared.ldap.model.schema.comparators.UuidComparator;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmMasterTable extends JdbmTable<String, Entry> implements MasterTable
{
    private static final StringComparator STRCOMP = new StringComparator();

    private static final SerializableComparator<String> STRING_COMPARATOR =
        new SerializableComparator<String>( "1.3.6.1.4.1.18060.0.4.1.1.3" )
        {
            private static final long serialVersionUID = 3258689922792961845L;


            public int compare( String o1, String o2 )
            {
                return STRCOMP.compare( o1, o2 );
            }
        };

    protected final JdbmTable<String, String> adminTbl;


    /**
     * Creates the master table using JDBM B+Trees for the backing store.
     *
     * @param recMan the JDBM record manager
     * @param schemaManager the schema manager
     * @throws Exception if there is an error opening the Db file.
     */
    public JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager, DBF, recMan, UuidComparator.INSTANCE, UuidSerializer.INSTANCE,
            new EntrySerializer( schemaManager ) );
        adminTbl = new JdbmTable<String, String>( schemaManager, "admin", recMan, STRING_COMPARATOR, null, null );
        String seqValue = adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );
        STRING_COMPARATOR.setSchemaManager( schemaManager );
    }


    protected JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager, String dbName, Serializer serializer )
        throws Exception
    {
        super( schemaManager, DBF, recMan, UuidComparator.INSTANCE, UuidSerializer.INSTANCE, serializer );
        adminTbl = new JdbmTable<String, String>( schemaManager, dbName, recMan, STRING_COMPARATOR, null, null );
        String seqValue = adminTbl.get( SEQPROP_KEY );

        if ( null == seqValue )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
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
    public String getNextId( Entry entry ) throws Exception
    {
        return UUID.randomUUID().toString();
    }


    /**
     * {@inheritDoc}
     */
    public void resetCounter() throws Exception
    {
        synchronized ( adminTbl )
        {
            adminTbl.put( SEQPROP_KEY, "0" );
        }
    }
}
