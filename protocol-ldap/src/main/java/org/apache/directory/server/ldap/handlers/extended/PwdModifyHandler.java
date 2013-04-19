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


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.directory.api.ldap.extras.extended.PwdModifyRequest;
import org.apache.directory.api.ldap.extras.extended.PwdModifyResponse;
import org.apache.directory.api.ldap.extras.extended.PwdModifyResponseImpl;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.ldap.ExtendedOperationHandler;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.LdapSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An handler to manage PwdModifyRequest. Users can send a pwdModify request
 * for their own passwords, or for another people password. Only admin can 
 * change someone else password without having to provide the original password.
 * Here rae the different use cases : <br/>
 * <ul>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PwdModifyHandler implements ExtendedOperationHandler<PwdModifyRequest, PwdModifyResponse>
{
    private static final Logger LOG = LoggerFactory.getLogger( PwdModifyHandler.class );
    public static final Set<String> EXTENSION_OIDS;

    static
    {
        Set<String> set = new HashSet<String>( 2 );
        set.add( PwdModifyRequest.EXTENSION_OID );
        set.add( PwdModifyResponse.EXTENSION_OID );
        EXTENSION_OIDS = Collections.unmodifiableSet( set );
    }


    /**
     * {@inheritDoc}
     */
    public String getOid()
    {
        return PwdModifyRequest.EXTENSION_OID;
    }


    /**
     * {@inheritDoc}
     */
    public void handleExtendedOperation( LdapSession requestor, PwdModifyRequest req ) throws Exception
    {
        // Grab the adminSession, we might need it later
        DirectoryService service = requestor.getLdapServer().getDirectoryService();
        CoreSession adminSession = service.getAdminSession();

        // First check if the user is bound or not
        if ( requestor.isAuthenticated() )
        {

        }
        else
        {
            // The user is not authenticated : we have to use the provided userIdentity
            // and the oldPassword to check if the user is present
            String userIdentity = Strings.utf8ToString( req.getUserIdentity() );

            Dn userDn = null;

            try
            {
                userDn = service.getDnFactory().create( userIdentity );
            }
            catch ( LdapInvalidDnException lide )
            {
                // The userIdentity is not a DN : return with an error code.
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_DN_SYNTAX ) );

                return;
            }

            byte[] oldPassword = req.getOldPassword();
            byte[] newPassword = req.getNewPassword();

            BindOperationContext bindContext = new BindOperationContext( adminSession );
            bindContext.setDn( userDn );
            bindContext.setCredentials( oldPassword );

            try
            {
                service.getOperationManager().bind( bindContext );
            }
            catch ( LdapException le )
            {
                // We can't bind with the provided information : we thus can't 
                // change the password...
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_CREDENTIALS ) );

                return;
            }

            // Ok, we were able to bind using the userIdentity and the password. Let's 
            // modify the password now
            ModifyOperationContext modifyContext = new ModifyOperationContext( adminSession );
            modifyContext.setDn( userDn );
            List<Modification> modifications = new ArrayList<Modification>();
            Modification modification = new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE,
                SchemaConstants.USER_PASSWORD_AT, newPassword );
            modifications.add( modification );
            modifyContext.setModItems( modifications );

            try
            {
                service.getOperationManager().modify( modifyContext );

                // Ok, all done
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.SUCCESS ) );
            }
            catch ( LdapException le )
            {
                // We can't modify the password
                requestor.getIoSession().write( new PwdModifyResponseImpl(
                    req.getMessageId(), ResultCodeEnum.INVALID_CREDENTIALS ) );

                return;
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public static PwdModifyResponse getPwdModifyResponse()
    {
        // build the PwdModifyResponse message with replicationContexts
        return new PwdModifyResponseImpl();
    }


    /**
     * {@inheritDoc}
     */
    public Set<String> getExtensionOids()
    {
        return EXTENSION_OIDS;
    }


    /**
     * {@inheritDoc}
     */
    public void setLdapServer( LdapServer ldapServer )
    {
    }
}
