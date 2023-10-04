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
package org.apache.directory.server.installers.macosxpkg;


import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.Target;
import org.apache.directory.server.installers.TargetArch;
import org.apache.directory.server.installers.TargetName;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


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
    /**
     * Creates a new instance of MacOsXPkgTarget.
     */
    public MacOsXPkgTarget()
    {
        setOsName( TargetName.OS_NAME_MAC_OS_X );
        setOsArch( TargetArch.OS_ARCH_X86_64 );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void execute( GenerateMojo mojo ) throws MojoExecutionException, MojoFailureException
    {
        MacOsXPkgInstallerCommand pkgCmd = new MacOsXPkgInstallerCommand( mojo, this );
        pkgCmd.execute();
    }
}
