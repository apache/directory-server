/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.config.beans;


import org.apache.directory.server.config.ConfigurationElement;


/**
 * A class used to store the IndexBean configuration. It can't be instanciated
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class IndexBean extends AdsBaseBean
{
    /** The index unique identifier */
    @ConfigurationElement(attributeType = "ads-indexAttributeId", isRdn = true)
    private String indexAttributeId;

    @ConfigurationElement(attributeType = "ads-indexHasReverse", isRdn = true)
    private String indexHasReverse;


    /**
     * Create a new IndexBean instance
     */
    protected IndexBean()
    {
    }


    /**
     * @return the indexAttributeId
     */
    public String getIndexAttributeId()
    {
        return indexAttributeId;
    }


    /**
     * @param indexAttributeId the indexAttributeId to set
     */
    public void setIndexAttributeId( String indexAttributeId )
    {
        this.indexAttributeId = indexAttributeId;
    }


    /**
     * @param indexHasReverse the indexHasReverse to set
     */
    public void setIndexHasReverse( String indexHasReverse )
    {
        this.indexHasReverse = indexHasReverse;
    }


    /**
     * @return the indexHasReverse
     */
    public String getIndexHasReverse()
    {
        return indexHasReverse;
    }


    /**
     * {@inheritDoc}
     */
    public String toString( String tabs )
    {
        StringBuilder sb = new StringBuilder();

        sb.append( super.toString( tabs + "  " ) );
        sb.append( tabs ).append( "  indexed attribute ID : " ).append( indexAttributeId ).append( '\n' );
        sb.append( tabs ).append( "  indexed has reverse : " ).append( indexHasReverse ).append( '\n' );

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return toString( "" );
    }
}
