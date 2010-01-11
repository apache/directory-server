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
package org.apache.directory.server.kerberos.shared.messages.value;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LastRequestEntry
{
    private LastRequestType lastRequestType;
    private KerberosTime lastRequestValue;


    /**
     * Creates a new instance of LastRequestEntry.
     *
     * @param type
     * @param value
     */
    public LastRequestEntry( LastRequestType type, KerberosTime value )
    {
        lastRequestType = type;
        lastRequestValue = value;
    }


    /**
     * Returns the {@link LastRequestType}.
     *
     * @return The {@link LastRequestType}.
     */
    public LastRequestType getLastRequestType()
    {
        return lastRequestType;
    }


    /**
     * Returns the {@link KerberosTime} of the last request.
     *
     * @return The {@link KerberosTime} of the last request.
     */
    public KerberosTime getLastRequestValue()
    {
        return lastRequestValue;
    }
}
