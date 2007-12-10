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


import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.jndi.operations.GetRecords;
import org.apache.directory.server.protocol.shared.ServiceConfigurationException;
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
    /**
     * the LOG for this class
     */
    private static final Logger LOG = LoggerFactory.getLogger( SingleBaseSearch.class );

    private final DirContext ctx;


    SingleBaseSearch( String searchBaseDn, DirectoryService directoryService )
    {
        try
        {
            ctx = directoryService.getJndiContext( searchBaseDn );
        } catch ( NamingException e )
        {
            throw new ServiceConfigurationException( "Can't get context at" + searchBaseDn, e );
        }

    }


    public Set<ResourceRecord> getRecords( QuestionRecord question ) throws DnsException
    {
        try
        {

            return new GetRecords( question ).execute( ctx, null );
        }
        catch ( Exception e )
        {
            LOG.debug( "Unexpected error retrieving DNS records.", e );
            throw new DnsException( ResponseCode.SERVER_FAILURE );
        }
    }


}
