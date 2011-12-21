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
import java.util.UUID;

import javax.naming.NamingException;

import jdbm.recman.BaseRecordManager;
import jdbm.recman.SnapshotRecordManager;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.core.api.partition.index.ForwardIndexComparator;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdn;
import org.apache.directory.server.core.api.partition.index.ParentIdAndRdnComparator;
import org.apache.directory.server.core.api.partition.index.ReverseIndexComparator;
import org.apache.directory.server.core.api.partition.index.UUIDComparator;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.MatchingRule;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.SynchronizedLRUMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A special index which stores Rdn objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class JdbmRdnIndex<E> extends JdbmIndex<ParentIdAndRdn>
{

    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( JdbmRdnIndex.class );


    public JdbmRdnIndex()
    {
        initialized = false;
    }


    public JdbmRdnIndex( String attributeId )
    {
        initialized = false;
        setAttributeId( attributeId );
    }


    public void init( SchemaManager schemaManager, AttributeType attributeType ) throws IOException
    {
        LOG.debug( "Initializing an Index for attribute '{}'", attributeType.getName() );
        
        //System.out.println( "IDX Initializing RDNindex for AT " + attributeType.getOid() + ", wkDirPath : " + wkDirPath + ", base dir : " + this.wkDirPath );

        keyCache = new SynchronizedLRUMap( cacheSize );
        this.attributeType = attributeType;

        if ( attributeId == null )
        {
            setAttributeId( attributeType.getName() );
        }
        
        if ( this.wkDirPath == null )
        {
            NullPointerException e = new NullPointerException( "The index working directory has not be set" );
            
            e.printStackTrace();
            throw e;
        }
        
        String path = new File( this.wkDirPath, attributeType.getOid() ).getAbsolutePath();
        
        //System.out.println( "IDX Created index " + path );
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();
        this.recMan = new SnapshotRecordManager( base, cacheSize );

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

        ParentIdAndRdnComparator comp = new ParentIdAndRdnComparator( mr.getOid() );

        forward = new JdbmTable<ParentIdAndRdn, UUID>( schemaManager, attributeType.getOid() + FORWARD_BTREE,
            recMan, comp, null, UUIDSerializer.INSTANCE );
        reverse = new JdbmTable<UUID, ParentIdAndRdn>( schemaManager, attributeType.getOid() + REVERSE_BTREE,
            recMan, UUIDComparator.INSTANCE, UUIDSerializer.INSTANCE, null );
        
        fIndexEntryComparator = new ForwardIndexComparator( comp );
        rIndexEntryComparator = new ReverseIndexComparator( comp );
    }


    public void add( ParentIdAndRdn rdn, UUID entryId ) throws Exception
    {
        forward.put( rdn, entryId );
        reverse.put( entryId, rdn );
    }


    public void drop( UUID entryId ) throws Exception
    {
        ParentIdAndRdn rdn = reverse.get( entryId );
        forward.remove( rdn );
        reverse.remove( entryId );
    }


    public void drop( ParentIdAndRdn rdn, UUID id ) throws Exception
    {
        UUID val = forward.get( rdn );
        if ( val.compareTo( id ) == 0 )
        {
            forward.remove( rdn );
            reverse.remove( val );
        }
    }


    public ParentIdAndRdn getNormalized( ParentIdAndRdn rdn ) throws Exception
    {
        return rdn;
    }
}
