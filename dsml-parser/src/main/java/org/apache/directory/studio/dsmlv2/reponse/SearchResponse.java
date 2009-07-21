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

import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;


/**
 * This class represents the DSML Search Response
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SearchResponse extends LdapResponseCodec
{
    /** The List of contained Search Result Entries */
    private List<SearchResultEntryCodec> searchResultEntryList;

    /** The List of contained Search Result References */
    private List<SearchResultReferenceCodec> searchResultReferenceList;

    /** The Search Result Done object */
    private SearchResultDoneCodec searchResultDone;


    /**
     * Creates a new instance of SearchResponse.
     */
    public SearchResponse()
    {
        searchResultEntryList = new ArrayList<SearchResultEntryCodec>();
        searchResultReferenceList = new ArrayList<SearchResultReferenceCodec>();
    }


    /**
     * Adds a Search Result Entry
     *
     * @param searchResultEntry
     *      the Search Result Entry to add
     * @return
     *      true (as per the general contract of the Collection.add method)
     */
    public boolean addSearchResultEntry( SearchResultEntryCodec searchResultEntry )
    {
        return searchResultEntryList.add( searchResultEntry );
    }


    /**
     * Gets the Current Search Result Entry
     * 
     * @return
     *      the current Searche Result Entry
     */
    public SearchResultEntryCodec getCurrentSearchResultEntry()
    {
        if ( searchResultEntryList.size() > 0 )
        {
            return searchResultEntryList.get( searchResultEntryList.size() - 1 );
        }
        else
        {
            return null;
        }
    }


    /**
     * Gets the Search Result Entry List
     *
     * @return
     *      the Search Result Entry List
     */
    public List<SearchResultEntryCodec> getSearchResultEntryList()
    {
        return searchResultEntryList;
    }


    /**
     * Adds a Search Result Reference
     *
     * @param searchResultReference
     *      the Search Result Reference to add
     * @return
     *      true (as per the general contract of the Collection.add method)
     */
    public boolean addSearchResultReference( SearchResultReferenceCodec searchResultReference )
    {
        return searchResultReferenceList.add( searchResultReference );
    }


    /**
     * Gets the current Search Result Reference
     *
     * @return
     *      the current Search Result Reference
     */
    public SearchResultReferenceCodec getCurrentSearchResultReference()
    {
        if ( searchResultReferenceList.size() > 0 )
        {
            return searchResultReferenceList.get( searchResultReferenceList.size() - 1 );
        }
        else
        {
            return null;
        }
    }


    /**
     * Gets the Search Result Reference List
     *
     * @return
     *      the Search Result Reference List
     */
    public List<SearchResultReferenceCodec> getSearchResultReferenceList()
    {
        return searchResultReferenceList;
    }


    /**
     * Gets the Search Result Entry
     * 
     * @return
     *      the Search Result Entry
     */
    public SearchResultDoneCodec getSearchResultDone()
    {
        return searchResultDone;
    }


    /**
     * Sets the Search Result Entry
     *
     * @param searchResultDone
     *      the Search Result Entry to set
     */
    public void setSearchResultDone( SearchResultDoneCodec searchResultDone )
    {
        this.searchResultDone = searchResultDone;
    }
}
