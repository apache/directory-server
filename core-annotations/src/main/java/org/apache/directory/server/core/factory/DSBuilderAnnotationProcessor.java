
package org.apache.directory.server.core.factory;

import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.ApplyLdifs;
import org.apache.directory.server.core.annotations.DSBuilder;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DSBuilderAnnotationProcessor
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DSBuilderAnnotationProcessor.class );

    public static DirectoryService getDirectoryService( Description description )
    {
        try
        {
            DSBuilder dsBuilder = description.getAnnotation( DSBuilder.class );
            
            if ( dsBuilder != null )
            {
                LOG.debug( "Starting DS {}...", dsBuilder.name() );
                Class<?> factory = dsBuilder.factory();
                DirectoryServiceFactory dsf = ( DirectoryServiceFactory ) factory.newInstance();
                dsf.init( dsBuilder.name() );
                
                DirectoryService service = dsf.getDirectoryService();
                
                return service;
            }
            else
            {
                LOG.debug( "No {} DS.", description.getDisplayName() );
                return null;
            }
        }
        catch ( Exception e )
        {
            return null;
        }
    }
    
    
    public static DirectoryService getClassDirectoryService( Class<?> clazz )
    {
        try
        {
            DSBuilder dsBuilder = clazz.getAnnotation( DSBuilder.class );
            
            if ( dsBuilder != null )
            {
                LOG.debug( "Starting the {} DS...", clazz.getName() );
                Class<?> factory = dsBuilder.factory();
                DirectoryServiceFactory dsf = ( DirectoryServiceFactory ) factory.newInstance();
                dsf.init( dsBuilder.name() );
                
                DirectoryService service = dsf.getDirectoryService();
                
                return service;
            }
            else
            {
                LOG.debug( "No {} DS.", clazz.getName() );
                return null;
            }
        }
        catch ( Exception e )
        {
            return null;
        }
    }

    
    /**
     * injects an LDIF entry in the given DirectoryService
     * 
     * @param entry the LdifEntry to be injected
     * @param service the DirectoryService
     * @throws Exception
     */
    private static void injectEntry( LdifEntry entry, DirectoryService service ) throws Exception
    {
        if ( entry.isChangeAdd() )
        {
            service.getAdminSession().add( new DefaultServerEntry( service.getSchemaManager(), entry.getEntry() ) );
        }
        else if ( entry.isChangeModify() )
        {
            service.getAdminSession().modify( entry.getDn(), entry.getModificationItems() );
        }
        else
        {
            String message = "Unsupported changetype found in LDIF: " + entry.getChangeType();
            throw new NamingException( message );
        }
    }

    
    /**
     * injects the LDIF entries present in a LDIF file
     * 
     * @param service the DirectoryService 
     * @param ldifFiles the array of LDIF file names (only )
     * @throws Exception
     */
    public static void injectLdifFiles( Class<?> clazz, DirectoryService service, String[] ldifFiles ) throws Exception
    {
        if ( ( ldifFiles != null ) && ( ldifFiles.length > 0 ) )
        {
            for ( String ldifFile : ldifFiles )
            {
                try
                {
                    LdifReader ldifReader = new LdifReader( clazz.getClassLoader().getResourceAsStream( ldifFile ) ); 
    
                    for ( LdifEntry entry : ldifReader )
                    {
                        injectEntry( entry, service );
                    }
                    
                    ldifReader.close();
                }
                catch ( Exception e )
                {
                    LOG.error( "Cannot inject the following entry : {}. Error : {}.", ldifFile, e.getMessage() );
                }
            }
        }
    }
    
    
    /**
     * Inject an ldif String into the server. DN must be relative to the
     * root.
     *
     * @param service the directory service to use 
     * @param ldif the ldif containing entries to add to the server.
     * @throws NamingException if there is a problem adding the entries from the LDIF
     */
    public static void injectEntries( DirectoryService service, String ldif ) throws Exception
    {
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif( ldif );

        for ( LdifEntry entry : entries )
        {
            injectEntry( entry, service );
        }

        // And close the reader
        reader.close();
    }

    
    /**
     * Apply the LDIF entries to the given service
     */
    public static void applyLdifs( Description desc, DirectoryService service ) throws Exception
    {
        if( desc == null )
        {
            return;
        }
        
        ApplyLdifFiles applyLdifFiles = desc.getAnnotation( ApplyLdifFiles.class );

        if ( applyLdifFiles != null )
        {
            LOG.debug( "Applying {} to {}", applyLdifFiles.value(), desc.getDisplayName() );
            injectLdifFiles( desc.getClass(), service, applyLdifFiles.value() );
        }
        
        ApplyLdifs applyLdifs = desc.getAnnotation( ApplyLdifs.class ); 
        
        if ( ( applyLdifs != null ) && ( applyLdifs.value() != null ) )
        {
            String[] ldifs = applyLdifs.value();

            for ( String s : ldifs )
            {
                LOG.debug( "Applying {} to {}", ldifs, desc.getDisplayName() );
                injectEntries( service, s );
            }
        }
    }

}
