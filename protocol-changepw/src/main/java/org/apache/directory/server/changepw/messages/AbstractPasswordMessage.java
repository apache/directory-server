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
package org.apache.directory.server.changepw.messages;

abstract public class AbstractPasswordMessage
{
    // ChangePassword protocol version number
    public static final int PVNO = 1;

    private short messageLength;
    private short versionNumber;
    private short authHeaderLength;

    protected AbstractPasswordMessage( short messageLength, short versionNumber,
            short authHeaderLength )
    {
        this.messageLength = messageLength;
        this.versionNumber = versionNumber;
        this.authHeaderLength = authHeaderLength;
    }

    public short getMessageLength()
    {
        return messageLength;
    }

    public short getVersionNumber()
    {
        return versionNumber;
    }

    public short getAuthHeaderLength()
    {
        return authHeaderLength;
    }
}
