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


import java.io.File;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.Durability;
import com.sleepycat.je.Durability.ReplicaAckPolicy;
import com.sleepycat.je.Durability.SyncPolicy;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;


/**
 * A helper class which contains a reference to the bdb je Enviroment 
 * instance and the associated configuration.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class BdbJePartitionEnviroment
{

    private static final Logger LOG = LoggerFactory.getLogger( BdbJePartitionEnviroment.class );

    private Environment jeEnv;

    private EnvironmentConfig defaultConfig;

    /**
     * a set holding the new names created for each cache name present in the cacheMap
     * This is done to prevent name collision when multiple bdb je partitions are present
     * and they all use(for the sake of simplicity and ease) copies of the config cache 
     * file with same cache names
     */
    private Set<String> usedCacheNameSet = new HashSet<String>();


    public BdbJePartitionEnviroment( SchemaManager schemaManager, File partitionDir )
    {
        defaultConfig = new EnvironmentConfig();
        defaultConfig.setAllowCreate( true );
        defaultConfig.setTransactional( true );

        Durability durability = new Durability( SyncPolicy.WRITE_NO_SYNC, SyncPolicy.NO_SYNC, ReplicaAckPolicy.NONE );

        defaultConfig.setDurability( durability );

        jeEnv = new Environment( partitionDir, defaultConfig );
    }


    public Database createDb( String name )
    {
        DatabaseConfig dbConfig = new DatabaseConfig();
        //dbConfig.setBtreeComparator( keyComparator );

        return createDb( name, dbConfig );
    }


    public Database createDb( String name, Comparator keyComparator )
    {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setBtreeComparator( keyComparator );

        return createDb( name, dbConfig );
    }


    public Database createDb( String name, Comparator keyComparator, Comparator valueComparator, boolean allowDups )
    {
        DatabaseConfig dbConfig = new DatabaseConfig();

        if ( keyComparator != null )
        {
            dbConfig.setBtreeComparator( keyComparator );
        }

        if ( valueComparator != null )
        {
            dbConfig.setDuplicateComparator( valueComparator );
        }

        dbConfig.setSortedDuplicates( allowDups );

        return createDb( name, dbConfig );

    }


    public Database createDb( String name, boolean allowDups )
    {
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setSortedDuplicates( allowDups );

        return createDb( name, dbConfig );
    }


    public Database createDb( String name, Comparator keyComparator, Comparator valueComparator )
    {
        return createDb( name, keyComparator, valueComparator, false );
    }


    public Database createDb( String name, DatabaseConfig dbConfig )
    {
        dbConfig.setAllowCreate( true );
        dbConfig.setTransactional( true );

        Transaction txn = createTxn();

        Database db = jeEnv.openDatabase( txn, name, dbConfig );

        txn.commit();

        return db;
    }


    public Transaction createTxn()
    {
        return jeEnv.beginTransaction( null, null );
    }


    public void commitTxn( Transaction txn )
    {
        txn.commit();
    }


    public void abortTxn( Transaction txn )
    {
        if ( txn != null )
        {
            txn.abort();
        }
    }


    public boolean isTransactional()
    {
        return jeEnv.getConfig().getTransactional();
    }


    public void close()
    {
        if ( jeEnv != null )
        {
            LOG.debug( "closing je environemt" );
            jeEnv.close();
        }
    }


    /**
     * gives a set of OIDs of the attributes that are indexed  in the partition
     * Warn: should only be called after opening the environment
     * 
     * @return set of index OIDs
     */
    public Set<String> getIndexOids()
    {
        List<String> names = jeEnv.getDatabaseNames();
        Set<String> oids = new HashSet<String>();

        for ( String s : names )
        {
            int pos = s.indexOf( BdbJeIndex.FORWARD_KEY );
            if ( pos > 0 )
            {
                oids.add( s.substring( 0, pos ) );
            }
        }

        return oids;
    }


    /**
     * delete the databases of the index for the given attribute id
     *  
     * @param atOid the OID of the attribute which is indexed
     */
    public void deleteIndexDb( String atOid )
    {
        deleteDb( atOid + BdbJeIndex.FORWARD_KEY );
        deleteDb( atOid + BdbJeIndex.REVERSE_KEY );
    }


    public void deleteDb( String name )
    {
        jeEnv.removeDatabase( null, name );
    }
}
