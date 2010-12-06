package org.apache.directory.server.config;


import java.io.File;

import org.apache.directory.server.config.beans.ConfigBean;
import org.apache.directory.server.core.partition.ldif.SingleFileLdifPartition;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.loader.ldif.JarLdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;


public class Main
{
    private Object field;


    public static void main( String[] args ) throws Exception
    {
        //        Deprecated deprecatedAnnotation = TestClass.class.getAnnotation( Deprecated.class );
        //        System.out.println( deprecatedAnnotation );
        //        
        //        deprecatedAnnotation = Main.class.getAnnotation( Deprecated.class );
        //        
        //        Field[] fields = Main.class.getDeclaredFields();
        //        for ( Field field : fields )
        //        {
        //            System.out.println( field );
        //        }

        SchemaLoader schemaLoader = new JarLdifSchemaLoader();
        SchemaManager schemaManager = new DefaultSchemaManager( schemaLoader );
        schemaManager.loadAllEnabled();

        SingleFileLdifPartition configPartition = new SingleFileLdifPartition(
            "/Users/pajbam/Development/Apache/ApacheDS/apacheds/server-config/src/main/resources/config.ldif" );
        configPartition.setId( "config" );
        configPartition.setSuffix( new DN( "ou=config" ) );
        configPartition.setSchemaManager( schemaManager );
        configPartition.initialize();
        

        ConfigPartitionReader cpReader = new ConfigPartitionReader( configPartition, new File(
            "/Users/pajbam/Development/Apache/ApacheDS/apacheds/server-config/src/main/resources/" ) );

        ConfigBean configBean = cpReader.readConfig( new DN( "ou=config" ) );
        
        ConfigWriter.writeConfiguration( schemaManager, configBean, null );
        
    }
}
