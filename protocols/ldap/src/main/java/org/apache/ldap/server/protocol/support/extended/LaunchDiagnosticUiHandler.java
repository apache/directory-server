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


import java.util.Iterator;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;

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
import org.apache.ldap.server.partition.impl.btree.gui.MainFrame;
import org.apache.ldap.server.protocol.ExtendedOperationHandler;
import org.apache.ldap.server.protocol.SessionRegistry;
import org.apache.mina.common.IoSession;


public class LaunchDiagnosticUiHandler implements ExtendedOperationHandler
{
    public String getOid()
    {
        return LaunchDiagnosticUiRequest.OID;
    }

    
    public void handleExtendedOperation( IoSession session, SessionRegistry registry, ExtendedRequest req ) throws NamingException 
    {
        LdapContext ctx = registry.getLdapContext( session, null, false );
        ctx = ( LdapContext ) ctx.lookup( "" );
        
        if ( ctx instanceof ServerLdapContext )
        {
            ServerLdapContext slc = ( ServerLdapContext ) ctx;
            DirectoryService service = slc.getService();
            
            if ( ! slc.getPrincipal().getName().equalsIgnoreCase( DirectoryPartitionNexus.ADMIN_PRINCIPAL ) )
            {
                session.write( new LaunchDiagnosticUiResponse( req.getMessageId(), ResultCodeEnum.INSUFFICIENTACCESSRIGHTS ) );
                return;
            }

            session.write( new LaunchDiagnosticUiResponse( req.getMessageId() ) );

            DirectoryPartitionNexus nexus = service.getConfiguration().getPartitionNexus();
            Iterator list = nexus.listSuffixes( true );
            while ( list.hasNext() )
            {
                LdapName dn = new LdapName( ( String ) list.next() );
                DirectoryPartition partition = nexus.getPartition( dn );
                if ( partition instanceof BTreeDirectoryPartition )
                {
                    BTreeDirectoryPartition btPartition = ( BTreeDirectoryPartition ) partition;
                    MainFrame frame = new MainFrame( btPartition, btPartition.getSearchEngine() );
                    frame.setVisible( true );
                }
            }
            
            return;
        }

        session.write( new LaunchDiagnosticUiResponse( req.getMessageId(), ResultCodeEnum.OPERATIONSERROR ) );
    }
}
