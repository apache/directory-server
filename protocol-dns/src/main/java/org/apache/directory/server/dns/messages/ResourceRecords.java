/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.dns.messages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ResourceRecords
{
    private List resourceRecords;

    public ResourceRecords()
    {
        this.resourceRecords = new ArrayList();
    }

    public ResourceRecords( int initialCapacity )
    {
        this.resourceRecords = new ArrayList( initialCapacity );
    }

    public void add( ResourceRecord record )
    {
        resourceRecords.add( record );
    }

    public void addAll( Collection records )
    {
        resourceRecords.addAll( records );
    }

    public int size()
    {
        return resourceRecords.size();
    }

    public Iterator iterator()
    {
        return resourceRecords.iterator();
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        Iterator it = iterator();

        while ( it.hasNext() )
        {
            ResourceRecord record = (ResourceRecord) it.next();
            sb.append( "\n\t" + "dnsName                    " + record.getDomainName() );
            sb.append( "\n\t" + "dnsType                    " + record.getRecordType() );
            sb.append( "\n\t" + "dnsClass                   " + record.getRecordClass() );
            sb.append( "\n\t" + "dnsTtl                     " + record.getTimeToLive() );
        }

        return sb.toString();
    }
}
