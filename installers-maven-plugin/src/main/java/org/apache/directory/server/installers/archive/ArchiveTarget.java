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
package org.apache.directory.server.installers.archive;


import org.apache.directory.server.installers.Target;


/**
 * An archive installer for any platform. We generate 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ArchiveTarget extends Target
{
    /**
     * The archive type.
     * <p>
     * Possible types are:
     * <ul>
     *   <li>zip (default)</li>
     *   <li>tar</li>
     *   <li>tar.gz</li>
     *   <li>tar.bz2</li>
     * </ul>
     */
    private String archiveType = "zip";


    /**
     * Creates a new instance of ArchiveTarget.
     */
    public ArchiveTarget()
    {
        setOsName( Target.OS_NAME_ANY );
        setOsArch( Target.OS_ARCH_ANY );
    }


    /**
     * Gets the archive type.
     *
     * @return
     *      the archive type
     */
    public String getArchiveType()
    {
        return archiveType;
    }


    /**
     * Sets the archive type.
     *
     * @param archiveType The Archive type
     */
    public void setArchiveType( String archiveType )
    {
        this.archiveType = archiveType;
    }
}
