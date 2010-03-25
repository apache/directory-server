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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.avl.AvlPartition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.ldif.LdapLdifException;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A LDIF based partition. Data are stored on disk as LDIF, following this organisation :
 * <li> each entry is associated with a file, postfixed with LDIF
 * <li> each entry having at least one child will have a directory created using its name.
 * The root is the partition's suffix.
 * <br>
 * So for instance, we may have on disk :
 * <pre>
 * /ou=example,ou=system.ldif
 * /ou=example,ou=system/
 *   |
 *   +--> cn=test.ldif
 *        cn=test/
 *           |
 *           +--> cn=another test.ldif
 *                ...
 * </pre>
 * <br><br>            
 * In this exemple, the partition's suffix is <b>ou=example,ou=system</b>. 
 * <br>   
 *  
 * @org.apache.xbean.XBean
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdifPartition extends BTreePartition<Long>
{
    /** A logger for this class */
    private static Logger LOG = LoggerFactory.getLogger( LdifPartition.class );

    /** The directory into which the partition is stored */
    private String workingDirectory;

    /** The directory into which the entries are stored */
    private File suffixDirectory;

    /** Flags used for the getFile() method */
    private static final boolean CREATE = Boolean.TRUE;
    private static final boolean DELETE = Boolean.FALSE;

    private int ldifScanInterval;

    /** A filter used to pick all the directories */
    private FileFilter dirFilter = new FileFilter()
    {
        public boolean accept( File dir )
        {
            return dir.isDirectory();
        }
    };

    /** A filter used to pick all the ldif entries */
    private FileFilter entryFilter = new FileFilter()
    {
        public boolean accept( File dir )
        {
            if ( dir.getName().endsWith( CONF_FILE_EXTN ) )
            {
                return dir.isFile();
            }
            else
            {
                return false;
            }
        }
    };

    /** The extension used for LDIF entry files */
    private static final String CONF_FILE_EXTN = ".ldif";

    /** We use a partition to manage searches on this partition */
    private AvlPartition wrappedPartition;

    /** A default CSN factory */
    private static CsnFactory defaultCSNFactory;


    /**
     * Creates a new instance of LdifPartition.
     */
    public LdifPartition()
    {
        wrappedPartition = new AvlPartition();
    }


    /**
     * {@inheritDoc}
     */
    protected void doInit() throws Exception
    {
        // Initialize the AvlPartition
        wrappedPartition.setId( id );
        wrappedPartition.setSuffix( suffix.getName() );
        wrappedPartition.setSchemaManager( schemaManager );
        wrappedPartition.initialize();

        // Create the CsnFactory with a invalid ReplicaId
        // @TODO : inject a correct ReplicaId
        defaultCSNFactory = new CsnFactory( 0 );

        this.searchEngine = wrappedPartition.getSearchEngine();

        LOG.debug( "id is : {}", wrappedPartition.getId() );

        // Initialize the suffixDirectory : it's a composition
        // of the workingDirectory followed by the suffix
        if ( ( suffix == null ) || ( suffix.isEmpty() ) )
        {
            String msg = I18n.err( I18n.ERR_150 );
            LOG.error( msg );
            throw new LdapInvalidDnException( msg );
        }

        if ( !suffix.isNormalized() )
        {
            suffix.normalize( schemaManager.getNormalizerMapping() );
        }

        String suffixDirName = getFileName( suffix );
        suffixDirectory = new File( workingDirectory, suffixDirName );

        // Create the context entry now, if it does not exists, or load the
        // existing entries
        if ( suffixDirectory.exists() )
        {
            loadEntries( new File( workingDirectory ) );
        }
        else
        {
            // The partition directory does not exist, we have to create it, including parent directories
            try
            {
                suffixDirectory.mkdirs();
            }
            catch ( SecurityException se )
            {
                String msg = I18n.err( I18n.ERR_151, suffixDirectory.getAbsolutePath(), se.getLocalizedMessage() );
                LOG.error( msg );
                throw se;
            }

            // And create the context entry too
            File contextEntryFile = new File( suffixDirectory + CONF_FILE_EXTN );

            LOG.info( "ldif file doesn't exist {}, creating it.", contextEntryFile.getAbsolutePath() );

            if ( contextEntry == null )
            {
                if ( contextEntryFile.exists() )
                {
                    LdifReader reader = new LdifReader( contextEntryFile );
                    contextEntry = new DefaultServerEntry( schemaManager, reader.next().getEntry() );
                    reader.close();
                }
                else
                {
                    // No context entry and no LDIF file exists.
                    // Skip initialization of context entry here, it will be added later.
                    return;
                }
            }

            if ( contextEntry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
            {
                contextEntry.add( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
            }

            if ( contextEntry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
            {
                String uuid = UUID.randomUUID().toString();
                contextEntry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
            }

            FileWriter fw = new FileWriter( contextEntryFile );
            fw.write( LdifUtils.convertEntryToLdif( contextEntry ) );
            fw.close();

            // And add this entry to the underlying partition
            wrappedPartition.getStore().add( contextEntry );
        }
    }


    //-------------------------------------------------------------------------
    // Operations
    //-------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws Exception
    {
        wrappedPartition.add( addContext );
        add( addContext.getEntry() );
    }


    /**
     * {@inheritDoc}
     */
    public void bind( BindOperationContext bindContext ) throws Exception
    {
        wrappedPartition.bind( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete( Long id ) throws Exception
    {
        ServerEntry entry = lookup( id );

        wrappedPartition.delete( id );

        if ( entry != null )
        {
            File ldifFile = getFile( entry.getDn(), DELETE );

            boolean deleted = deleteFile( ldifFile );

            LOG.debug( "deleted file {} {}", ldifFile.getAbsoluteFile(), deleted );

            // Delete the parent if there is no more children
            File parentFile = ldifFile.getParentFile();

            if ( parentFile.listFiles().length == 0 )
            {
                deleteFile( parentFile );

                LOG.debug( "deleted file {} {}", parentFile.getAbsoluteFile(), deleted );
            }

        }

    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws Exception
    {
        Long id = getEntryId( modifyContext.getDn().getNormName() );

        wrappedPartition.modify( id, modifyContext.getModItems() );

        // Get the modified entry and store it in the context for post usage
        ClonedServerEntry modifiedEntry = lookup( id );
        modifyContext.setAlteredEntry( modifiedEntry );

        // just overwrite the existing file
        DN dn = modifyContext.getDn();

        // And write it back on disk
        FileWriter fw = new FileWriter( getFile( dn, DELETE ) );
        fw.write( LdifUtils.convertEntryToLdif( modifiedEntry ) );
        fw.close();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws Exception
    {
        DN oldDn = moveContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        wrappedPartition.move( moveContext );

        // Get the modified entry
        ClonedServerEntry modifiedEntry = lookup( id );

        entryMoved( oldDn, modifiedEntry, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws Exception
    {
        DN oldDn = moveAndRenameContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        wrappedPartition.moveAndRename( moveAndRenameContext );

        // Get the modified entry and store it in the context for post usage
        ClonedServerEntry modifiedEntry = lookup( id );
        moveAndRenameContext.setAlteredEntry( modifiedEntry );

        entryMoved( oldDn, modifiedEntry, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws Exception
    {
        DN oldDn = renameContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        // Create the new entry 
        wrappedPartition.rename( renameContext );

        // Get the modified entry and store it in the context for post usage
        ClonedServerEntry modifiedEntry = lookup( id );
        renameContext.setAlteredEntry( modifiedEntry );

        // Now move the potential children for the old entry
        // and remove the old entry
        entryMoved( oldDn, modifiedEntry, id );
    }


    /**
     * rewrites the moved entry and its associated children
     * Note that instead of moving and updating the existing files on disk
     * this method gets the moved entry and its children and writes the LDIF files
     *
     * @param oldEntryDn the moved entry's old DN
     * @param entryId the moved entry's master table ID
     * @param deleteOldEntry a flag to tell whether to delete the old entry files
     * @throws Exception
     */
    private void entryMoved( DN oldEntryDn, Entry modifiedEntry, Long entryIdOld ) throws Exception
    {
        // First, add the new entry
        add( modifiedEntry );

        // Then, if there are some children, move then to the new place
        IndexCursor<Long, ServerEntry, Long> cursor = getSubLevelIndex().forwardCursor( entryIdOld );

        while ( cursor.next() )
        {
            IndexEntry<Long, ServerEntry, Long> entry = cursor.get();

            // except the parent entry add the rest of entries
            if ( entry.getId() != entryIdOld )
            {
                add( wrappedPartition.lookup( entry.getId() ) );
            }
        }

        cursor.close();

        // And delete the old entry's LDIF file
        File file = getFile( oldEntryDn, DELETE );
        boolean deleted = deleteFile( file );
        LOG.warn( "move operation: deleted file {} {}", file.getAbsoluteFile(), deleted );

        // and the associated directory ( the file's name's minus ".ldif")
        String dirName = file.getAbsolutePath();
        dirName = dirName.substring( 0, dirName.indexOf( CONF_FILE_EXTN ) );
        deleted = deleteFile( new File( dirName ) );
        LOG.warn( "move operation: deleted dir {} {}", dirName, deleted );
    }


    /**
     * loads the configuration into the DIT from the file system
     * Note that it assumes the presence of a directory with the partition suffix's upname
     * under the partition's base dir
     * 
     * for ex. if 'config' is the partition's id and 'ou=config' is its suffix it looks for the dir with the path
     * 
     * <directory-service-working-dir>/config/ou=config
     * e.x example.com/config/ou=config
     * 
     * NOTE: this dir setup is just to ease the testing of this partition, this needs to be 
     * replaced with some kind of bootstrapping the default config from a jar file and
     * write to the FS in LDIF format
     * 
     * @throws Exception
     */
    private void loadEntries( File entryDir ) throws Exception
    {
        LOG.debug( "Processing dir {}", entryDir.getName() );

        // First, load the entries
        File[] entries = entryDir.listFiles( entryFilter );

        if ( ( entries != null ) && ( entries.length != 0 ) )
        {
            LdifReader ldifReader = new LdifReader();

            for ( File entry : entries )
            {
                LOG.debug( "parsing ldif file {}", entry.getName() );
                List<LdifEntry> ldifEntries = ldifReader.parseLdifFile( entry.getAbsolutePath() );
                ldifReader.close();

                if ( ( ldifEntries != null ) && !ldifEntries.isEmpty() )
                {
                    // this ldif will have only one entry
                    LdifEntry ldifEntry = ldifEntries.get( 0 );
                    LOG.debug( "Adding entry {}", ldifEntry );

                    ServerEntry serverEntry = new DefaultServerEntry( schemaManager, ldifEntry.getEntry() );

                    if ( !serverEntry.containsAttribute( SchemaConstants.ENTRY_CSN_AT ) )
                    {
                        serverEntry.put( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
                    }

                    if ( !serverEntry.containsAttribute( SchemaConstants.ENTRY_UUID_AT ) )
                    {
                        serverEntry.put( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
                    }

                    // call add on the wrapped partition not on the self
                    wrappedPartition.getStore().add( serverEntry );
                }
            }

        }
        else
        {
            // If we don't have ldif files, we won't have sub-directories
            return;
        }

        // Second, recurse on the sub directories
        File[] dirs = entryDir.listFiles( dirFilter );

        if ( ( dirs != null ) && ( dirs.length != 0 ) )
        {
            for ( File f : dirs )
            {
                loadEntries( f );
            }
        }
    }


    /**
     * Create the file name from the entry DN.
     */
    private File getFile( DN entryDn, boolean create ) throws LdapException
    {
        StringBuilder filePath = new StringBuilder();
        filePath.append( suffixDirectory ).append( File.separator );

        DN baseDn = ( DN ) entryDn.getSuffix( suffix.size() );

        for ( int i = 0; i < baseDn.size() - 1; i++ )
        {
            String rdnFileName = getFileName( baseDn.getRdn( i ) );

            filePath.append( rdnFileName ).append( File.separator );
        }

        String rdnFileName = getFileName( entryDn.getRdn() ) + CONF_FILE_EXTN;
        String parentDir = filePath.toString();

        File dir = new File( parentDir );

        if ( !dir.exists() && create )
        {
            // We have to create the entry if it does not have a parent
            dir.mkdir();
        }

        File ldifFile = new File( parentDir + rdnFileName );

        if ( ldifFile.exists() && create )
        {
            // The entry already exists
            throw new LdapException( I18n.err( I18n.ERR_633 ) );
        }

        return ldifFile;
    }


    /**
     * Compute the real name based on the RDN, assuming that depending on the underlying 
     * OS, some characters are not allowed.
     * 
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( RDN rdn ) throws LdapException
    {
        String fileName = "";

        Iterator<AVA> iterator = rdn.iterator();
        while ( iterator.hasNext() )
        {
            AVA ava = iterator.next();

            // First, get the AT name, or OID
            String normAT = ava.getNormType();
            AttributeType at = schemaManager.lookupAttributeTypeRegistry( normAT );

            String atName = at.getName();

            // Now, get the normalized value
            String normValue = ava.getNormValue().getString();

            fileName += atName + "=" + normValue;

            if ( iterator.hasNext() )
            {
                fileName += "+";
            }
        }

        return getOSFileName( fileName );
    }


    /**
     * Compute the real name based on the DN, assuming that depending on the underlying 
     * OS, some characters are not allowed.
     * 
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( DN dn ) throws LdapException
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for ( RDN rdn : dn.getRdns() )
        {
            // First, get the AT name, or OID
            String normAT = rdn.getAtav().getNormType();
            AttributeType at = schemaManager.lookupAttributeTypeRegistry( normAT );

            String atName = at.getName();

            // Now, get the normalized value
            String normValue = rdn.getAtav().getNormValue().getString();

            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append( "," );
            }

            sb.append( atName ).append( "=" ).append( normValue );
        }

        return getOSFileName( sb.toString() );
    }


    /**
     * Get a OS compatible file name
     */
    private String getOSFileName( String fileName )
    {
        if ( SystemUtils.IS_OS_WINDOWS )
        {
            // On Windows, we escape '/', '<', '>', '\', '|', '"', ':', '+', ' ', '[', ']', 
            // '*', [0x00-0x1F], '?'
            StringBuilder sb = new StringBuilder();

            for ( char c : fileName.toCharArray() )
            {
                switch ( c )
                {
                    case 0x00:
                    case 0x01:
                    case 0x02:
                    case 0x03:
                    case 0x04:
                    case 0x05:
                    case 0x06:
                    case 0x07:
                    case 0x08:
                    case 0x09:
                    case 0x0A:
                    case 0x0B:
                    case 0x0C:
                    case 0x0D:
                    case 0x0E:
                    case 0x0F:
                    case 0x10:
                    case 0x11:
                    case 0x12:
                    case 0x13:
                    case 0x14:
                    case 0x15:
                    case 0x16:
                    case 0x17:
                    case 0x18:
                    case 0x19:
                    case 0x1A:
                    case 0x1B:
                    case 0x1C:
                    case 0x1D:
                    case 0x1E:
                    case 0x1F:
                        sb.append( "\\" ).append( StringTools.dumpHex( ( byte ) ( c >> 4 ) ) ).append(
                            StringTools.dumpHex( ( byte ) ( c & 0x04 ) ) );
                        break;

                    case '/':
                    case '\\':
                    case '<':
                    case '>':
                    case '|':
                    case '"':
                    case ':':
                    case '+':
                    case ' ':
                    case '[':
                    case ']':
                    case '*':
                    case '?':
                        sb.append( '\\' ).append( c );
                        break;

                    default:
                        sb.append( c );
                        break;
                }
            }

            return sb.toString().toLowerCase();
        }
        else
        {
            // On linux, just escape '/' and null
            StringBuilder sb = new StringBuilder();

            for ( char c : fileName.toCharArray() )
            {
                switch ( c )
                {
                    case '/':
                        sb.append( "\\/" );
                        break;

                    case '\0':
                        sb.append( "\\00" );
                        break;

                    default:
                        sb.append( c );
                        break;
                }
            }

            return sb.toString().toLowerCase();
        }
    }


    /**
     * Write the new entry on disk. It does not exist, as this ha sbeen checked
     * by the ExceptionInterceptor.
     */
    private void add( Entry entry ) throws Exception
    {
        FileWriter fw = new FileWriter( getFile( entry.getDn(), CREATE ) );
        fw.write( LdifUtils.convertEntryToLdif( entry ) );
        fw.close();
    }


    /** 
     * Recursively delete an entry and all of its children. If the entry is a directory, 
     * then get into it, call the same method on each of the contained files,
     * and delete the directory.
     */
    private boolean deleteFile( File file )
    {
        if ( file.isDirectory() )
        {
            File[] files = file.listFiles();

            // Process the contained files
            for ( File f : files )
            {
                deleteFile( f );
            }

            // then delete the directory itself
            return file.delete();
        }
        else
        {
            return file.delete();
        }
    }


    @Override
    public void addIndexOn( Index<? extends Object, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.addIndexOn( index );
    }


    @Override
    public int count() throws Exception
    {
        return wrappedPartition.count();
    }


    @Override
    protected void doDestroy() throws Exception
    {
        wrappedPartition.destroy();
    }


    @Override
    public Index<String, ServerEntry, Long> getAliasIndex()
    {
        return wrappedPartition.getAliasIndex();
    }


    @Override
    public int getChildCount( Long id ) throws Exception
    {
        return wrappedPartition.getChildCount( id );
    }


    @Override
    public String getEntryDn( Long id ) throws Exception
    {
        return wrappedPartition.getEntryDn( id );
    }


    @Override
    public Long getEntryId( String dn ) throws Exception
    {
        return wrappedPartition.getEntryId( dn );
    }


    @Override
    public String getEntryUpdn( Long id ) throws Exception
    {
        return wrappedPartition.getEntryUpdn( id );
    }


    @Override
    public String getEntryUpdn( String dn ) throws Exception
    {
        return wrappedPartition.getEntryUpdn( dn );
    }


    @Override
    public Index<String, ServerEntry, Long> getNdnIndex()
    {
        return wrappedPartition.getNdnIndex();
    }


    @Override
    public Index<Long, ServerEntry, Long> getOneAliasIndex()
    {
        return wrappedPartition.getOneAliasIndex();
    }


    @Override
    public Index<Long, ServerEntry, Long> getOneLevelIndex()
    {
        return wrappedPartition.getOneLevelIndex();
    }


    @Override
    public Long getParentId( Long childId ) throws Exception
    {
        return wrappedPartition.getParentId( childId );
    }


    @Override
    public Long getParentId( String dn ) throws Exception
    {
        return wrappedPartition.getParentId( dn );
    }


    @Override
    public Index<String, ServerEntry, Long> getPresenceIndex()
    {
        return wrappedPartition.getPresenceIndex();
    }


    @Override
    public String getProperty( String propertyName ) throws Exception
    {
        return wrappedPartition.getProperty( propertyName );
    }


    @Override
    public Index<Long, ServerEntry, Long> getSubAliasIndex()
    {
        return wrappedPartition.getSubAliasIndex();
    }


    @Override
    public Index<Long, ServerEntry, Long> getSubLevelIndex()
    {
        return wrappedPartition.getSubLevelIndex();
    }


    @Override
    public Index<?, ServerEntry, Long> getSystemIndex( String id ) throws Exception
    {
        return wrappedPartition.getSystemIndex( id );
    }


    @Override
    public Iterator<String> getSystemIndices()
    {
        return wrappedPartition.getSystemIndices();
    }


    @Override
    public Index<String, ServerEntry, Long> getUpdnIndex()
    {
        return wrappedPartition.getUpdnIndex();
    }


    @Override
    public Index<? extends Object, ServerEntry, Long> getUserIndex( String id ) throws Exception
    {
        return wrappedPartition.getUserIndex( id );
    }


    @Override
    public Iterator<String> getUserIndices()
    {
        return wrappedPartition.getUserIndices();
    }


    @Override
    public boolean hasSystemIndexOn( String id ) throws Exception
    {
        return wrappedPartition.hasSystemIndexOn( id );
    }


    @Override
    public boolean hasUserIndexOn( String id ) throws Exception
    {
        return wrappedPartition.hasUserIndexOn( id );
    }


    @Override
    public boolean isInitialized()
    {
        return wrappedPartition != null && wrappedPartition.isInitialized();
    }


    @Override
    public IndexCursor<Long, ServerEntry, Long> list( Long id ) throws Exception
    {
        return wrappedPartition.list( id );
    }


    @Override
    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        return wrappedPartition.lookup( id );
    }


    @Override
    public void setAliasIndexOn( Index<String, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setAliasIndexOn( index );
    }


    @Override
    public void setNdnIndexOn( Index<String, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setNdnIndexOn( index );
    }


    @Override
    public void setOneAliasIndexOn( Index<Long, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setOneAliasIndexOn( index );
    }


    @Override
    public void setOneLevelIndexOn( Index<Long, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setOneLevelIndexOn( index );
    }


    @Override
    public void setPresenceIndexOn( Index<String, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setPresenceIndexOn( index );
    }


    @Override
    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        wrappedPartition.setProperty( propertyName, propertyValue );
    }


    @Override
    public void setSchemaManager( SchemaManager schemaManager )
    {
        super.setSchemaManager( schemaManager );
    }


    @Override
    public void setSubAliasIndexOn( Index<Long, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setSubAliasIndexOn( index );
    }


    @Override
    public void setUpdnIndexOn( Index<String, ServerEntry, Long> index ) throws Exception
    {
        wrappedPartition.setUpdnIndexOn( index );
    }


    @Override
    public void sync() throws Exception
    {
        wrappedPartition.sync();
        //TODO implement the File I/O here to push the update to entries to the corresponding LDIF file
    }


    public void unbind( UnbindOperationContext unbindContext ) throws Exception
    {
        wrappedPartition.unbind( unbindContext );
    }


    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return super.getId();
    }


    @Override
    public void setId( String id )
    {
        super.setId( id );
        wrappedPartition.setId( id );
    }


    @Override
    public void setSuffix( String suffix ) throws LdapInvalidDnException
    {
        super.setSuffix( suffix );
        wrappedPartition.setSuffix( suffix );
    }


    /**
     * the interval at which the config directory containing LDIF files
     * should be scanned, default value is 10 min
     * 
     * @param ldifScanInterval the scan interval time in minutes
     */
    public void setLdifScanInterval( int ldifScanInterval )
    {
        this.ldifScanInterval = ldifScanInterval;
    }


    /**
     * @return the workingDirectory
     */
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }


    /**
     * @param workingDirectory the workingDirectory to set
     */
    public void setWorkingDirectory( String workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }


    /**
     * @return the contextEntry
     */
    public Entry getContextEntry()
    {
        return contextEntry;
    }


    /**
     * @param contextEntry the contextEntry to set
     */
    public void setContextEntry( String contextEntry ) throws LdapLdifException
    {
        LdifReader ldifReader = new LdifReader();
        List<LdifEntry> entries = ldifReader.parseLdif( contextEntry );

        try
        {
            ldifReader.close();
        }
        catch ( IOException ioe )
        {
            // What can we do here ???
        }

        this.contextEntry = new DefaultServerEntry( schemaManager, entries.get( 0 ).getEntry() );
    }


    /**
     * @return the wrappedPartition
     */
    public Partition getWrappedPartition()
    {
        return wrappedPartition;
    }


    /**
     * @param wrappedPartition the wrappedPartition to set
     */
    public void setWrappedPartition( AvlPartition wrappedPartition )
    {
        this.wrappedPartition = wrappedPartition;
    }
}