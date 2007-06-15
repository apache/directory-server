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
package org.apache.directory.server.dns.store;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.dns.messages.QuestionRecord;
import org.apache.directory.server.dns.messages.RecordClass;
import org.apache.directory.server.dns.messages.RecordType;
import org.apache.directory.server.dns.messages.ResourceRecord;
import org.apache.directory.server.dns.messages.ResourceRecordModifier;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RecordStoreStub implements RecordStore
{
    public Set<ResourceRecord> getRecords( QuestionRecord question ) throws Exception
    {
        Set<ResourceRecord> set = new HashSet<ResourceRecord>();

        ResourceRecordModifier rm = new ResourceRecordModifier();
        rm.setDnsClass( RecordClass.IN );
        rm.setDnsName( "ldap.example.com" );
        rm.setDnsTtl( 100 );
        rm.setDnsType( RecordType.A );
        rm.put( DnsAttribute.IP_ADDRESS, "10.0.0.2" );

        set.add( rm.getEntry() );

        ResourceRecordModifier rm2 = new ResourceRecordModifier();
        rm2.setDnsClass( RecordClass.IN );
        rm2.setDnsName( "www.example.com" );
        rm2.setDnsTtl( 100 );
        rm2.setDnsType( RecordType.A );
        rm2.put( DnsAttribute.IP_ADDRESS, "10.0.0.3" );

        set.add( rm2.getEntry() );

        return set;
    }
}
