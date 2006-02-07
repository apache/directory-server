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
package org.apache.changepw.messages;

import org.apache.kerberos.messages.ApplicationRequest;
import org.apache.kerberos.messages.application.PrivateMessage;

public class ChangePasswordRequest extends AbstractPasswordMessage
{
    private ApplicationRequest authHeader;
    private PrivateMessage privateMessage;

    public ChangePasswordRequest( short messageLength, short versionNumber, short authHeaderLength,
            ApplicationRequest authHeader, PrivateMessage privateMessage )
    {
        super( messageLength, versionNumber, authHeaderLength );

        this.authHeader = authHeader;
        this.privateMessage = privateMessage;
    }

    public ApplicationRequest getAuthHeader()
    {
        return authHeader;
    }

    public PrivateMessage getPrivateMessage()
    {
        return privateMessage;
    }
}
