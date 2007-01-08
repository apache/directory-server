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
package org.apache.directory.mitosis.operation;


import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.mitosis.common.CSN;


/**
 * An {@link Operation} that adds an attribute to an entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AddAttributeOperation extends AttributeOperation
{
    private static final long serialVersionUID = 7373124294791982297L;


    /**
     * Creates a new operation that adds the specified attribute.
     * 
     * @param attribute an attribute to add
     */
    public AddAttributeOperation( CSN csn, LdapDN name, Attribute attribute )
    {
        super( csn, name, attribute );
    }


    public String toString()
    {
        return super.toString() + ".add( " + getAttribute() + " )";
    }


    protected void execute1( PartitionNexus nexus ) throws NamingException
    {
        Attributes attrs = new AttributesImpl( true );
        attrs.put( getAttribute() );
        nexus.modify( getName(), DirContext.ADD_ATTRIBUTE, attrs );
    }
}
