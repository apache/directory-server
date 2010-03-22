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
package org.apache.directory.server.ldap.handlers;

 
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.ldap.LdapSession;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A single reply handler for {@link InternalModifyDnRequest}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664302 $
 */
public class ModifyDnHandler extends LdapRequestHandler<InternalModifyDnRequest>
{
    private static final Logger LOG = LoggerFactory.getLogger( ModifyDnHandler.class );

    
    /**
     * Deal with a ModifyDN request received from a client.
     *
     * A ModifyDN operation has more than one semantic, depending on its parameters.
     *
     * In any case, the first argument is the DN entry to be changed. We then
     * have the new relative DN for this entry.
     *
     * Two other arguments can be provided :
     * - deleteOldRdn : if the old RDN attributes should be removed from the
     * new entry or not (for instance, if the old RDN was cn=acme, and the new
     * one is sn=acme, then we may have to remove the cn: acme from the attributes
     * list)
     * - newSuperior : this is a move operation. The entry is removed from its
     * current location, and created in the new one.
     */
    public void handle( LdapSession session, InternalModifyDnRequest req )
    {
        InternalLdapResult result = req.getResultResponse().getLdapResult();
        LOG.debug( "Handling modify dn request while ignoring referrals: {}", req );

        if ( req.getName().isEmpty() )
        {
            // it is not allowed to modify the name of the Root DSE
            String msg = "Modify DN is not allowed on Root DSE.";
            result.setResultCode( ResultCodeEnum.PROTOCOL_ERROR );
            result.setErrorMessage( msg );
            session.getIoSession().write( req.getResultResponse() );
            return;
        }
        
        try
        {
            DN newRdn = new DN( req.getNewRdn().getName() );
            newRdn.normalize( session.getCoreSession().getDirectoryService()
                .getSchemaManager().getNormalizerMapping() );
            
            DN oldRdn = new DN( req.getName().getRdn().getName() );
            oldRdn.normalize( session.getCoreSession().getDirectoryService()
                .getSchemaManager().getNormalizerMapping() );
            
            boolean rdnChanged = req.getNewRdn() != null && 
                ! newRdn.getNormName().equals( oldRdn.getNormName() );
            
            CoreSession coreSession = session.getCoreSession();
            
            if ( rdnChanged )
            {
                if ( req.getNewSuperior() != null )
                {
                    coreSession.moveAndRename( req );
                }
                else
                {
                    coreSession.rename( req );
                }
            }
            else if ( req.getNewSuperior() != null )
            {
                req.setNewRdn( null );
                coreSession.move( req );
            }
            else
            {
                result.setErrorMessage( "Attempt to move entry onto itself." );
                result.setResultCode( ResultCodeEnum.ENTRY_ALREADY_EXISTS );
                result.setMatchedDn( req.getName() );
                session.getIoSession().write( req.getResultResponse() );
                return;
            }

            result.setResultCode( ResultCodeEnum.SUCCESS );
            session.getIoSession().write( req.getResultResponse() );
        }
        catch ( Exception e )
        {
            handleException( session, req, e );
        }
    }
}