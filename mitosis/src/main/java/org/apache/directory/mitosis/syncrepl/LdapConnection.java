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
package org.apache.directory.mitosis.syncrepl;

import java.io.IOException;
import java.util.List;

import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapResponse;
import org.apache.directory.shared.ldap.codec.search.SearchRequest;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntry;


/**
 * 
 *  Describe the methods to be implemented by the LdapConnection class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface LdapConnection
{
    /**
     * Connect to the remote LDAP server
     *
     * @return <code>true</code> if the connection is established, false otherwise
     * @throws IOException if some I/O error occurs
     */
    boolean connect() throws IOException; 
    
    /**
     * Disconnect from the remote LDAP server
     *
     * @return <code>true</code> if the connection is closed, false otherwise
     * @throws IOException if some I/O error occurs
     */
    boolean close() throws IOException; 
    
    /**
     * Simple Bind on a server
     *
     * @param name The name we use to authenticate the user. It must be a 
     * valid DN
     * @param credentials The password. It can't be null 
     * @return The BindResponse LdapResponse 
     */
    LdapResponse bind( String name, String credentials ) throws Exception;
    
    /**
     * Do a search, on the base object, using the given filter. The
     * SearchRequest parameters default to :
     * Scope : ONE
     * DerefAlias : ALWAYS
     * SizeLimit : none
     * TimeLimit : none
     * TypesOnly : false
     * Attributes : all the user's attributes 
     * 
     * @param baseObject The base for the search. It must be a valid
     * DN, and can't be emtpy
     * @param filter The filter to use for this search. It can't be empty
     * @return An Object array of size 2 containing the result entries
     * at the 0th index and the syncdone value at 1st index
     */
    Object[] search( String baseObject, String filter ) throws Exception;
    
    
    /**
     * Send the already built SearchRequest to the server.
     *  
     * @param searchRequest the SearchRequest object to send to the server
     * @return An Object array of size 2 containing the result entries
     * at the 0th index and the syncdone value at 1st index
     */
    Object[] search( SearchRequest searchRequest ) throws Exception;
    
    /*
    void search( String baseObject, SearchScope scope, int derefAlias,
        int sizeLimit, int timeLimit, boolean typesOnly, String filter, 
        String[] attributes );
    
    void search( String baseObject, SearchScope scope, int derefAlias,
        int sizeLimit, int timeLimit, boolean typesOnly, String filter, 
        String[] attributes );
    
    void search( String baseObject, SearchScope scope, int derefAlias,
        int sizeLimit, int timeLimit, boolean typesOnly, String filter, 
        String[] attributes );
    
    void search( String baseObject, SearchScope scope, int derefAlias,
        int sizeLimit, int timeLimit, boolean typesOnly, String filter, 
        String[] attributes );
    */
    
    /**
     * UnBind from a server
     */
    void unBind() throws Exception;
    
    
    /**
     * Return the response stored into the current session.
     *
     * @return The last request response
     */
    LdapMessage getResponse();
}
