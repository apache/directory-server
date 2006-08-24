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
import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.QuestionRecords;
import org.apache.directory.server.dns.messages.ResponseCode;
import org.apache.directory.server.dns.store.RecordStore;
import org.apache.directory.server.protocol.shared.chain.Context;
import org.apache.directory.server.protocol.shared.chain.impl.CommandBase;


public class GetResourceRecords extends CommandBase
{
    public boolean execute( Context context ) throws Exception
    {
        DnsContext dnsContext = ( DnsContext ) context;
        RecordStore store = dnsContext.getStore();

        QuestionRecords questions = dnsContext.getRequest().getQuestionRecords();

        Iterator it = questions.iterator();

        while ( it.hasNext() )
        {
            dnsContext.addResourceRecords( getEntry( store, ( QuestionRecord ) it.next() ) );
        }

        return CONTINUE_CHAIN;
    }


    public Set getEntry( RecordStore store, QuestionRecord question ) throws Exception
    {
        Set records = null;

        try
        {
            records = store.getRecords( question );
        }
        catch ( Exception e )
        {
            throw new DnsException( ResponseCode.SERVER_FAILURE );
        }

        if ( records == null || records.isEmpty() )
        {
            throw new DnsException( ResponseCode.NAME_ERROR );
        }

        return records;
    }
}
