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
package org.apache.kerberos.messages.value;

public class PreAuthenticationData
{
    private PreAuthenticationDataType dataType;
    private byte[] dataValue;

    public PreAuthenticationData( PreAuthenticationDataType dataType, byte[] dataValue )
    {
        this.dataType = dataType;
        this.dataValue = dataValue;
    }

    public PreAuthenticationDataType getDataType()
    {
        return dataType;
    }

    public byte[] getDataValue()
    {
        return dataValue;
    }
}
