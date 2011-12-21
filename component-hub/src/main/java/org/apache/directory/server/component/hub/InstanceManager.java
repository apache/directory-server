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
package org.apache.directory.server.component.hub;


import org.apache.directory.server.component.ADSComponent;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.message.SearchRequest;
import org.apache.directory.shared.ldap.model.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.model.message.SearchScope;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class InstanceManager
{

    /*
     * Logger
     */
    private final Logger LOG = LoggerFactory.getLogger( InstanceManager.class );


    public void registerComponent( ADSComponent component )
    {
        
    }


    /**
     * Register inner DirectoryListener class with DirectoryService
     * DirectoryService reference must have its EventService set by EventInterceptor initialization.
     *
     * @param ads DirectoryService reference to register listener.
     */
    public void registerWithDirectoryService( DirectoryService ads )
    {
        try
        {
            SearchRequest sr = new SearchRequestImpl()
                .setBase( new Dn( "ou=config" ) )
                .setScope( SearchScope.SUBTREE )
                .setFilter( "(objectClass=*)" );

            NotificationCriteria nfCriteria = new NotificationCriteria( sr );
            ads.getEventService().addListener( listener, nfCriteria );
        }
        catch ( LdapException e )
        {
            LOG.info( "Ldap exception while creating SearchRequest" );
            e.printStackTrace();
        }
        catch ( Exception e )
        {
            LOG.info( "Exception while registering with EventService" );
            e.printStackTrace();
        }

    }

    private DirectoryListener listener = new DirectoryListener()
    {

        @Override
        public void entryRenamed( RenameOperationContext renameContext )
        {
            // TODO Auto-generated method stub

        }


        @Override
        public void entryMovedAndRenamed( MoveAndRenameOperationContext moveAndRenameContext )
        {
            // TODO Auto-generated method stub

        }


        @Override
        public void entryMoved( MoveOperationContext moveContext )
        {
            // TODO Auto-generated method stub

        }


        @Override
        public void entryModified( ModifyOperationContext modifyContext )
        {
            // TODO Auto-generated method stub

        }


        @Override
        public void entryDeleted( DeleteOperationContext deleteContext )
        {
            // TODO Auto-generated method stub

        }


        @Override
        public void entryAdded( AddOperationContext addContext )
        {
            // TODO Auto-generated method stub

        }
    };
}
