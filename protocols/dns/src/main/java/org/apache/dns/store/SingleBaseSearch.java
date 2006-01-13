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

package org.apache.dns.store;

import java.util.Hashtable;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.spi.InitialContextFactory;

import org.apache.dns.DnsConfiguration;
import org.apache.dns.messages.QuestionRecord;
import org.apache.dns.store.operations.GetRecords;
import org.apache.ldap.server.configuration.ConfigurationException;
import org.apache.protocol.common.store.ContextOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A JNDI-backed search strategy implementation.  This search strategy searches a
 * single base DN for resource records.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SingleBaseSearch implements SearchStrategy
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( SingleBaseSearch.class );

    private DirContext ctx;

    SingleBaseSearch( DnsConfiguration config, InitialContextFactory factory )
    {
        Hashtable env = new Hashtable( config.toJndiEnvironment() );
        env.put( Context.INITIAL_CONTEXT_FACTORY, config.getInitialContextFactory() );
        env.put( Context.PROVIDER_URL, config.getEntryBaseDn() );

        try
        {
            ctx = (DirContext) factory.getInitialContext( env );
        }
        catch ( NamingException ne )
        {
            log.error( ne.getMessage(), ne );
            String message = "Failed to get initial context " + (String) env.get( Context.PROVIDER_URL );
            throw new ConfigurationException( message, ne );
        }
    }

    public Set getRecords( QuestionRecord question ) throws Exception
    {
        return (Set) execute( new GetRecords( question ) );
    }

    private Object execute( ContextOperation operation ) throws Exception
    {
        return operation.execute( ctx, null );
    }
}
