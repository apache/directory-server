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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.handlers.extended.StartTlsHandler;
import org.apache.directory.server.ldap.handlers.extended.StoredProcedureExtendedOperationHandler;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.LdapDN;
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
 * @version $Rev$, $Date$
 */
public class SyncreplRunnerUI implements ActionListener
{
    private SyncreplConfiguration config;

    private SyncReplConsumer agent = new SyncReplConsumer();

    private File workDir;

    private DirectoryService dirService;

    private LdapServer ldapServer;

    private static final Logger LOG = LoggerFactory.getLogger( SyncreplRunnerUI.class );

    // UI components
    private JButton btnStart;

    private JButton btnStop;

    private JButton btnCleanStart;

    private EntryInjector entryInjector;

    private String provServerHost = "localhost";
    private int provServerPort = 389;
    private String provServerBindDn = "cn=admin,dc=nodomain";
    private String provServerPwd = "secret";

    private boolean connected;


    public SyncreplRunnerUI()
    {
        config = new SyncreplConfiguration();
        config.setProviderHost( "localhost" );
        config.setPort( 389 );
        config.setBindDn( "cn=admin,dc=nodomain" );
        config.setCredentials( "secret" );
        config.setBaseDn( "dc=test,dc=nodomain" );
        config.setFilter( "(objectclass=*)" );
        config.setAttributes( "*,+" );
        config.setSearchScope( SearchScope.SUBTREE.getJndiScope() );
        config.setReplicaId( 1 );
        agent.setConfig( config );

        workDir = new File( System.getProperty( "java.io.tmpdir" ) + "/syncrepl-work" );
    }


    public void start()
    {
        try
        {
            if ( !workDir.exists() )
            {
                workDir.mkdirs();
            }

            dirService = startEmbeddedServer( workDir );
            agent.init( dirService );
            agent.bind();

            entryInjector.enable( true );

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
                entryInjector.enable( false );
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


    private DirectoryService startEmbeddedServer( File workDir )
    {
        try
        {
            DefaultDirectoryService dirService = new DefaultDirectoryService();
            dirService.setShutdownHookEnabled( false );
            dirService.setWorkingDirectory( workDir );
            int consumerPort = AvailablePortFinder.getNextAvailable( 1024 );
            ldapServer = new LdapServer();
            ldapServer.setTransports( new TcpTransport( consumerPort ) );
            ldapServer.setDirectoryService( dirService );

            LdapDN suffix = new LdapDN( config.getBaseDn() );
            JdbmPartition partition = new JdbmPartition();
            partition.setSuffix( suffix.getUpName() );
            partition.setId( "syncrepl" );
            partition.setSyncOnWrite( true );
            partition.init( dirService );

            dirService.addPartition( partition );

            dirService.startup();

            ldapServer.addExtendedOperationHandler( new StartTlsHandler() );
            ldapServer.addExtendedOperationHandler( new StoredProcedureExtendedOperationHandler() );

            ldapServer.start();
            return dirService;
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return null;
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
        entryInjector.enable( false );
        entryInjector.setBorder( new TitledBorder( "Entry Injector" ) );

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
            SwingUtilities.invokeLater( new Runnable()
            {
                public void run()
                {
                    start();
                }
            } );
            btnStop.setEnabled( true );
        }
        else if ( src == btnStop )
        {
            btnStop.setEnabled( false );
            SwingUtilities.invokeLater( new Runnable()
            {
                public void run()
                {
                    stop();
                }
            } );

            btnStart.setEnabled( true );
            btnCleanStart.setEnabled( true );
        }
        else if ( src == btnCleanStart )
        {
            btnCleanStart.setEnabled( false );
            btnStart.setEnabled( false );

            SwingUtilities.invokeLater( new Runnable()
            {
                public void run()
                {
                    cleanStart();
                }
            } );
            btnStop.setEnabled( true );
        }
    }


    public static void main( String[] args )
    {
        SyncreplRunnerUI runnerUi = new SyncreplRunnerUI();
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
