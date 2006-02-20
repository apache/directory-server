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
package org.apache.directory.server.kerberos.shared.messages.value;


import org.apache.directory.server.kerberos.shared.messages.Encodable;


/**
 * Pre-authentication encrypted timestamp
 */
public class EncryptedTimeStamp implements Encodable
{
    private KerberosTime timeStamp;
    private int microSeconds; //optional


    public EncryptedTimeStamp(KerberosTime timeStamp, int microSeconds)
    {
        this.timeStamp = timeStamp;
        this.microSeconds = microSeconds;
    }


    public KerberosTime getTimeStamp()
    {
        return timeStamp;
    }


    public int getMicroSeconds()
    {
        return microSeconds;
    }
}
