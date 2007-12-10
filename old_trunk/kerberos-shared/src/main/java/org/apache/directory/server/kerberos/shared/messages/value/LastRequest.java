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
public class LastRequest
{
    private LastRequestEntry[] entries = new LastRequestEntry[1];


    /**
     * Creates a new instance of LastRequest.
     */
    public LastRequest()
    {
        entries[0] = new LastRequestEntry( LastRequestType.NONE, new KerberosTime() );
    }


    /**
     * Creates a new instance of LastRequest.
     *
     * @param entries
     */
    public LastRequest( LastRequestEntry[] entries )
    {
        this.entries = entries;
    }


    /**
     * Returns an array of {@link LastRequestEntry}s.
     *
     * @return The array of {@link LastRequestEntry}s.
     */
    public LastRequestEntry[] getEntries()
    {
        return entries;
    }
}
