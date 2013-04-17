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

/**
 * A class for sending and receiving kerberos request and response data
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KerberosChannel
{
    /** the TCP socket */
    private Socket tcpSocket;
    
    /** the UDP socket */
    private DatagramSocket udpSocket;
    
    /** data input stream to read from */
    private DataInputStream in;
    
    /** data output stream to write to */
    private OutputStream out;
    
    /** flag to detect if this is a UDP channel */
    private boolean useUdp;
    
    private int timeOut = 60000;

    /** the UDP socket address of the server */
    private SocketAddress udpServerAddr = null;
    
    protected void openConnection( String hostName, int  portNo, int timeOut, boolean isUdp ) throws IOException
    {
        this.useUdp = isUdp;
        this.timeOut = timeOut;
        
        if ( isUdp )
        {
            udpServerAddr = new InetSocketAddress( hostName, portNo );
            udpSocket = new DatagramSocket();
        }
        else
        {
            tcpSocket = new Socket( hostName, portNo );
            tcpSocket.setSoTimeout( ( int ) timeOut );
            in = new DataInputStream( tcpSocket.getInputStream() );
            out = tcpSocket.getOutputStream();
        }
    }


    protected ByteBuffer sendAndReceive( ByteBuffer encodedBuf ) throws IOException
    {
        byte[] reqData  = encodedBuf.array();
        
        ByteBuffer repData;
        
        if ( useUdp )
        {
            DatagramPacket reqPacket = new DatagramPacket( reqData, reqData.length, udpServerAddr );
            udpSocket.send( reqPacket );
            
            byte[] buffer = new byte[2048];
            DatagramPacket repPacket = new DatagramPacket( buffer, buffer.length );
            udpSocket.receive( repPacket );
            
            byte[] receivedData = repPacket.getData();
            repData = ByteBuffer.allocate( receivedData.length );
            repData.put( receivedData );
        }
        else
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
        
        repData.flip();
        
        return repData;
    }
    
    
    protected boolean isUseTcp()
    {
        return !useUdp;
    }


    protected void close() throws IOException
    {
        if( useUdp )
        {
            udpSocket.close();
        }
        else
        {
            tcpSocket.close();
        }
    }
}

