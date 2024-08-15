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
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.csn.CsnFactory;
import org.apache.directory.api.ldap.model.cursor.Cursor;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.exception.LdapOperationErrorException;
import org.apache.directory.api.ldap.model.exception.LdapOperationException;
import org.apache.directory.api.ldap.model.exception.LdapOtherException;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.ldif.LdifUtils;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.partition.PartitionTxn;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.xdbm.IndexEntry;
import org.apache.directory.server.xdbm.ParentIdAndRdn;
import org.apache.directory.server.xdbm.SingletonIndexCursor;
import org.apache.directory.server.xdbm.search.cursor.DescendantCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A LDIF based partition. Data are stored on disk as LDIF, following this organization :
 * <ul>
 *   <li> each entry is associated with a file, post-fixed with LDIF
 *   <li> each entry having at least one child will have a directory created using its name.
 * </ul>
 * The root is the partition's suffix.
 * <br>
 * So for instance, we may have on disk :
 * <pre>
 * /ou=example,ou=system.ldif
 * /ou=example,ou=system/
 *   |
 *   +--&gt; cn=test.ldif
 *        cn=test/
 *           |
 *           +--&gt; cn=another test.ldif
 *                ...
 * </pre>
 * <br><br>
 * In this exemple, the partition's suffix is <b>ou=example,ou=system</b>.
 * <br>
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdifPartition extends AbstractLdifPartition
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( LdifPartition.class );

    /** The directory into which the entries are stored */
    private File suffixDirectory;

    /** Flags used for the getFile() method */
    private static final boolean CREATE = Boolean.TRUE;
    private static final boolean DELETE = Boolean.FALSE;

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


    /**
     * Creates a new instance of LdifPartition.
     * 
     * @param schemaManager The SchemaManager instance
     * @param dnFactory The DN factory
     */
    public LdifPartition( SchemaManager schemaManager, DnFactory dnFactory )
    {
        super( schemaManager, dnFactory );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected void doInit() throws LdapException
    {
        if ( !initialized )
        {
            File partitionDir = new File( getPartitionPath() );

            // Initialize the suffixDirectory : it's a composition
            // of the workingDirectory followed by the suffix
            if ( ( suffixDn == null ) || ( suffixDn.isEmpty() ) )
            {
                String msg = I18n.err( I18n.ERR_150 );
                LOG.error( msg );
                throw new LdapInvalidDnException( msg );
            }

            if ( !suffixDn.isSchemaAware() )
            {
                suffixDn = new Dn( schemaManager, suffixDn );
            }

            String suffixDirName = getFileName( suffixDn );
            suffixDirectory = new File( partitionDir, suffixDirName );

            super.doInit();

            // Create the context entry now, if it does not exists, or load the
            // existing entries
            if ( suffixDirectory.exists() )
            {
                loadEntries( partitionDir );
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
                        try ( LdifReader reader = new LdifReader( contextEntryFile ) )
                        {
                            contextEntry = new DefaultEntry( schemaManager, reader.next().getEntry() );
                        }
                        catch ( IOException ioe )
                        {
                            throw new LdapOtherException( ioe.getMessage(), ioe );
                        }
                    }
                    else
                    {
                        // No context entry and no LDIF file exists.
                        // Skip initialization of context entry here, it will be added later.
                        return;
                    }
                }

                // Initialization of the context entry
                if ( suffixDn != null )
                {
                    Dn contextEntryDn = contextEntry.getDn();

                    // Checking if the context entry DN is schema aware
                    if ( !contextEntryDn.isSchemaAware() )
                    {
                        contextEntryDn = new Dn( schemaManager, contextEntryDn );
                    }

                    // We're only adding the entry if the two DNs are equal
                    if ( suffixDn.equals( contextEntryDn ) )
                    {
                        // Looking for the current context entry
                        Entry suffixEntry;
                        
                        LookupOperationContext lookupContext = new LookupOperationContext( null, suffixDn );
                        lookupContext.setPartition( this );

                        try ( PartitionTxn partitionTxn = this.beginReadTransaction() )
                        {
                            lookupContext.setTransaction( partitionTxn );
                            suffixEntry = lookup( lookupContext );
                        }
                        catch ( IOException ioe )
                        {
                            throw new LdapOtherException( ioe.getMessage(), ioe );
                        }

                        // We're only adding the context entry if it doesn't already exist
                        if ( suffixEntry == null )
                        {
                            // Checking of the context entry is schema aware
                            if ( !contextEntry.isSchemaAware() )
                            {
                                // Making the context entry schema aware
                                contextEntry = new DefaultEntry( schemaManager, contextEntry );
                            }

                            // Adding the 'entryCsn' attribute
                            if ( contextEntry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
                            {
                                contextEntry.add( SchemaConstants.ENTRY_CSN_AT, new CsnFactory( 0 ).newInstance()
                                    .toString() );
                            }

                            // Adding the 'entryUuid' attribute
                            if ( contextEntry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
                            {
                                String uuid = UUID.randomUUID().toString();
                                contextEntry.add( SchemaConstants.ENTRY_UUID_AT, uuid );
                            }

                            // And add this entry to the underlying partition
                            AddOperationContext addContext = new AddOperationContext( null, contextEntry );
                            addContext.setPartition( this );
                            PartitionTxn partitionTxn = null;
                            
                            try
                            {
                                partitionTxn = beginWriteTransaction();
                                addContext.setTransaction( partitionTxn );
                            
                                add( addContext );
                                partitionTxn.commit();
                            }
                            catch ( Exception e )
                            {
                                try
                                {
                                    if ( partitionTxn != null )
                                    {
                                        partitionTxn.abort();
                                    }
                                }
                                catch ( IOException ioe )
                                {
                                    throw new LdapOtherException( ioe.getMessage(), ioe );
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    //-------------------------------------------------------------------------
    // Operations
    //-------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        super.add( addContext );

        addEntry( addContext.getEntry() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Entry delete( PartitionTxn partitionTxn, String id ) throws LdapException
    {
        Entry deletedEntry = super.delete( partitionTxn, id );

        if ( deletedEntry != null )
        {
            File ldifFile = getFile( deletedEntry.getDn(), DELETE );

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

        return deletedEntry;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        PartitionTxn partitionTxn = modifyContext.getTransaction();
        String id = getEntryId( partitionTxn, modifyContext.getDn() );

        try
        {
            super.modify( modifyContext.getTransaction(), modifyContext.getDn(), modifyContext.getModItems().toArray( new Modification[]
                {} ) );
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage(), e );
        }

        // Get the modified entry and store it in the context for post usage
        Entry modifiedEntry = fetch( modifyContext.getTransaction(), id, modifyContext.getDn() );
        modifyContext.setAlteredEntry( modifiedEntry );

        // Remove the EntryDN
        modifiedEntry.removeAttributes( entryDnAT );

        // just overwrite the existing file
        Dn dn = modifyContext.getDn();

        // And write it back on disk
        
        try ( Writer fw = Files.newBufferedWriter( getFile( dn, DELETE ).toPath(), StandardCharsets.UTF_8 ) )
        {
            fw.write( LdifUtils.convertToLdif( modifiedEntry, true ) );
        }
        catch ( IOException ioe )
        {
            throw new LdapOperationException( ioe.getMessage(), ioe );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws LdapException
    {
        PartitionTxn partitionTxn = moveContext.getTransaction();
        Dn oldDn = moveContext.getDn();
        String id = getEntryId( partitionTxn, oldDn );

        super.move( moveContext );

        // Get the modified entry
        Entry modifiedEntry = fetch( moveContext.getTransaction(), id, moveContext.getNewDn() );

        try
        {
            entryMoved( partitionTxn, oldDn, modifiedEntry, id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws LdapException
    {
        PartitionTxn partitionTxn = moveAndRenameContext.getTransaction(); 
        Dn oldDn = moveAndRenameContext.getDn();
        String id = getEntryId( partitionTxn, oldDn );

        super.moveAndRename( moveAndRenameContext );

        // Get the modified entry and store it in the context for post usage
        Entry modifiedEntry = fetch( moveAndRenameContext.getTransaction(), id, moveAndRenameContext.getNewDn() );
        moveAndRenameContext.setModifiedEntry( modifiedEntry );

        try
        {
            entryMoved( partitionTxn, oldDn, modifiedEntry, id );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws LdapException
    {
        PartitionTxn partitionTxn = renameContext.getTransaction(); 
        Dn oldDn = renameContext.getDn();
        String entryId = getEntryId( partitionTxn, oldDn );

        // Create the new entry
        super.rename( renameContext );

        // Get the modified entry and store it in the context for post usage
        Dn newDn = oldDn.getParent().add( renameContext.getNewRdn() );
        Entry modifiedEntry = fetch( renameContext.getTransaction(), entryId, newDn );
        renameContext.setModifiedEntry( modifiedEntry );

        // Now move the potential children for the old entry
        // and remove the old entry
        try
        {
            entryMoved( partitionTxn, oldDn, modifiedEntry, entryId );
        }
        catch ( Exception e )
        {
            throw new LdapOperationErrorException( e.getMessage(), e );
        }
    }


    /**
     * rewrites the moved entry and its associated children
     * Note that instead of moving and updating the existing files on disk
     * this method gets the moved entry and its children and writes the LDIF files
     *
     * @param oldEntryDn the moved entry's old Dn
     * @param entryId the moved entry's master table ID
     * @param deleteOldEntry a flag to tell whether to delete the old entry files
     * @throws Exception
     */
    private void entryMoved( PartitionTxn partitionTxn, Dn oldEntryDn, Entry modifiedEntry, String entryIdOld ) throws LdapException
    {
        // First, add the new entry
        addEntry( modifiedEntry );

        String baseId = getEntryId( partitionTxn, modifiedEntry.getDn() );

        ParentIdAndRdn parentIdAndRdn = getRdnIndex().reverseLookup( partitionTxn, baseId );
        IndexEntry indexEntry = new IndexEntry();

        indexEntry.setId( baseId );
        indexEntry.setKey( parentIdAndRdn );

        Cursor<IndexEntry<ParentIdAndRdn, String>> cursor = new SingletonIndexCursor<>( partitionTxn, indexEntry );
        String parentId = parentIdAndRdn.getParentId();

        Cursor<IndexEntry<String, String>> scopeCursor = new DescendantCursor( partitionTxn, this, baseId, parentId, cursor );

        // Then, if there are some children, move then to the new place
        try
        {
            while ( scopeCursor.next() )
            {
                IndexEntry<String, String> entry = scopeCursor.get();

                // except the parent entry add the rest of entries
                if ( !Strings.equals( entry.getId(), entryIdOld ) )
                {
                    addEntry( fetch( partitionTxn, entry.getId() ) );
                }
            }

            scopeCursor.close();
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage(), e );
        }

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
    private void loadEntries( File entryDir ) throws LdapException
    {
        LOG.debug( "Processing dir {}", entryDir.getName() );

        // First, load the entries
        File[] entries = entryDir.listFiles( entryFilter );

        if ( ( entries != null ) && ( entries.length != 0 ) )
        {
            LdifReader ldifReader = new LdifReader( schemaManager );

            for ( File entry : entries )
            {
                LOG.debug( "parsing ldif file {}", entry.getName() );
                List<LdifEntry> ldifEntries = ldifReader.parseLdifFile( entry.getAbsolutePath() );
                
                try
                {
                    ldifReader.close();
                }
                catch ( IOException ioe )
                {
                    throw new LdapOtherException( ioe.getMessage(), ioe );
                }

                if ( ( ldifEntries != null ) && !ldifEntries.isEmpty() )
                {
                    // this ldif will have only one entry
                    LdifEntry ldifEntry = ldifEntries.get( 0 );
                    LOG.debug( "Adding entry {}", ldifEntry );

                    Entry serverEntry = new DefaultEntry( schemaManager, ldifEntry.getEntry() );

                    if ( !serverEntry.containsAttribute( SchemaConstants.ENTRY_CSN_AT ) )
                    {
                        serverEntry.put( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
                    }

                    if ( !serverEntry.containsAttribute( SchemaConstants.ENTRY_UUID_AT ) )
                    {
                        serverEntry.put( SchemaConstants.ENTRY_UUID_AT, UUID.randomUUID().toString() );
                    }

                    // call add on the wrapped partition not on the self
                    AddOperationContext addContext = new AddOperationContext( null, serverEntry );
                    PartitionTxn partitionTxn = beginWriteTransaction();
                    
                    try
                    {
                        addContext.setTransaction( partitionTxn );
                        addContext.setPartition( this );
                    
                        super.add( addContext );
                        
                        partitionTxn.commit();
                    }
                    catch ( LdapException le )
                    {
                        try
                        {
                            partitionTxn.abort();
                        }
                        catch ( IOException ioe )
                        {
                            throw new LdapOtherException( ioe.getMessage(), ioe );
                        }
                        
                        throw le;
                    }
                    catch ( IOException ioe )
                    {
                        try
                        {
                            partitionTxn.abort();
                        }
                        catch ( IOException ioe2 )
                        {
                            throw new LdapOtherException( ioe2.getMessage(), ioe2 );
                        }
                        
                        throw new LdapOtherException( ioe.getMessage(), ioe );
                    }
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
     * Create the file name from the entry Dn.
     */
    private File getFile( Dn entryDn, boolean create ) throws LdapException
    {
        String parentDir = null;
        String rdnFileName = null;

        if ( entryDn.equals( suffixDn ) )
        {
            parentDir = suffixDirectory.getParent() + File.separator;
            rdnFileName = suffixDn.getName() + CONF_FILE_EXTN;
        }
        else
        {
            StringBuilder filePath = new StringBuilder();
            filePath.append( suffixDirectory ).append( File.separator );

            Dn baseDn = entryDn.getDescendantOf( suffixDn );
            int size = baseDn.size();

            for ( int i = 0; i < size - 1; i++ )
            {
                rdnFileName = getFileName( baseDn.getRdn( size - 1 - i ) );

                filePath.append( rdnFileName ).append( File.separator );
            }

            rdnFileName = getFileName( entryDn.getRdn() ) + CONF_FILE_EXTN;
            parentDir = filePath.toString();
        }

        File dir = new File( parentDir );

        if ( !dir.exists() && create )
        {
            // We have to create the entry if it does not have a parent
            if ( !dir.mkdir() )
            {
                throw new LdapException( I18n.err( I18n.ERR_00004_COULD_NOT_CREATE_DIRECTORY, dir ) );
            }
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
     * Compute the real name based on the Rdn, assuming that depending on the underlying
     * OS, some characters are not allowed.
     *
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( Rdn rdn ) throws LdapException
    {
        StringBuilder fileName = new StringBuilder( "" );

        Iterator<Ava> iterator = rdn.iterator();

        while ( iterator.hasNext() )
        {
            Ava ava = iterator.next();

            // First, get the AT name, or OID
            String normAT = ava.getNormType();
            AttributeType at = schemaManager.lookupAttributeTypeRegistry( normAT );

            String atName = at.getName();

            // Now, get the normalized value
            String normValue = null;
            
            if ( at.getSyntax().isHumanReadable() )
            {
                normValue = ava.getValue().getString();
            }
            else
            {
                normValue = Strings.utf8ToString( ava.getValue().getBytes() );
            }

            fileName.append( atName ).append( "=" ).append( normValue );

            if ( iterator.hasNext() )
            {
                fileName.append( "+" );
            }
        }

        return getOSFileName( fileName.toString() );
    }


    /**
     * Compute the real name based on the Dn, assuming that depending on the underlying
     * OS, some characters are not allowed.
     *
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( Dn dn ) throws LdapException
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;

        for ( Rdn rdn : dn.getRdns() )
        {
            // First, get the AT name, or OID
            String normAT = rdn.getNormType();
            AttributeType at = schemaManager.lookupAttributeTypeRegistry( normAT );

            String atName = at.getName();

            // Now, get the normalized value
            String normValue = rdn.getAva().getValue().getString();

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
     * Get a OS compatible file name. We URL encode all characters that may cause trouble
     * according to http://en.wikipedia.org/wiki/Filenames. This includes C0 control characters
     * [0x00-0x1F] and 0x7F, see http://en.wikipedia.org/wiki/Control_characters.
     */
    private String getOSFileName( String fileName )
    {
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
                case 0x7F:
                case ' ': // 0x20
                case '"': // 0x22
                case '%': // 0x25
                case '&': // 0x26
                case '(': // 0x28
                case ')': // 0x29
                case '*': // 0x2A
                case '+': // 0x2B
                case '/': // 0x2F
                case ':': // 0x3A
                case ';': // 0x3B
                case '<': // 0x3C
                case '>': // 0x3E
                case '?': // 0x3F
                case '[': // 0x5B
                case '\\': // 0x5C
                case ']': // 0x5D
                case '|': // 0x7C
                    sb.append( "%" ).append( Strings.dumpHex( ( byte ) ( c >> 4 ) ) )
                        .append( Strings.dumpHex( ( byte ) ( c & 0xF ) ) );
                    break;

                default:
                    sb.append( c );
                    break;
            }
        }

        return Strings.toLowerCaseAscii( sb.toString() );
    }


    /**
     * Write the new entry on disk. It does not exist, as this has been checked
     * by the ExceptionInterceptor.
     */
    private void addEntry( Entry entry ) throws LdapException
    {
        // Remove the EntryDN
        entry.removeAttributes( entryDnAT );

        try ( Writer fw = Files.newBufferedWriter( getFile( entry.getDn(), CREATE ).toPath(), StandardCharsets.UTF_8 ) )
        {
            fw.write( LdifUtils.convertToLdif( entry ) );
        }
        catch ( IOException ioe )
        {
            throw new LdapOperationException( ioe.getMessage(), ioe );
        }
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
}