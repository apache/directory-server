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


import java.util.List;

import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.studio.dsmlv2.DsmlDecorator;
import org.dom4j.Element;


/**
 * DSML Decorator for SearchResultReference
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResultReferenceDsml extends LdapResponseDecorator implements DsmlDecorator
{
    /**
     * Creates a new instance of SearchResultReferenceDsml.
     */
    public SearchResultReferenceDsml()
    {
        super( new SearchResultReferenceCodec() );
    }


    /**
     * Creates a new instance of SearchResultReferenceDsml.
     *
     * @param ldapMessage
     *      the message to decorate
     */
    public SearchResultReferenceDsml( LdapMessageCodec ldapMessage )
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
        Element element = root.addElement( "searchResultReference" );
        SearchResultReferenceCodec searchResultReference = ( SearchResultReferenceCodec ) instance;

        // Adding References
        List<LdapURL> refsList = searchResultReference.getSearchResultReferences();
        for ( int i = 0; i < refsList.size(); i++ )
        {
            element.addElement( "ref" ).addText( refsList.get( i ).toString() );
        }

        return element;
    }


    /**
     * Add a new reference to the list.
     * 
     * @param searchResultReference The search result reference
     */
    public void addSearchResultReference( LdapURL searchResultReference )
    {
        ( ( SearchResultReferenceCodec ) instance ).addSearchResultReference( searchResultReference );
    }


    /**
     * Get the list of references
     * 
     * @return An ArrayList of SearchResultReferences
     */
    public List<LdapURL> getSearchResultReferences()
    {
        return ( ( SearchResultReferenceCodec ) instance ).getSearchResultReferences();
    }
}
