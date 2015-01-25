/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.config.listener;


import static org.apache.directory.server.core.api.InterceptorEnum.AUTHENTICATION_INTERCEPTOR;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.config.ConfigPartitionReader;
import org.apache.directory.server.config.beans.PasswordPolicyBean;
import org.apache.directory.server.config.builder.ServiceBuilder;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.authn.ppolicy.PasswordPolicyConfiguration;
import org.apache.directory.server.core.api.event.DirectoryListenerAdapter;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authn.ppolicy.PpolicyConfigContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A listener for handling the config partition changes.
 * 
 * Note: currently handles password policy related configuration changes only.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigChangeListener extends DirectoryListenerAdapter
{
    /** the config parition reader */
    private ConfigPartitionReader cpReader;

    /** the directory service */
    private DirectoryService directoryService;

    /** container holding the current active password policy configurations */
    private PpolicyConfigContainer ppolicyConfigContainer;

    /** the root DN of password policy configurations */
    private Dn ppolicyConfigDnRoot;

    private static final String PPOLICY_OC_NAME = "ads-passwordPolicy";

    // attribute holding the value of #PPOLICY_OC_NAME
    private Attribute AT_PWDPOLICY;
    
    
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ConfigChangeListener.class );

    /**
     * 
     * Creates a new instance of ConfigChangeListener.
     *
     * @param cpReader the configuration reader
     * @param directoryService the DirectoryService instance
     * @throws LdapException
     */
    public ConfigChangeListener( ConfigPartitionReader cpReader, DirectoryService directoryService )
        throws LdapException
    {
        this.cpReader = cpReader;
        this.directoryService = directoryService;

        SchemaManager schemaManager = directoryService.getSchemaManager();

        ppolicyConfigDnRoot = new Dn(
            "ou=passwordPolicies,ads-interceptorId=authenticationInterceptor,ou=interceptors,ads-directoryServiceId=default,ou=config" );
        ppolicyConfigDnRoot.apply( schemaManager );

        AuthenticationInterceptor authInterceptor = ( AuthenticationInterceptor ) directoryService
            .getInterceptor( AUTHENTICATION_INTERCEPTOR.getName() );
        ppolicyConfigContainer = authInterceptor.getPwdPolicyContainer();

        AttributeType ocType = schemaManager.lookupAttributeTypeRegistry( SchemaConstants.OBJECT_CLASS_AT );
        AT_PWDPOLICY = new DefaultAttribute( ocType, PPOLICY_OC_NAME );
    }


    @Override
    public void entryAdded( AddOperationContext addContext )
    {
        Entry entry = addContext.getEntry();
        updatePasswordPolicy( entry, false );
    }


    @Override
    public void entryDeleted( DeleteOperationContext deleteContext )
    {
        Entry entry = deleteContext.getEntry();
        updatePasswordPolicy( entry, true );
    }


    @Override
    public void entryModified( ModifyOperationContext modifyContext )
    {
        Entry entry = modifyContext.getAlteredEntry();
        updatePasswordPolicy( entry, false );
    }


    /**
     * Updates the password policy represented by the given configuration entry
     * 
     * @param entry the password policy configuration entry
     * @param deleted flag to detect if this is a deleted entry
     */
    private void updatePasswordPolicy( Entry entry, boolean deleted )
    {
        Dn dn = entry.getDn();

        if ( !dn.isDescendantOf( ppolicyConfigDnRoot ) )
        {
            return;
        }

        if ( !entry.contains( AT_PWDPOLICY ) )
        {
            return;
        }

        if ( deleted )
        {
            LOG.debug( "Deleting ppolicy config {}", dn );
            ppolicyConfigContainer.removePolicyConfig( dn );
            return;
        }
        
        PasswordPolicyBean bean = null;
        
        try
        {
            bean = ( PasswordPolicyBean ) cpReader.readConfig( entry );
        }
        catch( Exception e )
        {
            LOG.warn( "Failed to read the updated ppolicy configuration from {}", dn );
            LOG.warn("", e);
            return;
        }

        if( bean.isDisabled() )
        {
            LOG.debug( "Deleting disabled ppolicy config {}", dn );
            ppolicyConfigContainer.removePolicyConfig( dn );
        }
        else
        {
            PasswordPolicyConfiguration updated = ServiceBuilder.createPwdPolicyConfig( bean );
            
            PasswordPolicyConfiguration existing = ppolicyConfigContainer.getPolicyConfig( dn );
            
            if( existing == null )
            {
                LOG.debug( "Adding ppolicy config {}", dn );
            }
            else
            {
                LOG.debug( "Updating ppolicy config {}", dn );
            }
            
            ppolicyConfigContainer.addPolicy( dn, updated );
        }
    }
}
