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
package org.apache.directory.server.core.event;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.event.DirectoryListener;
import org.apache.directory.server.core.api.event.EventService;
import org.apache.directory.server.core.api.event.NotificationCriteria;
import org.apache.directory.server.core.api.event.RegistrationEntry;
import org.apache.directory.server.core.api.normalization.FilterNormalizingVisitor;
import org.apache.directory.shared.ldap.model.filter.ExprNode;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.ldap.model.schema.normalizers.ConcreteNameComponentNormalizer;
import org.apache.directory.shared.ldap.model.schema.normalizers.NameComponentNormalizer;

/**
 * A class implementing the EventService interface. It stores all the Listener 
 * associated with a DirectoryService.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
class DefaultEventService implements EventService
{
    /** The list of RegistrationEntries being registered */
    private List<RegistrationEntry> registrations = new CopyOnWriteArrayList<RegistrationEntry>();
    
    /** The DirectoryService instance */
    private DirectoryService directoryService;
    
    /** A normalizer used for filters */
    private FilterNormalizingVisitor filterNormalizer;

    /**
     * Create an instance of EventService
     * @param directoryService The associated DirectoryService
     * @param registrations The list of Registrations
     */
    public DefaultEventService( DirectoryService directoryService )
    {
        this.directoryService= directoryService;
        SchemaManager schemaManager = directoryService.getSchemaManager();
        NameComponentNormalizer ncn = new ConcreteNameComponentNormalizer( schemaManager );
        filterNormalizer = new FilterNormalizingVisitor( ncn, schemaManager );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void addListener( DirectoryListener listener, NotificationCriteria criteria ) throws Exception
    {
        criteria.getBase().apply( directoryService.getSchemaManager() );
        ExprNode result = ( ExprNode ) criteria.getFilter().accept( filterNormalizer );
        criteria.setFilter( result );
        registrations.add( new RegistrationEntry( listener, criteria ) );
    }


    /**
     * {@inheritDoc}
     */
    public void removeListener( DirectoryListener listener )
    {
        for ( RegistrationEntry entry : registrations )
        {
            if ( entry.getListener() == listener )
            {
                registrations.remove( entry );
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    public List<RegistrationEntry> getRegistrationEntries()
    {
        return Collections.unmodifiableList( registrations );
    }
}
