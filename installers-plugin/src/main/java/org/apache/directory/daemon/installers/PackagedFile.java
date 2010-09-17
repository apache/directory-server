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
package org.apache.directory.daemon.installers;



/**
 * A file packaged within the installer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PackagedFile
{
    /** true if this file is obtained from the local maven repository as a project dependency */
    private boolean dependency = false;
    /** true if this file is a directory to be copied */
    private boolean directory = false;
    /** true if this file is to be filtered to substitute variables */
    private boolean filtered = false;
    /** true if this file is to be made executable */
    private boolean executable = false;
    /** true if this file is to be expanded based on its zip or jar extension to the destination */
    private boolean expandable = false;
    /** 
     * the dependency descriptor (groupId:artifactId) if the file is a dependency or a 
     * path to a file.  If the path is not absolute then it is searched for in the project
     * first under src/main/installers, then from the ${basedir} down.
     */
    private String source;
    /** the destination file or directory path relative to the installation image base */
    private String destinationPath;
    /** the identifier of an installation bundle if this file is part of a bundle */
    private String installationBundleId;
    
    
    public void setDependency( boolean dependency )
    {
        this.dependency = dependency;
    }
    
    
    public boolean isDependency()
    {
        return dependency;
    }


    public void setFiltered( boolean filtered )
    {
        this.filtered = filtered;
    }


    public boolean isFiltered()
    {
        return filtered;
    }


    public void setExecutable( boolean executable )
    {
        this.executable = executable;
    }


    public boolean isExecutable()
    {
        return executable;
    }


    public void setExpandable( boolean expand )
    {
        this.expandable = expand;
    }


    public boolean isExpandable()
    {
        return expandable;
    }


    public void setSource( String sourcePath )
    {
        this.source = sourcePath;
    }


    public String getSource()
    {
        return source;
    }


    public void setDestinationPath( String destinationPath )
    {
        this.destinationPath = destinationPath;
    }


    public String getDestinationPath()
    {
        return destinationPath;
    }


    public void setInstallationBundleId( String installationBundleId )
    {
        this.installationBundleId = installationBundleId;
    }


    public String getInstallationBundleId()
    {
        return installationBundleId;
    }


    public void setDirectory( boolean directory )
    {
        this.directory = directory;
    }


    public boolean isDirectory()
    {
        return directory;
    }
}
