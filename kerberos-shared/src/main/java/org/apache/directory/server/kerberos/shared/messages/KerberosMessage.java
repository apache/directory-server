/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.kerberos.shared.messages;


public class KerberosMessage
{
    // Kerberos protocol version number
    public static final int PVNO = 5;

    private int protocolVersionNumber;
    private MessageType messageType;


    public KerberosMessage(MessageType type)
    {
        this( PVNO, type );
    }


    public KerberosMessage(int versionNumber, MessageType type)
    {
        protocolVersionNumber = versionNumber;
        messageType = type;
    }


    public MessageType getMessageType()
    {
        return messageType;
    }


    public void setMessageType( MessageType type )
    {
        messageType = type;
    }


    public int getProtocolVersionNumber()
    {
        return protocolVersionNumber;
    }


    public void setProtocolVersionNumber( int versionNumber )
    {
        protocolVersionNumber = versionNumber;
    }
}
