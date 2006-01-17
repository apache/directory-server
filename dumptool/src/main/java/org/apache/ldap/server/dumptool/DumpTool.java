package org.apache.ldap.server.dumptool;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;


import org.apache.ldap.common.message.LockableAttributeImpl;
import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.server.configuration.StartupConfiguration;
import org.apache.ldap.server.partition.impl.btree.Tuple;
import org.apache.ldap.server.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.ldap.server.partition.impl.btree.jdbm.JdbmMasterTable;
import org.apache.ldap.server.schema.bootstrap.BootstrapRegistries;
import org.apache.ldap.server.schema.bootstrap.BootstrapSchemaLoader;


/**
 * Simple tool used to dump the contents of a jdbm based partition.
 */
public class DumpTool
{
    /**
     * Just give it the path to the working directory where all the db files
     * are for a partition like the system partition.  Right now this will print 
     * stuff out to the console. 
     * 
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception
    {
        File workingDirectory = new File( args[0] );
        System.out.println( "# ========================================================================");
        System.out.println( "# Dumptool Version: 0.1" );
        System.out.println( "# Partition Working Directory: " + workingDirectory );
        System.out.println( "# ========================================================================\n\n");
        CacheRecordManager recMan;
        JdbmMasterTable master;
        
        try 
        {
            String path = workingDirectory.getPath() + File.separator + "master";
            BaseRecordManager base = new BaseRecordManager( path );
            base.disableTransactions();
            recMan = new CacheRecordManager( base, new MRU( 1000 ) );
        } 
        catch ( IOException e )
        {
            NamingException ne = new NamingException( 
                "Could not initialize RecordManager" );
            ne.setRootCause( e );
            throw ne;
        }

        master = new JdbmMasterTable( recMan );
        BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
        BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
        StartupConfiguration startupConfiguration = new StartupConfiguration();
        loader.load( startupConfiguration.getBootstrapSchemas(), bootstrapRegistries );
        AttributeType attributeType = bootstrapRegistries.getAttributeTypeRegistry().lookup( "apacheUpdn" );
        JdbmIndex idIndex = new JdbmIndex( attributeType, workingDirectory );

        System.out.println( "#---------------------" );
        NamingEnumeration list = master.listTuples();
        while ( list.hasMore() )
        {
            Tuple tuple = ( Tuple ) list.next();
            BigInteger id = ( BigInteger ) tuple.getKey();
            Attributes entry = ( Attributes ) tuple.getValue();
            
            if ( ! ( entry instanceof LockableAttributesImpl ) )
            {
                Attributes tmp = entry;
                entry = new LockableAttributesImpl();
                NamingEnumeration attrs = tmp.getAll();
                while ( attrs.hasMore() )
                {
                    Attribute attr = ( Attribute ) attrs.next();
                    LockableAttributeImpl myattr = new LockableAttributeImpl( attr.getID() );
                    entry.put( myattr );
                    for ( int ii = 0; ii < attr.size(); ii++ )
                    {
                        myattr.add( attr.get( ii ) );
                    }
                }
            }
            
            String dn = ( String ) idIndex.reverseLookup( id );
            if ( list.hasMore() )
            {
                System.out.println( "# Entry: " + id + "\n#---------------------\n\n" + "dn: " + dn + "\n" + entry 
                    + "\n\n#---------------------" );
            }
            else
            {
                System.out.println( "# Entry: " + id + "\n#---------------------\n\n" + "dn: " + dn + "\n" + entry );
            }
        }
    }
}
