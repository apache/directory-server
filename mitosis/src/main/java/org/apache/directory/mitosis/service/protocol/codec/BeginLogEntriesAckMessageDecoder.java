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
import java.nio.charset.CharsetDecoder;

import org.apache.directory.mitosis.common.CSNVector;
import org.apache.directory.mitosis.common.ReplicaId;
import org.apache.directory.mitosis.common.SimpleCSN;
import org.apache.directory.mitosis.service.protocol.Constants;
import org.apache.directory.mitosis.service.protocol.message.BaseMessage;
import org.apache.directory.mitosis.service.protocol.message.BeginLogEntriesAckMessage;
import org.apache.mina.common.ByteBuffer;
import org.apache.mina.common.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderException;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class BeginLogEntriesAckMessageDecoder extends ResponseMessageDecoder
{
    private final CharsetDecoder utf8decoder;

    public BeginLogEntriesAckMessageDecoder()
    {
        super( Constants.GET_UPDATE_VECTOR_ACK, 0, 3072 );
        utf8decoder = Charset.forName( "UTF-8" ).newDecoder();
    }
    protected BaseMessage decodeBody( int sequence, int bodyLength,
                                      int responseCode, ByteBuffer in ) throws Exception
    {
        if( responseCode != Constants.OK )
        {
            return new BeginLogEntriesAckMessage( sequence, responseCode, null, null );
        }

        CSNVector purgeVector = new CSNVector();
        CSNVector updateVector = new CSNVector();
        BeginLogEntriesAckMessage m = new BeginLogEntriesAckMessage( sequence, responseCode, purgeVector, updateVector );
        readCSNVector( in, purgeVector );
        readCSNVector( in, updateVector );
        
        return m;
    }

    private void readCSNVector( ByteBuffer in, CSNVector updateVector ) throws Exception
    {
        int nReplicas = in.getInt();
        if( nReplicas < 0 )
        {
            throw new ProtocolDecoderException( "Wrong nReplicas: " + nReplicas );
        }
        
        for( ; nReplicas > 0; nReplicas-- )
        {
            ReplicaId replicaId;
            try
            {
                replicaId = new ReplicaId( in.getString( utf8decoder ) );
            }
            catch( CharacterCodingException e )
            {
                throw new ProtocolDecoderException( "Invalid replicaId", e );
            }
            
            updateVector.setCSN( new SimpleCSN( in.getLong(), replicaId, in.getInt() ) );
        }
    }
    
    public void finishDecode( IoSession session, ProtocolDecoderOutput out ) throws Exception
    {
    }
}
