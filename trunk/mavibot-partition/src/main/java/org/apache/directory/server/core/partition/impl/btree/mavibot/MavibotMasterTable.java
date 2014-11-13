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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.IOException;
import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.server.xdbm.MasterTable;


/**
 * TODO MavibotMasterTable.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotMasterTable extends MavibotTable<String, Entry> implements MasterTable
{
    public MavibotMasterTable( RecordManager recordMan, SchemaManager schemaManager, String name, int cacheSize )
        throws IOException
    {
        super( recordMan, schemaManager, name, StringSerializer.INSTANCE, new MavibotEntrySerializer(), false, cacheSize );
    }

    public MavibotMasterTable( RecordManager recordMan, SchemaManager schemaManager, String name )
        throws IOException
    {
        super( recordMan, schemaManager, name, StringSerializer.INSTANCE, new MavibotEntrySerializer(), false );
    }


    @Override
    public String getNextId( Entry entry ) throws Exception
    {
        return UUID.randomUUID().toString();
    }


    @Override
    public void close() throws Exception
    {
        // do nothing here, the RecordManager will be closed in MavibotMasterTable.close()
    }
}
