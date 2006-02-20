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


import java.util.Set;

import javax.naming.spi.InitialContextFactory;

import org.apache.directory.server.dns.DnsConfiguration;
import org.apache.directory.server.dns.messages.QuestionRecord;


/**
 * A JNDI-backed implementation of the RecordStore interface.  This RecordStore uses
 * the Strategy pattern to either serve records based on a single base DN or to lookup
 * catalog mappings from directory configuration.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class JndiRecordStoreImpl implements RecordStore
{
    /** a handle on the configuration */
    private DnsConfiguration config;
    /** a handle on the provider factory */
    private InitialContextFactory factory;
    /** a handle on the searchh strategy */
    private SearchStrategy strategy;


    public JndiRecordStoreImpl(DnsConfiguration config, InitialContextFactory factory)
    {
        this.config = config;
        this.factory = factory;

        strategy = getSearchStrategy();
    }


    public Set getRecords( QuestionRecord question ) throws Exception
    {
        return strategy.getRecords( question );
    }


    private SearchStrategy getSearchStrategy()
    {
        if ( config.getCatalogBaseDn() != null )
        {
            // build catalog from factory
            return new MultiBaseSearch( config, factory );
        }

        // use config for catalog baseDN
        return new SingleBaseSearch( config, factory );
    }
}
