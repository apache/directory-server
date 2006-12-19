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
package org.apache.directory.mitosis.service.protocol.codec;


import javax.naming.InvalidNameException;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.DefaultCSN;
import org.apache.directory.mitosis.operation.AddAttributeOperation;
import org.apache.directory.mitosis.service.protocol.codec.LogEntryMessageDecoder;
import org.apache.directory.mitosis.service.protocol.codec.LogEntryMessageEncoder;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.mitosis.service.protocol.message.LogEntryMessage;
import org.apache.directory.shared.ldap.name.LdapDN;


public class LogEntryMessageCodecTest extends AbstractMessageCodecTest
{

    public LogEntryMessageCodecTest() throws InvalidNameException
    {
        super(
            new LogEntryMessage( 1234, new AddAttributeOperation( new DefaultCSN( System.currentTimeMillis(),
                new ReplicaId( "testReplica0" ), 1234 ), new LdapDN( "ou=system" ),
                new BasicAttribute( "Hello", "Test" ) ) ), new LogEntryMessageEncoder(), new LogEntryMessageDecoder() );
    }


    protected boolean compare( BaseMessage expected0, BaseMessage actual0 )
    {
        LogEntryMessage expected = ( LogEntryMessage ) expected0;
        LogEntryMessage actual = ( LogEntryMessage ) actual0;

        // We don't compare operation here because it is {@link OperationCodec}'s
        // duty to serialize and deserialize Invocations.
        return expected.getType() == actual.getType() && expected.getSequence() == actual.getSequence()
            && expected.getOperation().getCSN().equals( actual.getOperation().getCSN() )
            && expected.getOperation().getClass() == actual.getOperation().getClass();
    }
}
