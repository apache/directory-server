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
import java.util.UUID;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.partition.avl.AvlPartition;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.csn.CsnFactory;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaUtils;
import org.apache.directory.shared.ldap.schema.registries.Registries;
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
public class LdifPartition extends BTreePartition
{
    /** A logger for this class */
    private static Logger LOG = LoggerFactory.getLogger( LdifPartition.class );
    
    /** The LDIF file parser */
    private LdifReader ldifParser = new LdifReader();

    /** The directory into which the partition is stored */
    private String workingDirectory;

    /** The directory into which the entries are stored */
    private File suffixDirectory;

    /** The context entry */
    private ServerEntry contextEntry;

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

    /** We use an AVL partition to manage searches on this partition */
    private AvlPartition wrappedPartition;

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
    public void initialize() throws Exception
    {
        wrappedPartition.initialize();

        this.searchEngine = wrappedPartition.getSearchEngine();

        LOG.debug( "id is : {}", wrappedPartition.getId() );

        // Initialize the suffixDirectory : it's a composition
        // of the workingDirectory followed by the suffix
        if ( ( suffix == null ) || ( suffix.isEmpty() ) )
        {
            String msg = "Cannot initialize a partition without a valid suffix";
            LOG.error( msg );
            throw new InvalidNameException( msg );
        }
        
        if ( !suffix.isNormalized() )
        {
            suffix.normalize( registries.getAttributeTypeRegistry().getNormalizerMapping() );
        }
        
        String suffixDirName = getFileName( suffix );
        suffixDirectory = new File( workingDirectory, suffixDirName);
        
        // Create the context entry now, if it does not exists, or load the
        // existing entries
        if ( suffixDirectory.exists() )
        {
            loadEntries( new File( workingDirectory ) );
        }
        else
        {
            // The partition directory does not exist, we have to create it
            try
            {
                suffixDirectory.mkdirs();
            }
            catch ( SecurityException se )
            {
                String msg = "The " + suffixDirectory.getAbsolutePath() + " can't be created : " + se.getMessage(); 
                LOG.error( msg );
                throw se;
            }
            
            // And create the context entry too
            File contextEntryFile = new File( suffixDirectory + CONF_FILE_EXTN );

            LOG.info( "ldif file doesn't exist {}, creating it.", contextEntryFile.getAbsolutePath() );
            
            if ( contextEntry == null )
            {
                throw new NamingException( "The expected context entry does not exist" );
            }
            
            if ( contextEntry.get( SchemaConstants.ENTRY_CSN_AT ) == null )
            {
                CsnFactory defaultCSNFactory = new CsnFactory( 0 );

                contextEntry.add( SchemaConstants.ENTRY_CSN_AT, defaultCSNFactory.newInstance().toString() );
            }
            

            if ( contextEntry.get( SchemaConstants.ENTRY_UUID_AT ) == null )
            {
                byte[] uuid = SchemaUtils.uuidToBytes( UUID.randomUUID() );
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
            File file = getFile( entry.getDn() ).getParentFile();
            boolean deleted = deleteFile( file );
            LOG.warn( "deleted file {} {}", file.getAbsoluteFile(), deleted );
        }

    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void modify( ModifyOperationContext modifyContext ) throws Exception
    {
        wrappedPartition.modify( modifyContext );
        // just overwrite the existing file
        add( modifyContext.getEntry() );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void move( MoveOperationContext moveContext ) throws Exception
    {
        LdapDN oldDn = moveContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        wrappedPartition.move( moveContext );

        entryMoved( oldDn, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void moveAndRename( MoveAndRenameOperationContext moveAndRenameContext ) throws Exception
    {
        LdapDN oldDn = moveAndRenameContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        wrappedPartition.moveAndRename( moveAndRenameContext );

        entryMoved( oldDn, id );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void rename( RenameOperationContext renameContext ) throws Exception
    {
        LdapDN oldDn = renameContext.getDn();
        Long id = getEntryId( oldDn.getNormName() );

        wrappedPartition.rename( renameContext );

        entryMoved( oldDn, id );
    }


    private void entryMoved( LdapDN entryDn, Long entryId ) throws Exception
    {
        File file = getFile( entryDn ).getParentFile();
        boolean deleted = deleteFile( file );
        LOG.warn( "move operation: deleted file {} {}", file.getAbsoluteFile(), deleted );

        add( lookup( entryId ) );

        IndexCursor<Long, ServerEntry> cursor = getSubLevelIndex().forwardCursor( entryId );
        
        while ( cursor.next() )
        {
            add( cursor.get().getObject() );
        }

        cursor.close();
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
            for ( File entry : entries )
            {
                LOG.debug( "parsing ldif file {}", entry.getName() );
                List<LdifEntry> ldifEntries = ldifParser.parseLdifFile( entry.getAbsolutePath() );
                
                if ( ( ldifEntries != null ) && !ldifEntries.isEmpty() )
                {
                    // this ldif will have only one entry
                    LdifEntry ldifEntry = ldifEntries.get( 0 );
                    LOG.debug( "Adding entry {}", ldifEntry );

                    ServerEntry serverEntry = new DefaultServerEntry( registries, ldifEntry.getEntry() );

                    // call add on the wrapped partition not on the self
                    wrappedPartition.getStore().add( serverEntry );
                }
            }
        }
        else
        {
            // If we don't have ldif files, we won't have sub directories
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
    private File getFile( LdapDN entryDn ) throws NamingException
    {
        StringBuilder filePath = new StringBuilder();
        filePath.append( suffixDirectory ).append( File.separator );
        
        LdapDN baseDn = (LdapDN)entryDn.getSuffix( suffix.size() );

        for ( int i = 0; i < baseDn.size() - 1; i++ )
        {
            String rdnFileName = getFileName( baseDn.getRdn( i ) );
            
            filePath.append( rdnFileName ).append( File.separator );
        }
        
        String rdnFileName = getFileName( entryDn.getRdn() ) + CONF_FILE_EXTN;
        String parentDir = filePath.toString();
        
        File dir = new File( parentDir );
        
        if ( !dir.exists() )
        {
            // We have to create the entry if it does not have a parent
            dir.mkdir();
        }
        
        File ldifFile = new File( parentDir + rdnFileName );
        
        if ( ldifFile.exists() )
        {
            // The entry already exists
            throw new NamingException( "The entry already exsists" );
        }
        
        return ldifFile;
    }
    
    
    /**
     * Compute the real name based on the RDN, assuming that depending on the underlying 
     * OS, some characters are not allowed.
     * 
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( Rdn rdn ) throws NamingException
    {
        // First, get the AT name, or OID
        String normAT = rdn.getAtav().getNormType();
        AttributeType at = registries.getAttributeTypeRegistry().lookup( normAT );
        
        String atName = at.getName();

        // Now, get the normalized value
        String normValue = rdn.getAtav().getNormValue().getString();
        
        String fileName = atName + "=" + normValue;
        
        return getOSFileName( fileName );
    }
    
    
    /**
     * Compute the real name based on the DN, assuming that depending on the underlying 
     * OS, some characters are not allowed.
     * 
     * We don't allow filename which length is > 255 chars.
     */
    private String getFileName( LdapDN dn ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        
        for ( Rdn rdn : dn.getRdns() )
        {
            // First, get the AT name, or OID
            String normAT = rdn.getAtav().getNormType();
            AttributeType at = registries.getAttributeTypeRegistry().lookup( normAT );
            
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
                    case 0x00 : case 0x01 : case 0x02 : case 0x03 : 
                    case 0x04 : case 0x05 : case 0x06 : case 0x07 : 
                    case 0x08 : case 0x09 : case 0x0A : case 0x0B :
                    case 0x0C : case 0x0D : case 0x0E : case 0x0F :
                    case 0x10 : case 0x11 : case 0x12 : case 0x13 : 
                    case 0x14 : case 0x15 : case 0x16 : case 0x17 : 
                    case 0x18 : case 0x19 : case 0x1A : case 0x1B :
                    case 0x1C : case 0x1D : case 0x1E : case 0x1F :
                        sb.append( "\\" ).append( StringTools.dumpHex( (byte)(c >> 4) ) ).
                            append( StringTools.dumpHex( (byte)(c & 0x04 ) ) );
                        break;
                        
                    case '/' :
                    case '\\' :
                    case '<' :
                    case '>' :
                    case '|' :
                    case '"' :
                    case ':' :
                    case '+' :
                    case ' ' :
                    case '[' :
                    case ']' :
                    case '*' :
                    case '?' :
                        sb.append( '\\' ).append( c );
                        break;
                        
                    default :
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
                    case '/' :
                        sb.append( "\\/" );
                        break;
                        
                    case '\0' :
                        sb.append(  "\\00" );
                        break;
                        
                    default :
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
        FileWriter fw = new FileWriter( getFile( entry.getDn() ) );
        fw.write( LdifUtils.convertEntryToLdif( entry ) );
        fw.close();
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

            return file.delete();
        }
        else
        {
            return file.delete();
        }
    }


    @Override
    public void addIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        wrappedPartition.addIndexOn( index );
    }


    @Override
    public int count() throws Exception
    {
        return wrappedPartition.count();
    }


    @Override
    public void destroy() throws Exception
    {
        wrappedPartition.destroy();
    }


    @Override
    public Index<String, ServerEntry> getAliasIndex()
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
    public Index<String, ServerEntry> getNdnIndex()
    {
        return wrappedPartition.getNdnIndex();
    }


    @Override
    public Index<Long, ServerEntry> getOneAliasIndex()
    {
        return wrappedPartition.getOneAliasIndex();
    }


    @Override
    public Index<Long, ServerEntry> getOneLevelIndex()
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
    public Index<String, ServerEntry> getPresenceIndex()
    {
        return wrappedPartition.getPresenceIndex();
    }


    @Override
    public String getProperty( String propertyName ) throws Exception
    {
        return wrappedPartition.getProperty( propertyName );
    }


    @Override
    public Index<Long, ServerEntry> getSubAliasIndex()
    {
        return wrappedPartition.getSubAliasIndex();
    }


    @Override
    public Index<Long, ServerEntry> getSubLevelIndex()
    {
        return wrappedPartition.getSubLevelIndex();
    }


    @Override
    public Index<?, ServerEntry> getSystemIndex( String id ) throws Exception
    {
        return wrappedPartition.getSystemIndex( id );
    }


    @Override
    public Iterator<String> getSystemIndices()
    {
        return wrappedPartition.getSystemIndices();
    }


    @Override
    public Index<String, ServerEntry> getUpdnIndex()
    {
        return wrappedPartition.getUpdnIndex();
    }


    @Override
    public Index<?, ServerEntry> getUserIndex( String id ) throws Exception
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
        return wrappedPartition.isInitialized();
    }


    @Override
    public IndexCursor<Long, ServerEntry> list( Long id ) throws Exception
    {
        return wrappedPartition.list( id );
    }


    @Override
    public ClonedServerEntry lookup( Long id ) throws Exception
    {
        return wrappedPartition.lookup( id );
    }


    @Override
    public void setAliasIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setAliasIndexOn( index );
    }


    @Override
    public void setNdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setNdnIndexOn( index );
    }


    @Override
    public void setOneAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setOneAliasIndexOn( index );
    }


    @Override
    public void setOneLevelIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setOneLevelIndexOn( index );
    }


    @Override
    public void setPresenceIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setPresenceIndexOn( index );
    }


