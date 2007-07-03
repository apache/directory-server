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
package org.apache.directory.server.dns.service;


import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetResourceRecords implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( GetResourceRecords.class );

    private String contextKey = "context";


    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        DnsContext dnsContext = ( DnsContext ) session.getAttribute( getContextKey() );
        RecordStore store = dnsContext.getStore();

        DnsMessage request = ( DnsMessage ) message;
        List<QuestionRecord> questions = request.getQuestionRecords();

        Iterator<QuestionRecord> it = questions.iterator();

        while ( it.hasNext() )
        {
            dnsContext.addResourceRecords( getEntry( store, it.next() ) );
        }

        next.execute( session, message );
    }


    /**
     * Returns a set of {@link ResourceRecord}s from a {@link RecordStore}, given a DNS {@link QuestionRecord}.
     *
     * @param store
     * @param question
     * @return The set of {@link ResourceRecord}s.
     * @throws DNSException
     */
    public Set<ResourceRecord> getEntry( RecordStore store, QuestionRecord question ) throws DnsException
    {
        Set<ResourceRecord> records = null;

        records = store.getRecords( question );

        if ( records == null || records.isEmpty() )
        {
            log.debug( "The domain name referenced in the query does not exist." );

            throw new DnsException( ResponseCode.NAME_ERROR );
        }

        return records;
    }


    protected String getContextKey()
    {
        return ( this.contextKey );
    }
}
