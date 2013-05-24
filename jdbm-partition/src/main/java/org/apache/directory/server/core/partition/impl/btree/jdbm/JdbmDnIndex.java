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

package org.apache.directory.server.core.partition.impl.btree.jdbm;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.naming.NamingException;

import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;
import jdbm.recman.TransactionManager;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.MatchingRule;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.comparators.UuidComparator;
import org.apache.directory.server.i18n.I18n;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A special index which stores DN objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmDnIndex extends JdbmIndex<Dn, Entry>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmDnIndex.class );


    public JdbmDnIndex( String oid )
    {
        super( oid, true );
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

        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();

        //System.out.println( "IDX Created index " + path )
        BaseRecordManager base = new BaseRecordManager( path );
        TransactionManager transactionManager = base.getTransactionManager();
        transactionManager.setMaximumTransactionsInLog( 2000 );

        recMan = new CacheRecordManager( base, new MRU( cacheSize ) );

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

        // finally write a text file in the format <OID>-<attribute-name>.txt
        FileWriter fw = new FileWriter( new File( path + "-" + attributeType.getName() + ".txt" ) );

        // write the AttributeType description
        fw.write( attributeType.toString() );
        fw.close();

        initialized = true;
    }


    /**
     * Initializes the forward and reverse tables used by this Index.
     * 
     * @param schemaManager The server schemaManager
     * @throws IOException if we cannot initialize the forward and reverse
     * tables
     * @throws NamingException
     */
    private void initTables( SchemaManager schemaManager ) throws IOException
    {
        MatchingRule mr = attributeType.getEquality();

        if ( mr == null )
        {
            throw new IOException( I18n.err( I18n.ERR_574, attributeType.getName() ) );
        }

        DnSerializerComparator comp = new DnSerializerComparator( mr.getOid() );

        UuidComparator.INSTANCE.setSchemaManager( schemaManager );

        DnSerializer dnSerializer = new DnSerializer( schemaManager );

        forward = new JdbmTable<Dn, String>( schemaManager, attributeType.getOid() + FORWARD_BTREE,
            recMan, comp, dnSerializer, UuidSerializer.INSTANCE );
        reverse = new JdbmTable<String, Dn>( schemaManager, attributeType.getOid() + REVERSE_BTREE,
            recMan, UuidComparator.INSTANCE, UuidSerializer.INSTANCE, dnSerializer );
    }
}
