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
package org.apache.directory.daemon.installers.macosxpkg;


import java.io.File;

import org.apache.directory.daemon.installers.Target;


/**
 * A PKG installer for the Mac OS X platform.
 * 
 * To create a PKG installer we use the PackageMaker utility that is bundled 
 * in the (free) developer tools supplied by Apple.
 * 
 * More information on the use of the PackageMaker utility in the command line
 * can be found at this address: 
 * http://developer.apple.com/documentation/Darwin/Reference/Manpages/man1/packagemaker.1.html
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class MacOsXPkgTarget extends Target
{
    /** The wrapper executable path */
    private String wrapperExecutablePath = "src/main/installers/wrapper/bin/wrapper-windows-x86-32.exe";
    /** The PackageMaker utility executable */
    private File packageMakerUtility = new File(
        "/Developer/Applications/Utilities/PackageMaker.app/Contents/MacOS/PackageMaker" );


    /**
     * Gets the PackageMaker utility.
     *
     * @return
     *      the PackageMaker utility
     */
    public File getPackageMakerUtility()
    {
        return packageMakerUtility;
    }


    /**
     * Gets the Wrapper executable path.
     *
     * @return
     *      the wrapper executable path
     */
    public String getWrapperExecutablePath()
    {
        return wrapperExecutablePath;
    }


    /**
     * Sets the PackageMaker utility.
     *
     * @param packageMakerUtility
     *      the PackageMaker utility
     */
    public void setPackageMakerUtility( File packageMakerUtility )
    {
        this.packageMakerUtility = packageMakerUtility;
    }


    /**
     * Sets the Wrapper executable path.
     *
     * @param wrapperExecutablePath
     *      the wrapper executable path
     */
    public void setWrapperExecutablePath( String wrapperExecutablePath )
    {
        this.wrapperExecutablePath = wrapperExecutablePath;
    }
}