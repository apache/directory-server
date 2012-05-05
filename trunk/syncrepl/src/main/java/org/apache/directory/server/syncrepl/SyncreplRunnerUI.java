/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.syncrepl;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;
import org.apache.mina.util.AvailablePortFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 *  A simple swing UI to start stop syncrepl consumer.
 *  This class avoids the costly operation of setting up config 
 *  and directory service between start/stop of consumer.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SyncreplRunnerUI implements ActionListener
{
    private SyncreplConfiguration config;

    private SyncReplConsumer agent = new SyncReplConsumer();

    private File workDir;

    private DirectoryService dirService;

    private LdapServer ldapServer;

    private static final Logger LOG = LoggerFactory.getLogger( SyncreplRunnerUI.class.getSimpleName() );

    // UI components
    private JButton btnStart;

    private JButton btnStop;

    private JButton btnCleanStart;

    private EntryInjector entryInjector;

//    private String provServerHost = "192.168.22.105";
//    private int provServerPort = 389;
//    private String provServerBindDn = "cn=Manager,dc=example,dc=com";
    
    private String provServerHost = "localhost";
    private int provServerPort = 10389;
    private String provServerBindDn = "uid=admin,ou=system";
  
    private String provServerPwd = "secret";

    private boolean connected;


    public SyncreplRunnerUI()
    {
        config = new SyncreplConfiguration();
        config.setProviderHost( provServerHost );
        config.setPort( provServerPort );
        config.setReplUserDn( provServerBindDn );
        config.setReplUserPassword( provServerPwd );
        config.setBaseDn( "dc=example,dc=com" );
        config.setFilter( "(objectclass=*)" );
        config.setAttributes( new String[]{ "*", "ref", "entryUUID", "entryCSN" } );
        config.setSearchScope( SearchScope.SUBTREE );
        config.setReplicaId( 1 );
        config.setRefreshPersist( true );
        config.setRefreshInterval( 60 * 1000 );
        agent.setConfig( config );

        workDir = new File( "/Users/kayyagari/Desktop/syncrepl-work" );//new File( System.getProperty( "java.io.tmpdir" ) + "/syncrepl-work" );
    }


    public void start()
    {
        try
        {
            if ( !workDir.exists() )
            {
                workDir.mkdirs();
            }

            startEmbeddedServer( workDir );
            agent.init( dirService );
            agent.connect();

            connected = true;
            agent.prepareSyncSearchRequest();
            agent.startSync();
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to start the embedded server & syncrepl consumer", e );
            throw new RuntimeException( e );
        }
    }


    public void stop()
    {
        try
        {
            LOG.info( "stopping the embedded server" );

            if ( connected )
            {
                agent.disconnet();
            }

            if ( ( dirService != null ) && dirService.isStarted() )
            {
                dirService.shutdown();
                ldapServer.stop();
            }

        }
        catch ( Exception e )
        {
            LOG.error( "Failed to stop", e );
        }

        connected = false;
    }


    public void cleanStart()
    {
        try
        {
            if ( workDir.exists() )
            {
                FileUtils.forceDelete( workDir );
            }
        }
        catch ( Exception e )
        {
            LOG.error( "Failed to delete the work directory", e );
        }

        agent.deleteCookieFile();
        start();
    }


    private void startEmbeddedServer( File workDir )
    {
        try
        {
            dirService = new DefaultDirectoryService();
            dirService.setShutdownHookEnabled( false );
            dirService.setWorkingDirectory( workDir );
            int consumerPort = AvailablePortFinder.getNextAvailable( 1024 );

            initSchema( dirService );
            initSystemPartition( dirService );

            ldapServer = new LdapServer();
            ldapServer.setTransports( new TcpTransport( consumerPort ) );
            ldapServer.setDirectoryService( dirService );
            
            DN suffix = new DN( config.getBaseDn() );
            JdbmPartition partition = new JdbmPartition();
            partition.setSuffix( suffix.getName() );
            partition.setId( "syncrepl" );
            partition.setPartitionDir( new File( workDir, partition.getId() ) );
            partition.setSyncOnWrite( true );
            partition.setSchemaManager( dirService.getSchemaManager() );

            // Add objectClass attribute for the system partition
            Set<Index<?, Entry, Long>> indexedAttrs = new HashSet<Index<?, Entry, Long>>();
            indexedAttrs.add( new JdbmIndex<Object, Entry>( SchemaConstants.ENTRY_UUID_AT ) );
            ( ( JdbmPartition ) partition ).setIndexedAttributes( indexedAttrs );

            partition.initialize();

            dirService.addPartition( partition );

            dirService.startup();

            ldapServer.addExtendedOperationHandler( new StartTlsHandler() );
            ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            ldapServer.start();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    public void show() throws Exception
    {

        btnStart = new JButton( "Start" );
        btnStart.setMnemonic( 'S' );
        btnStart.addActionListener( this );

        btnCleanStart = new JButton( "Clean Start" );
        btnCleanStart.setMnemonic( 'R' );
        btnCleanStart.addActionListener( this );

        btnStop = new JButton( "Stop" );
        btnStop.setMnemonic( 'O' );
        btnStop.setEnabled( false );
        btnStop.addActionListener( this );

        JPanel serverPanel = new JPanel();
        serverPanel.add( btnStart );
        serverPanel.add( btnStop );
        serverPanel.add( btnCleanStart );
        serverPanel.setBorder( new TitledBorder( "Server Controls" ) );

        entryInjector = new EntryInjector( provServerHost, provServerPort, provServerBindDn, provServerPwd );
        entryInjector.setBorder( new TitledBorder( "Entry Injector" ) );
        entryInjector.setConfig( config );

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        frame.setTitle( "Syncrepl consumer UI" );

        frame.getContentPane().add( serverPanel, BorderLayout.NORTH );
        frame.getContentPane().add( entryInjector, BorderLayout.SOUTH );
        
        frame.addWindowListener( new WindowAdapter()
        {
            @Override
            public void windowClosed( WindowEvent e )
            {
                stop();
            }
        } );

        frame.pack();
        frame.setVisible( true );
    }


    public void actionPerformed( ActionEvent e )
    {
        Object src = e.getSource();

        if ( src == btnStart )
        {
            btnStart.setEnabled( false );
            btnCleanStart.setEnabled( false );
            Runnable startTask = new Runnable()
            {
                public void run()
                {
                    start();
                }
            };
            new Thread( startTask ).start();
            btnStop.setEnabled( true );
        }
        else if ( src == btnStop )
        {
            btnStop.setEnabled( false );
            Runnable stopTask = new Runnable()
            {
                public void run()
                {
                    stop();
                }
            };

            new Thread( stopTask ).start();

            btnStart.setEnabled( true );
            btnCleanStart.setEnabled( true );
        }
        else if ( src == btnCleanStart )
        {
            btnCleanStart.setEnabled( false );
            btnStart.setEnabled( false );

            Runnable cleanStartTask = new Runnable()
            {
                public void run()
                {
                    cleanStart();
                }
            };

            new Thread( cleanStartTask ).start();
            btnStop.setEnabled( true );
        }
    }


    private void initSchema( DirectoryService service ) throws Exception
    {
        SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

        // Init the LdifPartition
        LdifPartition ldifPartition = new LdifPartition();
        String workingDirectory = service.getWorkingDirectory().getPath();
        ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

        // Extract the schema on disk (a brand new one) and load the registries
        File schemaRepository = new File( workingDirectory, "schema" );
        SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
        extractor.extractOrCopy( true );

        schemaPartition.setWrappedPartition( ldifPartition );

        SchemaLoader loader = new LdifSchemaLoader( schemaRepository );
        SchemaManager schemaManager = new DefaultSchemaManager( loader );
        service.setSchemaManager( schemaManager );

        // We have to load the schema now, otherwise we won't be able
        // to initialize the Partitions, as we won't be able to parse 
        // and normalize their suffix DN
        schemaManager.loadAllEnabled();

        schemaPartition.setSchemaManager( schemaManager );

        List<Throwable> errors = schemaManager.getErrors();

        if ( errors.size() != 0 )
        {
            throw new Exception( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
        }
    }


    private void initSystemPartition( DirectoryService service ) throws Exception
    {
        // change the working directory to something that is unique
        // on the system and somewhere either under target directory
        // or somewhere in a temp area of the machine.

        // Inject the System Partition
        Partition systemPartition = new JdbmPartition();
        systemPartition.setId( "system" );
        ( ( JdbmPartition ) systemPartition ).setCacheSize( 500 );
        systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
        systemPartition.setSchemaManager( service.getSchemaManager() );
        ( ( JdbmPartition ) systemPartition ).setPartitionDir( new File( service.getWorkingDirectory(), "system" ) );

        // Add objectClass attribute for the system partition
        Set<Index<?, Entry, Long>> indexedAttrs = new HashSet<Index<?, Entry, Long>>();
        indexedAttrs.add( new JdbmIndex<Object, Entry>( SchemaConstants.OBJECT_CLASS_AT ) );
        ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );

        service.setSystemPartition( systemPartition );
    }


    public static void main( String[] args )
    {
        final SyncreplRunnerUI runnerUi = new SyncreplRunnerUI();
        try
        {
            runnerUi.show();
        }
        catch ( Exception e )
        {
            e.printStackTrace();

            JOptionPane.showMessageDialog( null, e.getMessage(), "Failed to start Syncrepl test UI",
                JOptionPane.ERROR_MESSAGE );
            System.exit( 1 );
        }
    }
}
