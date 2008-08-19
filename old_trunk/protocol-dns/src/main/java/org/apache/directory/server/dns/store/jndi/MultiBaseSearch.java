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

package org.apache.directory.server.dns.store.jndi;


import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.jndi.operations.GetRecords;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
import org.apache.directory.server.protocol.shared.catalog.Catalog;
import org.apache.directory.server.protocol.shared.catalog.GetCatalog;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import java.util.Map;
import java.util.Set;


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
    /** the LOG for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MultiBaseSearch.class );

    private final Catalog catalog;
    private final DirectoryService directoryService;


    MultiBaseSearch( String catalogBaseDn, DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        try
        {
            DirContext ctx = directoryService.getJndiContext(catalogBaseDn);
            //noinspection unchecked
            catalog = new DnsCatalog( ( Map<String, Object> ) new GetCatalog().execute( ctx, null ) );
        }
        catch ( Exception e )
        {
            LOG.error( e.getMessage(), e );
            String message = "Failed to get catalog context " + catalogBaseDn;
            throw new ServiceConfigurationException( message, e );
        }
    }


    public Set<ResourceRecord> getRecords( QuestionRecord question ) throws DnsException
    {
        try
        {
            GetRecords getRecords = new GetRecords( question );
            String baseDn = catalog.getBaseDn( question.getDomainName() );
            DirContext dirContext = directoryService.getJndiContext( baseDn );
            return getRecords.execute( dirContext, null );
        }
        catch ( LdapNameNotFoundException lnnfe )
        {
            LOG.debug( "Name for DNS record search does not exist.", lnnfe );

            throw new DnsException( ResponseCode.NAME_ERROR );
        }
        catch ( NamingException ne )
        {
            LOG.error( ne.getMessage(), ne );
            String message = "Failed to get initial context " + question.getDomainName();
            throw new ServiceConfigurationException( message, ne );
        }
        catch ( Exception e )
        {
            LOG.debug( "Unexpected error retrieving DNS records.", e );
            throw new DnsException( ResponseCode.SERVER_FAILURE );
        }

    }


}
