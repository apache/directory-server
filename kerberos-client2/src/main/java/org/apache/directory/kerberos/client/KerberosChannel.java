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
package org.apache.directory.kerberos.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

public class KerberosChannel
{
    private Socket tcpSocket;
    private DatagramSocket udpSocket;
    
    private DataInputStream in;
    private OutputStream out;
    
    private boolean useTcp;
    
    private int timeOut = 60000;

    public void openConnection( String hostName, int  portNo, int timeOut, boolean isTcp ) throws IOException
    {
        this.useTcp = isTcp;
        this.timeOut = timeOut;
        
        if ( isTcp )
        {
            tcpSocket = new Socket( hostName, portNo );
            tcpSocket.setSoTimeout( ( int ) timeOut );
            in = new DataInputStream( tcpSocket.getInputStream() );
            out = tcpSocket.getOutputStream();
        }
        else
        {
            SocketAddress bindaddr = new InetSocketAddress( hostName, portNo );
            udpSocket = new DatagramSocket( bindaddr );
        }
    }


    public ByteBuffer sendAndReceive( ByteBuffer encodedBuf ) throws IOException
    {
        byte[] reqData  = encodedBuf.array();
        
        ByteBuffer repData;
        
        if ( useTcp )
        {
            out.write( reqData );
            out.flush();

            int len = in.readInt();
            
            repData = ByteBuffer.allocate( len + 4 );
            repData.putInt( len );
            
            byte[] tmp = new byte[ 1024 * 8 ];
            while ( in.available() > 0 )
            {
                int read = in.read( tmp );
                repData.put( tmp, 0, read );
            }
        }
        else
        {
            DatagramPacket reqPacket = new DatagramPacket( reqData, reqData.length );
            udpSocket.send( reqPacket );
            
            DatagramPacket repPacket = new DatagramPacket( new byte[1], 1);
            udpSocket.receive( repPacket );
            
            byte[] receivedData = repPacket.getData();
            repData = ByteBuffer.allocate( receivedData.length );
            repData.put( receivedData );
        }
        
        repData.flip();
        
        return repData;
    }
    
    
    public boolean isUseTcp()
    {
        return useTcp;
    }


    public void close() throws IOException
    {
        if( useTcp )
        {
            tcpSocket.close();
        }
        else
        {
            udpSocket.close();
        }
    }
}

