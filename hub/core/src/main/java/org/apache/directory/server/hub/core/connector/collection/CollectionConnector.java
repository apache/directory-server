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

package org.apache.directory.server.hub.core.connector.collection;


import org.apache.directory.server.hub.api.ComponentHub;
import org.apache.directory.server.hub.api.HubConnector;
import org.apache.directory.server.hub.api.exception.HubAbortException;
import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;


public class CollectionConnector implements HubConnector
{
    public enum CollectionType
    {
        LIST,
        ARRAY,
        SET
    }


    @Override
    public void init( ComponentHub hub )
    {
        try
        {
            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.LIST ),
                new CollectionOperations( CollectionType.LIST ) );

            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.ARRAY ),
                new CollectionOperations( CollectionType.ARRAY ) );

            hub.connectHandler(
                CollectionMetaDescriptorGenerator.generateMetadataDescriptor( CollectionType.SET ),
                new CollectionOperations( CollectionType.SET ) );
        }
        catch ( HubAbortException e )
        {
            //TODO Log error
        }

    }

}
