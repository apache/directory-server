/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.dns.store;


import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.core.configuration.ConfigurationException;
import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.store.operations.GetRecords;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A JNDI-backed search strategy implementation.  This search strategy builds a catalog
 * from directory configuration to determine where zones are to search for
 * resource records.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MultiBaseSearch implements SearchStrategy
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( MultiBaseSearch.class );

    private InitialContextFactory factory;
    private Hashtable env;

    private Catalog catalog;


    MultiBaseSearch(DnsConfiguration config, InitialContextFactory factory)
    {
        this.factory = factory;

        env = new Hashtable( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getCatalogBaseDn() );

        try
        {
            DirContext ctx = ( DirContext ) factory.getInitialContext( env );
            catalog = new DnsCatalog( ( Map ) execute( ctx, new GetCatalog() ) );
        }
        catch ( Exception e )
        {
            log.error( e.getMessage(), e );
            String message = "Failed to get catalog context " + ( String ) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, e );
        }
    }


    public Set getRecords( QuestionRecord question ) throws Exception
    {
        env.put( Context.PROVIDER_URL, catalog.getBaseDn( question.getDomainName() ) );

        try
        {
            DirContext ctx = ( DirContext ) factory.getInitialContext( env );
            return ( Set ) execute( ctx, new GetRecords( question ) );
        }
        catch ( NamingException ne )
        {
            log.error( ne.getMessage(), ne );
            String message = "Failed to get initial context " + ( String ) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }


    private Object execute( DirContext ctx, ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }
}
