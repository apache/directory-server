/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.eve.jndi.ibs;


import org.apache.eve.jndi.BaseInterceptor;
import org.apache.eve.RootNexus;
import org.apache.eve.schema.GlobalRegistries;


/**
 * A schema management and enforcement interceptor service.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SchemaService extends BaseInterceptor
{
    /** the root nexus to all database partitions */
    private final RootNexus nexus;
    private FilterService filterService;
    /** the global schema object registries */
    private final GlobalRegistries globalRegistries;


    /**
     * Creates a schema service interceptor.
     *
     * @param nexus the root nexus to access all database partitions
     * @param globalRegistries the global schema object registries
     * @param filterService
     */
    public SchemaService( RootNexus nexus, GlobalRegistries globalRegistries,
                          FilterService filterService )
    {
        this.nexus = nexus;
        if ( this.nexus == null )
        {
            throw new NullPointerException( "the nexus cannot be null" );
        }

        this.globalRegistries = globalRegistries;
        if ( this.globalRegistries == null )
        {
            throw new NullPointerException( "the global registries cannot be null" );
        }

        this.filterService = filterService;
        if ( this.filterService == null )
        {
            throw new NullPointerException( "the filter service cannot be null" );
        }
    }
}
