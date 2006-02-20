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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.server.kerberos.shared.messages.Encodable;


public class AuthorizationData implements Encodable
{
    private List entries = new ArrayList();


    /**
     * Class constructor
     */
    public AuthorizationData()
    {
        // used by ASN.1 decoder
    }


    public void add( AuthorizationData data )
    {
        entries.addAll( data.entries );
    }


    public void add( AuthorizationDataEntry entry )
    {
        entries.add( entry );
    }


    public Iterator iterator()
    {
        return entries.iterator();
    }
}
