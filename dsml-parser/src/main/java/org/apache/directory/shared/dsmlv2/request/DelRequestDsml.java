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
package org.apache.directory.shared.dsmlv2.request;


import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.dom4j.Element;


/**
 * DSML Decorator for DelRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class DelRequestDsml extends AbstractRequestDsml
{
    /**
     * Creates a new instance of DelRequestDsml.
     */
    public DelRequestDsml()
    {
        super( new DelRequestCodec() );
    }


    /**
     * Creates a new instance of DelRequestDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public DelRequestDsml( DelRequestCodec ldapMessage )
    {
        super( ldapMessage );
    }


    /**
     * {@inheritDoc}
     */
    public int getMessageType()
    {
        return instance.getMessageType();
    }


    /**
     * {@inheritDoc}
     */
    public Element toDsml( Element root )
    {
        Element element = super.toDsml( root );

        DelRequestCodec request = ( DelRequestCodec ) instance;

        // DN
        if ( request.getEntry() != null )
        {
            element.addAttribute( "dn", request.getEntry().getName() );
        }

        return element;
    }


    /**
     * Get the entry to be deleted
     * 
     * @return Returns the entry.
     */
    public LdapDN getEntry()
    {
        return ( ( DelRequestCodec ) instance ).getEntry();
    }


    /**
     * Set the entry to be deleted
     * 
     * @param entry The entry to set.
     */
    public void setEntry( LdapDN entry )
    {
        ( ( DelRequestCodec ) instance ).setEntry( entry );
    }
}
