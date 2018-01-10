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
package org.apache.directory.mavibot.btree;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.mavibot.btree.memory.BulkDataSorter;
import org.apache.directory.mavibot.btree.serializer.ElementSerializer;
import org.apache.directory.mavibot.btree.serializer.StringSerializer;
import org.apache.directory.server.core.partition.impl.btree.mavibot.LdifTupleComparator;
import org.apache.directory.server.core.partition.impl.btree.mavibot.LdifTupleReaderWriter;
import org.apache.directory.server.core.partition.impl.btree.mavibot.MavibotEntrySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO LdifDataLoader.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifDataLoader
{
    private SchemaManager schemaManager;
    
    private ElementSerializer<String> keySerializer = StringSerializer.INSTANCE;
    
    private ElementSerializer<Entry> valSerializer = new MavibotEntrySerializer();

    private static final Logger LOG = LoggerFactory.getLogger( LdifDataLoader.class );
    
    public LdifDataLoader()
    {
    }
    
    public boolean load( String schemaPartitionDir, String ldifFile, String targetPartitionDir )
    {
        initSchema( schemaPartitionDir );
        
        if( schemaManager == null )
        {
            LOG.warn( "Failed to load the schema, aborting data load" );
            return false;
        }
        
        return load( ldifFile, targetPartitionDir );
    }

    
    public boolean load( String ldifFile, String targetPartitionDir )
    {

        if( schemaManager == null )
        {
            LOG.warn( "No SchemaManager instance was found, aborting data load" );
            return false;
        }

        File dataFile = new File( ldifFile );
        
        if( !dataFile.exists() )
        {
            LOG.warn( "File {} does not exist", ldifFile );
            return false;
        }
        else
        {
            if( !dataFile.canRead() )
            {
                LOG.warn( "File {} cannot be read by the current user", ldifFile );
                return false;
            }
        }
        
        RecordManager rm = new RecordManager( targetPartitionDir );
        Set<String> existing = rm.getManagedTrees();
        if( existing.size() > 2 )
        {
            LOG.warn( "Looks like the given partition directory {} already contains data of a mavibot partiton, please delete this data file and rerun this tool", ldifFile );
            return false;
        }
        
        //MavibotPartitionBuilder<String, Entry> builder = new MavibotPartitionBuilder<String, Entry>( rm, "master", BTree.DEFAULT_PAGE_SIZE, keySerializer, valSerializer );
        
        LdifTupleReaderWriter readerWriter = new LdifTupleReaderWriter( ldifFile, schemaManager );
        LdifTupleComparator tupleComparator = new LdifTupleComparator();
        
        BulkDataSorter sorter = new BulkDataSorter( readerWriter, tupleComparator, 10 );
        
        try
        {
            sorter.sort( dataFile );
            //builder.build( sorter.getMergeSortedTuples() );
            return true;
        }
        catch( Exception e )
        {
            LOG.warn( "Errors occurred while loading data from the data file {}", ldifFile, e );
        }
        finally
        {
            try
            {
                rm.close();
            }
            catch( IOException e )
            {
                LOG.warn( "Failed to close the recordmanager", e );
            }
        }
        
        return false;
    }
    
    
    private void initSchema( String dir )
    {
        if( schemaManager != null )
        {
            return;
        }
        
        try
        {
            File schemaRepository = new File( dir );
            
            if( !schemaRepository.exists() )
            {
                LOG.warn( "The given schema location {} does not exist", dir );
            }
            
            SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
            schemaManager = new DefaultSchemaManager( loader );
            
            LOG.debug( "Loading all enabled schemas" );
            schemaManager.loadAllEnabled();
            
            LOG.debug( "Loading all disabled schemas" );
            List<Schema> lstDisabled = schemaManager.getDisabled();
            for( Schema s : lstDisabled )
            {
                schemaManager.loadDisabled( s );
            }
            
            LOG.debug( "Successfully loaded schemas" );
        }
        catch( Exception e )
        {
            schemaManager = null;
            LOG.warn( e.getMessage(), e );
        }
    }
    
    
    /**
     * @param schemaManager the schemaManager to set
     */
    public void setSchemaManager( SchemaManager schemaManager )
    {
        this.schemaManager = schemaManager;
    }

    public static void main( String[] args )
    {
        
    }
}
