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
import java.util.Set;

import org.apache.directory.server.dns.DnsException;
import org.apache.directory.server.dns.messages.DnsMessage;
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.QuestionRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.shared.ldap.exception.LdapNameNotFoundException;
import org.apache.mina.common.IoSession;
import org.apache.mina.handler.chain.IoHandlerCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GetResourceRecords implements IoHandlerCommand
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( GetResourceRecords.class );

    private String contextKey = "context";

    public void execute( NextCommand next, IoSession session, Object message ) throws Exception
    {
        DnsContext dnsContext = (DnsContext) session.getAttribute( getContextKey() );
        RecordStore store = dnsContext.getStore();

        DnsMessage request = (DnsMessage) message;
        QuestionRecords questions = request.getQuestionRecords();

        Iterator it = questions.iterator();

        while ( it.hasNext() )
        {
            dnsContext.addResourceRecords( getEntry( store, ( QuestionRecord ) it.next() ) );
        }

        next.execute( session, message );
    }


    public Set getEntry( RecordStore store, QuestionRecord question ) throws Exception
    {
        Set records = null;

        try
        {
            records = store.getRecords( question );
        }
        catch ( LdapNameNotFoundException lnnfe )
        {
            log.debug( "Name for DNS record search does not exist.", lnnfe );

            throw new DnsException( ResponseCode.NAME_ERROR );
        }
        catch ( Exception e )
        {
            log.debug( "Unexpected error retrieving DNS records.", e );

            throw new DnsException( ResponseCode.SERVER_FAILURE );
        }

        if ( records == null || records.isEmpty() )
        {
            log.debug( "The domain name referenced in the query does not exist." );

            throw new DnsException( ResponseCode.NAME_ERROR );
        }

        return records;
    }

    public String getContextKey()
    {
        return ( this.contextKey );
    }
}
