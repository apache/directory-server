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


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.Semaphore;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.message.SearchResponse;
import org.apache.directory.ldap.client.api.message.SearchResultEntry;
import org.apache.directory.shared.ldap.cursor.Cursor;
import org.apache.directory.shared.ldap.entry.DefaultEntry;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.filter.SearchScope;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * A utility class to inject entries into the syncrepl provider server.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryInjector extends JPanel implements ActionListener
{
    private JButton btnAdd;
    private JButton btnPause;
    private JButton btnKeepAdding;
    private JButton btnDelete;
    private JButton btnRandDelete;
    private JTextField txtDn;
    private RunnerThread runner = new RunnerThread();

    private LdapConnection connection;

    private SyncreplConfiguration config;
    
    private static final Logger LOG = LoggerFactory.getLogger( EntryInjector.class );


    public EntryInjector( String host, int port, String bindDn, String pwd ) throws Exception
    {
        connection = new LdapNetworkConnection( host, port );
        connection.bind( bindDn, pwd );

        addcomponents();
    }


    public void addEntry()
    {

        try
        {
            String cn = "entry-" + System.currentTimeMillis();
            DN dn = new DN( "cn=" + cn + "," + config.getBaseDn() );
            Entry entry = new DefaultEntry();
            entry.add( "objectclass", "person" );
            entry.add( "cn", cn );
            entry.add( "sn", cn );
            entry.setDn( dn );

            LOG.debug( "adding entry with dn: {}" + dn );
            connection.add( entry );
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    public void deleteEntry( String dn )
    {
        try
        {
            if( dn != null && dn.trim().length() > 0 )
            {
                connection.delete( dn );
            }
            else if( dn == null )
            {
                Cursor<SearchResponse> cursor = connection.search( config.getBaseDn(), config.getFilter(), config.getSearchScope(), config.getAttributes() );
                cursor.beforeFirst();
                if( cursor.next() && cursor.next() ) // to skip the baseDN
                {
                    SearchResponse res = cursor.get();
                    if( res instanceof SearchResultEntry )
                    {
                        connection.delete( ( ( SearchResultEntry ) res ).getEntry().getDn()  );
                    }
                }
                
                cursor.close();
            }
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    
    public void close()
    {
        try
        {
            runner.stopThread();
            connection.unBind();
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }
    }


    private void addcomponents()
    {
        btnAdd = new JButton( "Add" );
        btnAdd.addActionListener( this );
        add( btnAdd );

        btnPause = new JButton( "Pause" );
        btnPause.addActionListener( this );
        btnPause.setEnabled( false );
        add( btnPause );

        btnKeepAdding = new JButton( "Keep Adding" );
        btnKeepAdding.addActionListener( this );
        add( btnKeepAdding );

        JPanel innerPanel = new JPanel();
        innerPanel.setBorder( new TitledBorder( "Delete Entry" ) );
        
        innerPanel.add( new JLabel( "DN:" ) );
        
        txtDn = new JTextField( 20 );
        txtDn.addActionListener( this );
        innerPanel.add( txtDn );

        btnDelete = new JButton( "Delete" );
        btnDelete.addActionListener( this );
        innerPanel.add( btnDelete );
        
        btnRandDelete = new JButton( "Delete Random" );
        btnRandDelete.addActionListener( this );
        innerPanel.add( btnRandDelete );
         
        add( innerPanel );
    }


    public void actionPerformed( ActionEvent e )
    {
        Object src = e.getSource();

        if ( src == btnAdd )
        {
            addEntry();
        }
        else if ( src == btnPause )
        {
            runner.pause( true );
            btnPause.setEnabled( false );
            btnKeepAdding.setEnabled( true );
        }
        else if ( src == btnKeepAdding )
        {
            if ( !runner.isRunning() )
            {
                runner.start();
            }
            else
            {
                runner.pause( false );
            }
            btnPause.setEnabled( true );
            btnKeepAdding.setEnabled( false );
        }
        else if ( src == btnRandDelete )
        {
            deleteEntry( null );
        }
        else if ( src == btnDelete )
        {
            deleteEntry( txtDn.getText() );
        }
    }

    class RunnerThread extends Thread
    {
        private boolean stop = false;
        private boolean running;

        private Semaphore mutex = new Semaphore( 1 );


        @Override
        public void run()
        {
            running = true;

            while ( !stop )
            {
                try
                {
                    mutex.acquire();
                    addEntry();
                    mutex.release();

                    Thread.sleep( 1000 );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }


        public boolean isRunning()
        {
            return running;
        }


        public void pause( boolean pause )
        {
            try
            {
                if ( pause )
                {
                    mutex.acquire();
                }
                else
                {
                    mutex.release();
                }
            }
            catch ( Exception e )
            {
                e.printStackTrace();
            }
        }


        public void stopThread()
        {
            stop = true;
        }

    }


    public void setConfig( SyncreplConfiguration config )
    {
        this.config = config;
    }


    public void enable( boolean enable )
    {
        btnAdd.setEnabled( enable );
        btnKeepAdding.setEnabled( enable );
        btnDelete.setEnabled( enable );
        btnRandDelete.setEnabled( enable );
        txtDn.setEnabled( enable );
    }
}