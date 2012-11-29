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
package org.apache.directory.server.core.api;

import org.apache.directory.shared.ldap.model.cursor.Cursor;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.message.SearchRequest;

/**
 * A container storing a SearchRequest being processed, and the associated 
 * size and time limit.
 * <br/>
 * We use an instance of this class for each new searchRequest, and we discard
 * it when the SearchRequest is completed or abandonned.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class SearchRequestContainer
{
    /** The SearchRequest */
    private SearchRequest searchRequest;
    
    /** The maximum number of entries we can send */
    private long sizeLimit;
    
    /** The number of entries already sent */
    private int count;
    
    /** The time limit */
    private long timeLimit;
    
    /** The SearchRequest reception date */
    private long initialTime;
    
    /** The Cursor associated with the searchRequest */
    private Cursor<Entry> cursor;
    
    
    /**
     * Create an instance of the container with the SearchRequest and its limit.
     * 
     * @param searchRequest The SearchRequest instance
     * @param sizeLimit The size limit
     * @param timeLimit The time limit
     */
    public SearchRequestContainer( SearchRequest searchRequest, Cursor<Entry> cursor )
    {
        this.searchRequest = searchRequest;
        this.cursor = cursor;
        this.sizeLimit = searchRequest.getSizeLimit();
        this.timeLimit = searchRequest.getTimeLimit() * 1000L; // Time limit is in seconds. Translate that to milliseconds
        
        // Initialize the count and current time
        count = 0;
        initialTime = System.currentTimeMillis();
    }
    

    /**
     * @return the searchRequest
     */
    public SearchRequest getSearchRequest()
    {
        return searchRequest;
    }
    

    /**
     * @return the sizeLimit
     */
    public long getSizeLimit()
    {
        return sizeLimit;
    }
    

    /**
     * @return the count
     */
    public int getCount()
    {
        return count;
    }
    

    /**
     * Check the size limit
     * 
     * @return true if we have reached the size limit
     */
    public boolean isSizeLimitReached()
    {
        return ( sizeLimit > 0 ) && ( count >= sizeLimit );
    }
    

    /**
     * @param count the count to set
     */
    public void increment()
    {
        this.count++;
    }
    

    /**
     * @return the timeLimit
     */
    public int getTimeLimit()
    {
        return (int)(timeLimit/1000L); // Convert it back to seconds
    }
    
    
    /**
     * Check if we have reached the time limit
     * 
     * @return true if we have reached the time limit
     */
    public boolean isTimeLimitReached()
    {
        long currentTime = System.currentTimeMillis();
        
        return ( timeLimit > 0 ) && ( initialTime + timeLimit < currentTime );
    }
    

    /**
     * @return the initialTime
     */
    public long getInitialTime()
    {
        return initialTime;
    }
    
    
    /**
     * @return The cursor associated with the SearchRequest
     */
    public Cursor<Entry> getCursor()
    {
        return cursor;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        long elapsed = System.currentTimeMillis() - initialTime;
        
        return "SRContainer : <" + searchRequest.getMessageId() + ", nbSent : " + count + ", elapsed : " + elapsed + ">";
    }
}
