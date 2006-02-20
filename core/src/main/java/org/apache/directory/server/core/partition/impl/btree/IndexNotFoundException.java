/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.core.partition.impl.btree;


import javax.naming.NamingException;


/**
 * NamingException for missing indicies if full table scans are disallowed.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IndexNotFoundException extends NamingException
{
    private static final long serialVersionUID = 3906088970608981815L;

    /** the name of the index that was not found */
    private final String indexName;


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param indexName the name of the index that was not found 
     */
    public IndexNotFoundException(String indexName)
    {
        super( "Cannot efficiently search the DIB w/o an index on attribute " + indexName
            + "\n. To allow such searches please contact the "
            + "directory\nadministrator to create the index or to enable "
            + "referrals on searches using these\nattributes to a replica with " + "the required set of indices." );
        this.indexName = indexName;
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param message the message associated with the exception.
     * @param indexName the name of the index that was not found 
     */
    public IndexNotFoundException(String message, String indexName)
    {
        super( message );
        this.indexName = indexName;
    }


    /**
     * Constructs an Exception with a detailed message and a root cause 
     * exception.
     * 
     * @param message the message associated with the exception.
     * @param indexName the name of the index that was not found 
     * @param rootCause the root cause of this exception 
     */
    public IndexNotFoundException(String message, String indexName, Throwable rootCause)
    {
        this( message, indexName );
        setRootCause( rootCause );
    }


    /**
     * Gets the name of the attribute the index was missing for.
     *
     * @return the name of the attribute the index was missing for.
     */
    public String getIndexName()
    {
        return indexName;
    }
}
