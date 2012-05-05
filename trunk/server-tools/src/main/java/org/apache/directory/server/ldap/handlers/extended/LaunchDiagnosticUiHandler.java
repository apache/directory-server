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
package org.apache.directory.server.ldap.handlers.extended;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JFrame;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.server.core.partition.impl.btree.gui.PartitionFrame;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.server.ldap.gui.SessionsFrame;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.message.ExtendedRequest;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.extended.LaunchDiagnosticUiRequest;
import org.apache.directory.shared.ldap.message.extended.LaunchDiagnosticUiResponse;
import org.apache.directory.shared.ldap.name.DN;


/**
 * @todo  Missing Javadoc
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LaunchDiagnosticUiHandler implements ExtendedOperationHandler
{
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 3 );
        set.add( LaunchDiagnosticUiRequest.EXTENSION_OID );
        set.add( LaunchDiagnosticUiResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }

    private LdapServer ldapServer;


    public String getOid()
    {
        return LaunchDiagnosticUiRequest.EXTENSION_OID;
    }


    // This will suppress PMD.EmptyCatchBlock warnings in this method
    @SuppressWarnings("PMD.EmptyCatchBlock")
    public void handleExtendedOperation( LdapSession requestor, ExtendedRequest req )
        throws Exception
    {
        DirectoryService service = requestor.getCoreSession().getDirectoryService();
        
        if ( ! requestor.getCoreSession().isAnAdministrator() )
        {
            requestor.getIoSession().write( new LaunchDiagnosticUiResponse( req.getMessageId(),
                ResultCodeEnum.INSUFFICIENT_ACCESS_RIGHTS ) );
            return;
        }

        requestor.getIoSession().write( new LaunchDiagnosticUiResponse( req.getMessageId() ) );

        PartitionNexus nexus = service.getPartitionNexus();
        DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED, service.getSchemaManager() );
        LdapPrincipal principal = new LdapPrincipal( adminDn, AuthenticationLevel.STRONG );
        Set<String> suffixes = nexus.listSuffixes();
        int launchedWindowCount = 0;
            
        for ( String suffix:suffixes )
        {
            DN dn = new DN( suffix );
            Partition partition = nexus.getPartition( dn );
            
            if ( partition instanceof BTreePartition )
            {
                try
                {
                    BTreePartition btPartition = ( BTreePartition ) partition;
                    // TODO : If a partition does not have an initial entry associated, we wil:
                    // get a NPE : this has to be fixed.
                    PartitionFrame frame = new PartitionFrame( btPartition, service.getSchemaManager() );
                    Point pos = getCenteredPosition( frame );
                    pos.y = launchedWindowCount * 20 + pos.y;
                    double multiplier = getAspectRatio() * 20.0;
                    pos.x = ( int ) ( launchedWindowCount * multiplier ) + pos.x;
                    frame.setLocation( pos );
                    frame.setVisible( true );
                    launchedWindowCount++;
                }
                catch ( Exception e )
                {
                    // Continue
                }
            }
        }

        SessionsFrame sessions = new SessionsFrame( ldapServer );
        sessions.setRequestor( requestor.getIoSession() );
        sessions.setLdapProvider( ldapServer.getHandler() );
        Point pos = getCenteredPosition( sessions );
        pos.y = launchedWindowCount * 20 + pos.y;
        double multiplier = getAspectRatio() * 20.0;
        pos.x = ( int ) ( launchedWindowCount * multiplier ) + pos.x;
        sessions.setLocation( pos );
        sessions.setVisible( true );
    }


    public double getAspectRatio()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        return screenSize.getWidth() / screenSize.getHeight();
    }


    public Point getCenteredPosition( JFrame frame )
    {
        Point pt = new Point();
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        pt.x = ( screenSize.width - frame.getWidth() ) / 2;
        pt.y = ( screenSize.height - frame.getHeight() ) / 2;
        return pt;
    }


    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    public void setLdapServer( LdapServer ldapServer )
    {
        this.ldapServer = ldapServer;
    }
}
