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

package org.apache.directory.server.core.api.partition;

/**
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Subordinates
{
    /** The number of direct children */
    long nbChildren = -1L;
    
    /** The number of subordinates */
    long nbSubordinates = -1L;

    /**
     * @return the nbChildren
     */
    public long getNbChildren()
    {
        return nbChildren;
    }

    
    /**
     * @param nbChildren the nbChildren to set
     */
    public void setNbChildren( long nbChildren )
    {
        this.nbChildren = nbChildren;
    }

    
    /**
     * @return the nbSubordinates
     */
    public long getNbSubordinates()
    {
        return nbSubordinates;
    }

    
    /**
     * @param nbSubordinates the nbSubordinates to set
     */
    public void setNbSubordinates( long nbSubordinates )
    {
        this.nbSubordinates = nbSubordinates;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "<NbChildren:" + nbChildren + ", nbSubordinates:" + nbSubordinates + ">";
    }
}
