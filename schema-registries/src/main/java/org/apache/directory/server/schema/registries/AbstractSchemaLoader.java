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
package org.apache.directory.server.schema.registries;


import java.util.Map;
import java.util.Properties;
import java.util.Stack;

import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An abstract class with a utility method and setListener() implemented.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public abstract class AbstractSchemaLoader implements SchemaLoader
{
    /** static class logger */
    private final static Logger log = LoggerFactory.getLogger( AbstractSchemaLoader.class );
    
    protected SchemaLoaderListener listener;
    
    
    public void setListener( SchemaLoaderListener listener )
    {
        this.listener = listener;
    }
    
    
    protected final void notifyListenerOrRegistries( Schema schema, Registries registries )
    {
        if ( listener != null )
        {
            listener.schemaLoaded( schema );
        }
        
        if ( registries instanceof SchemaLoaderListener )
        {
            if ( registries != listener )
            {
                SchemaLoaderListener listener = ( SchemaLoaderListener ) registries;
                listener.schemaLoaded( schema );
            }
        }
    }
    
    
    /**
     * Recursive method which loads schema's with their dependent schemas first
     * and tracks what schemas it has seen so the recursion does not go out of
     * control with depenency cycle detection.
     *
     * @param beenthere stack of schema names we have visited and have yet to load
     * @param notLoaded hash of schemas keyed by name which have yet to be loaded
     * @param schema the current schema we are attempting to load
     * @param registries the set of registries to use while loading
     * @param properties to use while trying resolve other schemas
     * @throws NamingException if there is a cycle detected and/or another
     * failure results while loading, producing and or registering schema objects
     */
    protected final void loadDepsFirst( Stack<String> beenthere, Map<String,Schema> notLoaded, Schema schema,
        Registries registries, Properties props ) throws NamingException
    {
        if ( registries.getLoadedSchemas().containsKey( schema.getSchemaName() ) )
        {
            log.warn( "{} schema has already been loaded" + schema.getSchemaName() );
            return;
        }
        
        beenthere.push( schema.getSchemaName() );
        String[] deps = schema.getDependencies();

        // if no deps then load this guy and return
        if ( deps == null || deps.length == 0 )
        {
            load( schema, registries );
            notLoaded.remove( schema.getSchemaName() );
            beenthere.pop();
            return;
        }

        /*
         * We got deps and need to load them before this schema.  We go through
         * all deps loading them with their deps first if they have not been
         * loaded.
         */
        for ( int ii = 0; ii < deps.length; ii++ )
        {
            if ( !notLoaded.containsKey( deps[ii] ) )
            {
                continue;
            }

            Schema dep = notLoaded.get( deps[ii] );
            
            // dep is not in the set of schema objects we need to try to resolve it
            if ( dep == null )
            {
                // try to load dependency with the provided properties default 
                dep = getSchema( deps[ii], props );
            }

            if ( beenthere.contains( dep.getSchemaName() ) )
            {
                // push again so we show the cycle in output
                beenthere.push( dep.getSchemaName() );
                throw new NamingException( "schema dependency cycle detected: " + beenthere );
            }

            loadDepsFirst( beenthere, notLoaded, dep, registries, props );
        }

        // We have loaded all our deps so we can load this schema
        load( schema, registries );
        notLoaded.remove( schema.getSchemaName() );
        beenthere.pop();
    }
}
