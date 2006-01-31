/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.protocol.support.extended;


import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.swing.JFrame;

import org.apache.ldap.common.message.ExtendedRequest;
import org.apache.ldap.common.message.ResultCodeEnum;
import org.apache.ldap.common.message.extended.LaunchDiagnosticUiRequest;
import org.apache.ldap.common.message.extended.LaunchDiagnosticUiResponse;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.DirectoryService;
import org.apache.ldap.server.jndi.ServerLdapContext;
import org.apache.ldap.server.partition.DirectoryPartition;
import org.apache.ldap.server.partition.DirectoryPartitionNexus;
import org.apache.ldap.server.partition.impl.btree.BTreeDirectoryPartition;
import org.apache.ldap.server.partition.impl.btree.gui.PartitionFrame;
import org.apache.ldap.server.protocol.ExtendedOperationHandler;
import org.apache.ldap.server.protocol.LdapProtocolProvider;
import org.apache.ldap.server.protocol.SessionRegistry;
import org.apache.ldap.server.protocol.gui.SessionsFrame;
import org.apache.mina.common.IoSession;
import org.apache.mina.registry.Service;
import org.apache.mina.registry.ServiceRegistry;


public class LaunchDiagnosticUiHandler implements ExtendedOperationHandler
{
    private Service ldapService;
    private ServiceRegistry minaRegistry;
    private LdapProtocolProvider ldapProvider;
    
    
    public String getOid()
    {
        return LaunchDiagnosticUiRequest.EXTENSION_OID;
    }

    
    public void handleExtendedOperation( IoSession requestor, SessionRegistry registry, ExtendedRequest req ) throws NamingException 
    {
        LdapContext ctx = registry.getLdapContext( requestor, null, false );
        ctx = ( LdapContext ) ctx.lookup( "" );
        
        if ( ctx instanceof ServerLdapContext )
        {
            ServerLdapContext slc = ( ServerLdapContext ) ctx;
            DirectoryService service = slc.getService();
            
            if ( ! slc.getPrincipal().getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) )
            {
                requestor.write( new LaunchDiagnosticUiResponse( req.getMessageId(), ResultCodeEnum.INSUFFICIENTACCESSRIGHTS ) );
                return;
            }

            requestor.write( new LaunchDiagnosticUiResponse( req.getMessageId() ) );

            DirectoryPartitionNexus nexus = service.getConfiguration().getPartitionNexus();
            Iterator list = nexus.listSuffixes( true );
            int launchedWindowCount = 0;
            while ( list.hasNext() )
            {
                LdapName dn = new LdapName( ( String ) list.next() );
                DirectoryPartition partition = nexus.getPartition( dn );
                if ( partition instanceof BTreeDirectoryPartition )
                {
                    BTreeDirectoryPartition btPartition = ( BTreeDirectoryPartition ) partition;
                    PartitionFrame frame = new PartitionFrame( btPartition, btPartition.getSearchEngine() );
                    Point pos = getCenteredPosition( frame );
                    pos.y = launchedWindowCount*20 + pos.y;
                    double multiplier = getAspectRatio() * 20.0;
                    pos.x =  ( int ) ( launchedWindowCount * multiplier ) + pos.x;
                    frame.setLocation( pos );
                    frame.setVisible( true );
                    launchedWindowCount++;
                }
            }
            
            SessionsFrame sessions = new SessionsFrame();
            sessions.setMinaRegistry( minaRegistry );
            sessions.setLdapService( ldapService );
            sessions.setRequestor( requestor );
            sessions.setLdapProvider( ldapProvider.getHandler() );
            Point pos = getCenteredPosition( sessions );
            pos.y = launchedWindowCount*20 + pos.y;
            double multiplier = getAspectRatio() * 20.0;
            pos.x =  ( int ) ( launchedWindowCount * multiplier ) + pos.x;
            sessions.setLocation( pos );
            sessions.setVisible( true );
            return;
        }

        requestor.write( new LaunchDiagnosticUiResponse( req.getMessageId(), ResultCodeEnum.OPERATIONSERROR ) );
    }
    
    
    public double getAspectRatio()
    {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        return ( double ) screenSize.getWidth() / ( double ) screenSize.getHeight();
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


    public void setLdapService( Service ldapService )
    {
        this.ldapService = ldapService;
    }


    public Service getLdapService()
    {
        return ldapService;
    }


    public void setServiceRegistry( ServiceRegistry minaRegistry )
    {
        this.minaRegistry = minaRegistry;
    }


    public ServiceRegistry getMinaRegistry()
    {
        return minaRegistry;
    }


    public void setLdapProvider( LdapProtocolProvider ldapProvider )
    {
        this.ldapProvider = ldapProvider;
    }
}
