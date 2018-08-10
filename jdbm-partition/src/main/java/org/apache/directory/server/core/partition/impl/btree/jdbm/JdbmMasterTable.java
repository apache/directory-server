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


import java.io.IOException;
import java.util.UUID;

import jdbm.RecordManager;
import jdbm.helper.Serializer;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.xdbm.MasterTable;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmMasterTable extends JdbmTable<String, Entry> implements MasterTable
{
    /**
     * Creates the master table using JDBM B+Trees for the backing store.
     *
     * @param recMan the JDBM record manager
     * @param schemaManager the schema manager
     * @throws IOException if there is an error opening the Db file.
     */
    public JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager ) throws IOException
    {
        super( schemaManager, DBF, recMan, UuidComparator.INSTANCE, UuidSerializer.INSTANCE,
            new EntrySerializer( schemaManager ) );

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );
    }


    protected JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager, String dbName, Serializer serializer )
        throws Exception
    {
        super( schemaManager, DBF, recMan, UuidComparator.INSTANCE, UuidSerializer.INSTANCE, serializer );
    }


    /**
     * Get's the next value from this SequenceBDb.  This has the side-effect of
     * changing the current sequence values permanently in memory and on disk.
     * Master table sequence begins at BigInteger.ONE.  The BigInteger.ZERO is
     * used for the fictitious parent of the suffix root entry.
     *
     * @return the current value incremented by one.
     */
    public String getNextId( Entry entry )
    {
        return UUID.randomUUID().toString();
    }
}
