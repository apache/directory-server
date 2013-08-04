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
package org.apache.directory.server.core.partition.impl.btree.mavibot;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.mavibot.btree.RecordManager;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A special index which stores DN objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MavibotDnIndex extends MavibotIndex<Dn, Entry>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MavibotDnIndex.class );


    public MavibotDnIndex( RecordManager recordMan, String oid )
    {
        super( recordMan, oid, true );
        initialized = false;
    }


    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );

        this.attributeType = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attributeType.getName() );
        }

        if ( this.wkDirPath == null )
        {
            NullPointerException e = new NullPointerException( "The index working directory has not be set" );
            throw e;
        }

        try
        {
            initTables( schemaManager );
        }
        catch ( IOException e )
        {
            // clean up
            close();
            throw e;
        }

        initialized = true;
    }


    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attributeType.getName() ) );
        }

        /*
         * The forward key/value map stores attribute values to master table
         * primary keys.  A value for an attribute can occur several times in
         * different entries so the forward map can have more than one value.
         */
        UuidComparator.INSTANCE.setSchemaManager( schemaManager );

        DnSerializer dnSerializer = new DnSerializer();

        String forwardTableName = attributeType.getOid() + FORWARD_BTREE;
        forward = new MavibotTable<Dn, String>( recordMan, schemaManager, forwardTableName, dnSerializer, new StringSerializer(), false );

        String reverseTableName = attributeType.getOid() + REVERSE_BTREE;
        reverse = new MavibotTable<String, Dn>( recordMan, schemaManager, reverseTableName, new StringSerializer(), dnSerializer, !attributeType.isSingleValued() );

        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();
        // finally write a text file in the format <OID>-<attribute-name>.txt
        FileWriter fw = new FileWriter( new File( path + "-" + attributeType.getName() + ".txt" ) );
        // write the AttributeType description
        fw.write( attributeType.toString() );
        fw.close();
    }
    
}