    @Override
    public void setProperty( String propertyName, String propertyValue ) throws Exception
    {
        wrappedPartition.setProperty( propertyName, propertyValue );
    }


    @Override
    public void setRegistries( Registries registries )
    {
        super.setRegistries( registries );
        wrappedPartition.setRegistries( registries );
    }


    @Override
    public void setSubAliasIndexOn( Index<Long, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setSubAliasIndexOn( index );
    }


    @Override
    public void setUpdnIndexOn( Index<String, ServerEntry> index ) throws Exception
    {
        wrappedPartition.setUpdnIndexOn( index );
    }


    @Override
    public void sync() throws Exception
    {
        wrappedPartition.sync();
        //TODO implement the File I/O here to push the update to entries to the corresponding LDIF file
    }


    public String getSuffix()
    {
        return wrappedPartition.getSuffix();
    }
    

    public LdapDN getSuffixDn()
    {
        return wrappedPartition.getSuffixDn();
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
    public void setSuffix( String suffix ) throws InvalidNameException
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
    public void setContextEntry( String contextEntry ) throws NamingException
    {
        List<LdifEntry> entries = ldifParser.parseLdif( contextEntry );
        
        this.contextEntry = new DefaultServerEntry( registries, entries.get( 0 ).getEntry() );
    }
}