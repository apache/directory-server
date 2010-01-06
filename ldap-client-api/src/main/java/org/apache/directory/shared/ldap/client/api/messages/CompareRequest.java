/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.shared.ldap.client.api.messages;


import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * Class for representing client's compare operation request.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CompareRequest extends AbstractRequest implements RequestWithResponse, AbandonableRequest
{
    /** the target entry's DN */
    private LdapDN entryDn;
    
    private String attrName;
    
    private Object value;

    public LdapDN getEntryDn()
    {
        return entryDn;
    }

    public void setEntryDn( LdapDN dn )
    {
        this.entryDn = dn;
    }

    public String getAttrName()
    {
        return attrName;
    }

    public void setAttrName( String attrName )
    {
        this.attrName = attrName;
    }

    public Object getValue()
    {
        return value;
    }

    public void setValue( Object attrValue )
    {
        this.value = attrValue;
    }
    
}
