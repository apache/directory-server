/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.partition.impl.btree.je;


import java.util.UUID;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.EntrySerializer;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.StringSerializer;
import org.apache.directory.server.xdbm.MasterTable;

import com.sleepycat.je.Database;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeMasterTable extends BdbJeTable<String, Entry> implements MasterTable
{

    public BdbJeMasterTable( Database db, SchemaManager schemaManager )
    {
        super( db, schemaManager, UuidComparator.INSTANCE,
            new StringSerializer(), new EntrySerializer( schemaManager ) );
    }


    @Override
    public String getNextId( Entry entry )
    {
        return UUID.randomUUID().toString();
    }
}
