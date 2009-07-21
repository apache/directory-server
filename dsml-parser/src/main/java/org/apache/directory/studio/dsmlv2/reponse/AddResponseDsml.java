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

package org.apache.directory.studio.dsmlv2.reponse;


import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.studio.dsmlv2.DsmlDecorator;
import org.dom4j.Element;


/**
 * DSML Decorator for AddResponse
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class AddResponseDsml extends LdapResponseDecorator implements DsmlDecorator
{
    /**
     * Creates a new instance of AddResponseDsml.
     */
    public AddResponseDsml()
    {
        super( new AddResponseCodec() );
    }


    /**
     * Creates a new instance of AddResponseDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public AddResponseDsml( AddResponseCodec ldapMessage )
    {
        super( ldapMessage );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.studio.dsmlv2.reponse.LdapMessageDecorator#getMessageType()
     */
    public int getMessageType()
    {
        return instance.getMessageType();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.studio.dsmlv2.reponse.DsmlDecorator#toDsml(org.dom4j.Element)
     */
    public Element toDsml( Element root )
    {
        Element element = root.addElement( "addResponse" );

        LdapResultDsml ldapResultDsml = new LdapResultDsml( ( ( AddResponseCodec ) instance ).getLdapResult(), instance );
        ldapResultDsml.toDsml( element );
        return element;
    }
}
