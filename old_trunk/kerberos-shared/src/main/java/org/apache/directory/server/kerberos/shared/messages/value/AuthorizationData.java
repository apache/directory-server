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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.Encodable;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AuthorizationData implements Encodable
{
    private List<AuthorizationDataEntry> entries = new ArrayList<AuthorizationDataEntry>();


    /**
     * Creates a new instance of AuthorizationData.
     */
    public AuthorizationData()
    {
        // used by ASN.1 decoder
    }


    /**
     * Adds all {@link AuthorizationData} entries to this {@link AuthorizationData}.
     *
     * @param data
     */
    public void add( AuthorizationData data )
    {
        entries.addAll( data.entries );
    }


    /**
     * Adds an {@link AuthorizationDataEntry} to this {@link AuthorizationData}.
     *
     * @param entry
     */
    public void add( AuthorizationDataEntry entry )
    {
        entries.add( entry );
    }


    /**
     * Returns an {@link Iterator} over the entries in this {@link AuthorizationData}.
     *
     * @return An {@link Iterator} over the entries in this {@link AuthorizationData}.
     */
    public Iterator iterator()
    {
        return entries.iterator();
    }
}
