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

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.mina.common.ByteBuffer;
import org.apache.directory.mitosis.common.CSN;
import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.mitosis.service.protocol.message.BeginLogEntriesAckMessage;

public class BeginLogEntriesAckMessageEncoder extends ResponseMessageEncoder
{
    private final CharsetEncoder utf8encoder;

    public BeginLogEntriesAckMessageEncoder()
    {
        utf8encoder = Charset.forName( "UTF-8" ).newEncoder();
    }
    
    protected void encodeBody(BaseMessage in, ByteBuffer out) throws Exception {
        // write out response code
        super.encodeBody( in, out );
        
        BeginLogEntriesAckMessage m = ( BeginLogEntriesAckMessage ) in;
        if( m.getResponseCode() != Constants.OK )
        {
            return;
        }
        
        writeCSNVector( out, m.getPurgeVector() );
        writeCSNVector( out, m.getUpdateVector() );
    }

    private void writeCSNVector( ByteBuffer out, CSNVector csns )
    {
        Set replicaIds = csns.getReplicaIds();
        
        int nReplicas = replicaIds.size();
        out.putInt( nReplicas );
        Iterator it = replicaIds.iterator();
        while( it.hasNext() )
        {
            ReplicaId replicaId = ( ReplicaId ) it.next();
            CSN csn = csns.getCSN( replicaId );
            try {
                out.putString( replicaId.getId(), utf8encoder );
                out.put( ( byte ) 0x00 );
                out.putLong( csn.getTimestamp() );
                out.putInt( csn.getOperationSequence() );
            }
            catch ( CharacterCodingException e )
            {
                throw new RuntimeException( e );
            }
        }
    }

    public Set getMessageTypes()
    {
        Set set = new HashSet();
        set.add( BeginLogEntriesAckMessage.class );
        return set;
    }

}
