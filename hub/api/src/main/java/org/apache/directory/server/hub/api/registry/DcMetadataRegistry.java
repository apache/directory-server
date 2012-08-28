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

package org.apache.directory.server.hub.api.registry;


import java.util.Hashtable;
import java.util.List;

import org.apache.directory.server.hub.api.meta.DcMetadataDescriptor;


public class DcMetadataRegistry
{
    private Hashtable<String, DcMetadataDescriptor> metadatas = new Hashtable<String, DcMetadataDescriptor>();


    public void addMetadataDescriptor( DcMetadataDescriptor meta )
    {
        metadatas.put( meta.getMetadataPID(), meta );
    }


    public void addMetadataDescriptor( List<DcMetadataDescriptor> metas )
    {
        for ( DcMetadataDescriptor meta : metas )
        {
            addMetadataDescriptor( meta );
        }
    }


    public void removeMetadataDescriptor( String metaPID )
    {
        metadatas.remove( metaPID );
    }


    public DcMetadataDescriptor getMetadataDescriptor( String metaPID )
    {
        return metadatas.get( metaPID );
    }
}
