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

package org.apache.directory.server.installers.archive;


/**
 * The list of possible Archive types.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum ArchiveType
{
    ZIP("zip"),
    TAR("tar"),
    TAR_GZ("tar.gz"),
    TAR_BZ2("tar.bz2"),
    UNKNOWN("");

    /** The archive type */
    private String type;


    /**
     * Creates a new instance of ArchiveType.
     */
    private ArchiveType( String type )
    {
        this.type = type;
    }


    /**
     * @return The archive type as a String
     */
    public String getType()
    {
        return type;
    }


    /**
     * @param type The wanted type
     * @return The ArchiveType
     */
    public static ArchiveType getType( String type )
    {
        if ( ZIP.type.equals( type ) )
        {
            return ZIP;
        }

        if ( TAR.type.equals( type ) )
        {
            return TAR;
        }

        if ( TAR_BZ2.type.equals( type ) )
        {
            return TAR_BZ2;
        }

        if ( TAR_GZ.type.equals( type ) )
        {
            return TAR_GZ;
        }

        return UNKNOWN;
    }
}
