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

import org.apache.directory.server.core.api.partition.index.MasterTable;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * The master table used to store the Attributes of entries.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmMasterTable extends JdbmTable<UUID,Entry> implements MasterTable
{
    /**
     * Creates the master table using JDBM B+Trees for the backing store.
     *
     * @param recMan the JDBM record manager
     * @param schemaManager the schema mamanger
     * @throws Exception if there is an error opening the Db file.
     */
    public JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager ) throws Exception
    {
        super( schemaManager, DBF, recMan, UUIDComparator.INSTANCE, UUIDSerializer.INSTANCE, new EntrySerializer( schemaManager ) );
    }


    protected JdbmMasterTable( RecordManager recMan, SchemaManager schemaManager, String dbName, Serializer serializer ) throws Exception
    {
        super( schemaManager, DBF, recMan, UUIDComparator.INSTANCE, UUIDSerializer.INSTANCE, serializer );
    }

    /**
     * {@inheritDoc}
     */
    public UUID getNextId( Entry entry ) throws Exception
    {
        String name = entry.get( SchemaConstants.ENTRY_UUID_AT ).getString();
        UUID uuid = UUID.fromString( name );
        return uuid; 
    }


    /**
     * {@inheritDoc}
     */
    public void resetCounter() throws Exception
    {
    }
}
