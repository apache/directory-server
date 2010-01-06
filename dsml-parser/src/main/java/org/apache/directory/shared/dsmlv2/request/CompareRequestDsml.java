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


import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.dom4j.Element;


/**
 * DSML Decorator for CompareRequest
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CompareRequestDsml extends AbstractRequestDsml
{
    /**
     * Creates a new instance of CompareRequestDsml.
     */
    public CompareRequestDsml()
    {
        super( new CompareRequestCodec() );
    }


    /**
     * Creates a new instance of CompareRequestDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public CompareRequestDsml( CompareRequestCodec ldapMessage )
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

        CompareRequestCodec request = ( CompareRequestCodec ) instance;

        // DN
        if ( request.getEntry() != null )
        {
            element.addAttribute( "dn", request.getEntry().getName() );
        }

        // Assertion
        Element assertionElement = element.addElement( "assertion" );
        if ( request.getAttributeDesc() != null )
        {
            assertionElement.addAttribute( "name", request.getAttributeDesc() );
        }
        if ( request.getAssertionValue() != null )
        {
            assertionElement.addElement( "value" ).setText( ( String ) request.getAssertionValue() );
        }

        return element;
    }


    /**
     * Get the entry to be compared
     * 
     * @return Returns the entry.
     */
    public LdapDN getEntry()
    {
        return ( ( CompareRequestCodec ) instance ).getEntry();
    }


    /**
     * Set the entry to be compared
     * 
     * @param entry The entry to set.
     */
    public void setEntry( LdapDN entry )
    {
        ( ( CompareRequestCodec ) instance ).setEntry( entry );
    }


    /**
     * Get the assertion value
     * 
     * @return Returns the assertionValue.
     */
    public Object getAssertionValue()
    {
        return ( ( CompareRequestCodec ) instance ).getAssertionValue();
    }


    /**
     * Set the assertion value
     * 
     * @param assertionValue The assertionValue to set.
     */
    public void setAssertionValue( Object assertionValue )
    {
        ( ( CompareRequestCodec ) instance ).setAssertionValue( assertionValue );
    }


    /**
     * Get the attribute description
     * 
     * @return Returns the attributeDesc.
     */
    public String getAttributeDesc()
    {
        return ( ( CompareRequestCodec ) instance ).getAttributeDesc();
    }


    /**
     * Set the attribute description
     * 
     * @param attributeDesc The attributeDesc to set.
     */
    public void setAttributeDesc( String attributeDesc )
    {
        ( ( CompareRequestCodec ) instance ).setAttributeDesc( attributeDesc );
    }
}
