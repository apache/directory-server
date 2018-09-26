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


import java.util.Comparator;

import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.DnComparator;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.DnSerializer;
import org.apache.directory.server.core.partition.impl.btree.je.serializers.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Database;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJeDnIndex extends BdbJeIndex<Dn>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( BdbJeDnIndex.class );


    public BdbJeDnIndex( String oid )
    {
        super( oid, true );
        initialized = false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( SchemaManager schemaManager, BdbJePartitionEnviroment environment )
    {
        LOG.debug( "Initializing RDN Index for attribute '{}'", attributeId );

        attributeType = schemaManager.getAttributeType( attributeId );
        attribute = attributeType;

        String oid = attributeType.getOid();
        Comparator comp = new DnComparator( oid );

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );

        DnSerializer dnSerializer = new DnSerializer( schemaManager );

        Database dnForwardDb = environment.createDb( oid + FORWARD_KEY );
        forward = new BdbJeTable<Dn, String>( dnForwardDb, schemaManager, comp, dnSerializer, new StringSerializer() );

        Database dnReverseDb = environment.createDb( oid + FORWARD_KEY );
        reverse = new BdbJeTable<String, Dn>( dnReverseDb, schemaManager, UuidComparator.INSTANCE,
            new StringSerializer(), dnSerializer );
        initialized = true;
    }
}
