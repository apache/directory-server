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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.studio.dsmlv2.DsmlDecorator;
import org.dom4j.Element;


/**
 * This class represents the Search Response Dsml Container. 
 * It is used to store Search Responses (Search Result Entry, 
 * Search Result Reference and SearchResultDone).
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResponseDsml extends LdapResponseDecorator implements DsmlDecorator
{
    /** The responses */
    private List<DsmlDecorator> responses = new ArrayList<DsmlDecorator>();


    /**
     * Creates a new instance of SearchResponseDsml.
     */
    public SearchResponseDsml()
    {
        super( new LdapMessageCodec() );
    }


    /**
     * Adds a response.
     *
     * @param response
     *      the response to add
     * @return
     *      true (as per the general contract of the Collection.add method).
     */
    public boolean addResponse( DsmlDecorator response )
    {
        return responses.add( response );
    }


    /**
     * Removes a response.
     *
     * @param response
     *      the response to remove
     * @return
     *      true if this list contained the specified element.
     */
    public boolean removeResponse( DsmlDecorator response )
    {
        return responses.remove( response );
    }


    /* (non-Javadoc)
     * @see org.apache.directory.studio.dsmlv2.DsmlDecorator#toDsml(org.dom4j.Element)
     */
    public Element toDsml( Element root )
    {
        Element element = root.addElement( "searchResponse" );

        // RequestID
        int requestID = instance.getMessageId();
        if ( requestID != 0 )
        {
            element.addAttribute( "requestID", "" + requestID );
        }

        for ( DsmlDecorator response : responses )
        {
            response.toDsml( element );
        }

        return element;
    }
}
