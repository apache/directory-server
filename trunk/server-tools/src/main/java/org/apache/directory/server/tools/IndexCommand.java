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
package org.apache.directory.server.tools;


import java.io.File;

import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmMasterTable;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.tools.IndexUtils;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.cursor.Tuple;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;


/**
 * Simple tool used to dump the contents of a jdbm based partition.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexCommand extends ToolCommand
{
    private SchemaManager schemaManager;

    private DirectoryService directoryService;

    public IndexCommand()
    {
        super( "index" );
    }


    private SchemaManager loadSchemaManager() throws Exception
    {
        // --------------------------------------------------------------------
        // Load the bootstrap schemas to start up the schema partition
        // --------------------------------------------------------------------
        directoryService = new DefaultDirectoryService();
        directoryService.setWorkingDirectory( getInstanceLayout().getPartitionsDir() );
        directoryService.startup();

        SchemaManager schemaManager = directoryService.getSchemaManager();
        
        return schemaManager;
    }


    public void execute( CommandLine cmdline ) throws Exception
    {
//        getLayout().verifyInstallation();
        schemaManager = loadSchemaManager();

        String[] partitions = cmdline.getOptionValues( 'p' );
        String attribute = cmdline.getOptionValue( 'a' );
        String indexDirPath = cmdline.getOptionValue( 'w' );
        
        for ( int ii = 0; ii < partitions.length; ii++ )
        {
            File partitionDirectory = new File( getInstanceLayout().getPartitionsDir(), partitions[ii] );
            File indexDir = null;
            
            if( indexDirPath != null )
            {
                indexDir = new File( indexDirPath );
            }
            
            AttributeType attrType = schemaManager.lookupAttributeTypeRegistry( attribute );
            
            System.out.println( "building index for attribute type: " + attrType + ", of the partition: " + partitions[ii] );
            if( indexDir != null )
            {
                System.out.println( "The index file location is: " + indexDir.getAbsolutePath() );
            }
            
            buildIndex( partitionDirectory, indexDir, attrType );
        }
        
        directoryService.shutdown();
    }


    @SuppressWarnings("unchecked")
    private void buildIndex( File partitionDirectory, File indexDirectory, AttributeType attributeType ) throws Exception
    {
        if ( !partitionDirectory.exists() )
        {
            System.err.println( I18n.err( I18n.ERR_196, partitionDirectory ) );
            System.exit( 1 );
        }

        String path = partitionDirectory.getPath() + File.separator + "master";
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();
        CacheRecordManager recMan = new CacheRecordManager( base, new MRU( 1000 ) );

        JdbmMasterTable<Entry> master = new JdbmMasterTable<Entry>( recMan, schemaManager );
        JdbmIndex index = new JdbmIndex();
        index.setAttributeId( attributeType.getName() );
        index.setCacheSize( JdbmIndex.DEFAULT_INDEX_CACHE_SIZE );
        index.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );

        if( indexDirectory == null )
        {
            indexDirectory = partitionDirectory;
        }

        index.setWkDirPath( indexDirectory );
        index.init( schemaManager, attributeType, indexDirectory );

        IndexUtils.printContents( index );
        
        JdbmIndex existenceIdx = new JdbmIndex();
        existenceIdx.setAttributeId( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID );
        existenceIdx.setCacheSize( JdbmIndex.DEFAULT_INDEX_CACHE_SIZE );
        existenceIdx.setNumDupLimit( JdbmIndex.DEFAULT_DUPLICATE_LIMIT );

        existenceIdx.setWkDirPath( partitionDirectory );
        existenceIdx.init( schemaManager, schemaManager.lookupAttributeTypeRegistry( ApacheSchemaConstants.APACHE_EXISTENCE_AT_OID ), partitionDirectory );

        Cursor<Tuple<Long,Entry>> list = master.cursor();
        
        while ( list.next() )
        {
            Tuple<Long,Entry> tuple = list.get();
            Long id = tuple.getKey();
            Entry entry = ( DefaultEntry ) tuple.getValue();

            EntryAttribute attr = entry.get( attributeType );
            if ( attr == null )
            {
                continue;
            }

            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                index.add( attr.get( ii ).get(), id );
            }

            existenceIdx.add( attributeType.getOid(), id );
        }

        index.sync();
        index.close();
        existenceIdx.sync();
        existenceIdx.close();
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = null;
        op = new Option( "p", "partitions", true, "the partitions to add the attribute indices to" );
        op.setRequired( true );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "a", "attributes", true, "the attribute to index" );
        op.setRequired( true );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        op.setRequired( true );
        opts.addOption( op );
        op = new Option( "w", "index-path", true, "path to the directory where index should be stored" );
        op.setRequired( false );
        opts.addOption( op );

        return opts;
    }
}
