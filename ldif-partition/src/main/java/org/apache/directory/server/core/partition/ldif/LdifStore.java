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

package org.apache.directory.server.core.partition.ldif;


import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.core.partition.avl.AvlStore;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexNotFoundException;
import org.apache.directory.server.xdbm.Store;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * TODO LdifStore.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdifStore<E> implements Store<E, Long>
{

    /** the working directory to use for files */
    private File workingDirectory;

    /** true if we sync disks on every write operation */
    private boolean isSyncOnWrite = true;

    /** in memory store used for serving the config data present in LDIF files  */
    private AvlStore<E> wrappedStore = new AvlStore<E>();

    private SchemaManager schemaManager;

    private LdifReader ldifReader;

    private FileFilter dirFilter = new FileFilter()
    {
        public boolean accept( File dir )
        {
            return dir.isDirectory();
        }
    };

    private static final String CONF_FILE_EXTN = ".ldif";

    private static Logger LOG = LoggerFactory.getLogger( LdifStore.class );


    public void init( SchemaManager schemaManager ) throws Exception
    {
        this.schemaManager = schemaManager;
        wrappedStore.init( schemaManager );

        // load the config 
        loadConfig();
    }


    /**
     * loads the configuration into the DIT from the file system
     * @throws Exception
     */
    public void loadConfig() throws Exception
    {
        String upsuffixDir = wrappedStore.getUpSuffix().getName().toLowerCase();
        File dir = new File( workingDirectory, upsuffixDir );

        if ( !dir.exists() )
        {
            throw new Exception( I18n.err( I18n.ERR_631, upsuffixDir, workingDirectory.getAbsolutePath() ) );
        }

        loadEntry( dir );
    }


    /*
     * recursively load the configuration entries
     */
    private void loadEntry( File entryDir ) throws Exception
    {
        LOG.debug( "processing dir {}", entryDir.getName() );

        File ldifFile = new File( entryDir, entryDir.getName() + CONF_FILE_EXTN );

        try
        {

            ldifReader = new LdifReader();

            if ( ldifFile.exists() )
            {
                LOG.debug( "parsing ldif file {}", ldifFile.getName() );
                List<LdifEntry> entries = ldifReader.parseLdifFile( ldifFile.getAbsolutePath() );

                if ( entries != null && !entries.isEmpty() )
                {
                    // this ldif will have only one entry
                    LdifEntry ldifEntry = entries.get( 0 );
                    LOG.debug( "adding entry {}", ldifEntry );

                    ServerEntry serverEntry = new DefaultServerEntry( schemaManager, ldifEntry.getEntry() );

                    // call add on the wrapped store not on the self  
                    wrappedStore.add( serverEntry );
                }
            }
            else
            {
                // TODO do we need to bomb out if the expected LDIF file doesn't exist
                // I think so
                LOG.warn( "ldif file doesn't exist {}", ldifFile.getAbsolutePath() );
            }
        }
        finally
        {
            ldifReader.close();
        }

        File[] dirs = entryDir.listFiles( dirFilter );

        if ( dirs != null )
        {
            for ( File f : dirs )
            {
                loadEntry( f );
            }
        }
    }


    private File getFile( DN entryDn )
    {
        int size = entryDn.size();

        StringBuilder filePath = new StringBuilder();
        filePath.append( workingDirectory.getAbsolutePath() ).append( File.separator );

        for ( int i = 0; i < size; i++ )
        {
            filePath.append( entryDn.getRdn( i ).getName().toLowerCase() ).append( File.separator );
        }

        File dir = new File( filePath.toString() );
        dir.mkdirs();

        return new File( dir, entryDn.getRdn().getName().toLowerCase() + CONF_FILE_EXTN );
    }


    public void add( ServerEntry entry ) throws Exception
    {
        wrappedStore.add( entry );

        FileWriter fw = new FileWriter( getFile( entry.getDn() ) );
        fw.write( LdifUtils.convertEntryToLdif( entry ) );
        fw.close();
    }


    public void delete( Long id ) throws Exception
    {
        ServerEntry entry = lookup( id );
        LOG.warn( "deleting the entry with id {} and dn {}", id, entry.getDn() );

        LOG.warn( "having the parent id {}", getParentId( entry.getDn().getName() ) );
        wrappedStore.delete( id );

        if ( entry != null )
        {
            File file = getFile( entry.getDn() ).getParentFile();
            boolean deleted = deleteFile( file );
            LOG.warn( "deleted file {} {}", file.getAbsoluteFile(), deleted );
        }
    }


    private boolean deleteFile( File file )
    {
        if ( file.isDirectory() )
        {
            File[] files = file.listFiles();
            for ( File f : files )
            {
                deleteFile( f );
            }
        }

        return file.delete();
    }


    public void destroy() throws Exception
    {
        wrappedStore.destroy();
    }


    public void modify( DN dn, List<Modification> mods ) throws Exception
    {
        wrappedStore.modify( dn, mods );
    }


    public void modify( DN dn, ModificationOperation modOp, ServerEntry mods ) throws Exception
    {
        wrappedStore.modify( dn, modOp, mods );
    }


    public void move( DN oldChildDn, DN newParentDn, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        wrappedStore.move( oldChildDn, newParentDn, newRdn, deleteOldRdn );
    }


    public void move( DN oldChildDn, DN newParentDn ) throws Exception
    {
        wrappedStore.move( oldChildDn, newParentDn );
    }


    public void rename( DN dn, RDN newRdn, boolean deleteOldRdn ) throws Exception
    {
        wrappedStore.rename( dn, newRdn, deleteOldRdn );
    }


    public void sync() throws Exception
    {
        //TODO implement the File I/O here to push the update to entries to the corresponding LDIF file
    }


    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }


    public File getWorkingDirectory()
    {
        return workingDirectory;
    }


    public void setSyncOnWrite( boolean isSyncOnWrite )
    {
        this.isSyncOnWrite = isSyncOnWrite;
    }


    public boolean isSyncOnWrite()
    {
        return isSyncOnWrite;
    }


    public void addIndex( Index<?, E, Long> index ) throws Exception
    {
        wrappedStore.addIndex( index );
    }


    public int count() throws Exception
    {
        return wrappedStore.count();
    }


    public Index<String, E, Long> getAliasIndex()
    {
        return wrappedStore.getAliasIndex();
    }


    public int getChildCount( Long id ) throws Exception
    {
        return wrappedStore.getChildCount( id );
    }


    public String getEntryDn( Long id ) throws Exception
    {
        return wrappedStore.getEntryDn( id );
    }


    public Long getEntryId( String dn ) throws Exception
    {
        return wrappedStore.getEntryId( dn );
    }


    public String getEntryUpdn( Long arg0 ) throws Exception
    {
        return wrappedStore.getEntryUpdn( arg0 );
    }


    public String getEntryUpdn( String dn ) throws Exception
    {
        return wrappedStore.getEntryUpdn( dn );
    }


    public String getName()
    {
        return wrappedStore.getName();
    }


    public Index<String, E, Long> getNdnIndex()
    {
        return wrappedStore.getNdnIndex();
    }


    public Index<Long, E, Long> getOneAliasIndex()
    {
        return wrappedStore.getOneAliasIndex();
    }


    public Index<Long, E, Long> getOneLevelIndex()
    {
        return wrappedStore.getOneLevelIndex();
    }


    public Long getParentId( Long arg0 ) throws Exception
    {
        return wrappedStore.getParentId( arg0 );
    }


    public Long getParentId( String dn ) throws Exception
    {
        return wrappedStore.getParentId( dn );
    }


    public Index<String, E, Long> getPresenceIndex()
    {
        return wrappedStore.getPresenceIndex();
    }


    public String getProperty( String propertyName ) throws Exception
    {
        return wrappedStore.getProperty( propertyName );
    }


    public Index<Long, E, Long> getSubAliasIndex()
    {
        return wrappedStore.getSubAliasIndex();
    }


    public Index<Long, E, Long> getSubLevelIndex()
    {
        return wrappedStore.getSubLevelIndex();
    }


    public Index<?, E, Long> getIndex( String id ) throws IndexNotFoundException
    {
        return wrappedStore.getIndex( id );
    }


    public Index<?, E, Long> getSystemIndex( String id ) throws IndexNotFoundException
    {
        return wrappedStore.getSystemIndex( id );
    }


    public Index<String, E, Long> getUpdnIndex()
    {
        return wrappedStore.getUpdnIndex();
    }


    public Index<?, E, Long> getUserIndex( String id ) throws IndexNotFoundException
    {
        return wrappedStore.getUserIndex( id );
    }


    public Set<Index<?, E, Long>> getUserIndices()
    {
        return wrappedStore.getUserIndices();
    }


    public boolean hasIndexOn( String id ) throws Exception
    {
        return wrappedStore.hasIndexOn( id );
    }


    public boolean hasSystemIndexOn( String id ) throws Exception
    {
        return wrappedStore.hasSystemIndexOn( id );
    }


    public boolean hasUserIndexOn( String id ) throws Exception
    {
        return wrappedStore.hasUserIndexOn( id );
    }


    public boolean isInitialized()
    {
        return wrappedStore.isInitialized();
    }


    public IndexCursor<Long, E, Long> list( Long id ) throws Exception
    {
        return wrappedStore.list( id );
    }


    public ServerEntry lookup( Long id ) throws Exception
    {
        return wrappedStore.lookup( id );
    }


    public void setAliasIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setAliasIndex( index );
    }


    public void setName( String name )
    {
        wrappedStore.setName( name );
    }


    public void setNdnIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setNdnIndex( index );
    }


    public void setOneAliasIndex( Index<Long, E, Long> index ) throws Exception
    {
        wrappedStore.setOneAliasIndex( index );
    }


    public void setOneLevelIndex( Index<Long, E, Long> index ) throws Exception
    {
        wrappedStore.setOneLevelIndex( index );
    }


    public void setPresenceIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setPresenceIndex( index );
    }


    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        wrappedStore.setProperty( propertyName, propertyValue );
    }


    public void setSubAliasIndex( Index<Long, E, Long> index ) throws Exception
    {
        wrappedStore.setSubAliasIndex( index );
    }


    public void setSubLevelIndex( Index<Long, E, Long> index ) throws Exception
    {
        wrappedStore.setSubLevelIndex( index );
    }


    public void setUpdnIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setUpdnIndex( index );
    }


    public Iterator<String> systemIndices()
    {
        return wrappedStore.systemIndices();
    }


    public Iterator<String> userIndices()
    {
        return wrappedStore.userIndices();
    }


    //TODO manage the cache size??
    public int getCacheSize()
    {
        return wrappedStore.getCacheSize();
    }


    public Index<String, E, Long> getEntryCsnIndex()
    {
        return wrappedStore.getEntryCsnIndex();
    }


    public Index<String, E, Long> getEntryUuidIndex()
    {
        return wrappedStore.getEntryUuidIndex();
    }


    public Index<String, E, Long> getObjectClassIndex()
    {
        return wrappedStore.getObjectClassIndex();
    }


    public DN getSuffix()
    {
        return wrappedStore.getSuffix();
    }


    public String getSuffixDn()
    {
        return wrappedStore.getSuffixDn();
    }


    public DN getUpSuffix()
    {
        return wrappedStore.getUpSuffix();
    }


    public void setCacheSize( int size )
    {
        wrappedStore.setCacheSize( size );
    }


    public void setUserIndices( Set<Index<?, E, Long>> userIndices )
    {
        wrappedStore.setUserIndices( userIndices );
    }


    public void setSuffixDn( String suffixDn )
    {
        wrappedStore.setSuffixDn( suffixDn );
    }


    public void setEntryCsnIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setEntryCsnIndex( index );
    }


    public void setEntryUuidIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setEntryUuidIndex( index );
    }


    public void setObjectClassIndex( Index<String, E, Long> index ) throws Exception
    {
        wrappedStore.setObjectClassIndex( index );
    }


    public Long getDefaultId()
    {
        return 1L;
    }

}
