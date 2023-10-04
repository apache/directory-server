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

package org.apache.directory.server.installers;


import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;


/**
 * A Deb package for the Debian platform.
 * 
 * To create a Deb package we use the dpkg utility that is bundled in the 
 * Debian operating system.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class LinuxInstallerCommand<T extends Target> extends AbstractMojoCommand<T>
{
    /** Some file postfix */
    protected static final String TAR = "tar";

    /** The etc/init.d script file */
    protected static final String ETC_INITD_SCRIPT = "etc-initd-script";

    /** The /opt/apacheds- root directory */
    protected static final String OPT_APACHEDS_DIR = "/opt/apacheds-";

    /** The /var/lib/apacheds- root directory */
    protected static final String VAR_LIB_APACHEDS_DIR = "/var/lib/apacheds-";

    /** The default instance name property */
    protected static final String DEFAULT_INSTANCE_NAME_PROP = "default.instance.name";
    
    private static final String WRAPPER_BIN_PATH = INSTALLERS_PATH + "wrapper/bin/";

    private static final String WRAPPER_LIB_PATH = INSTALLERS_PATH + "wrapper/lib/lib";

    protected LinuxInstallerCommand( GenerateMojo mojo, T target )
    {
        super( mojo, target );
    }


    /**
     * Copies wrapper files to the installation layout.
     *
     * @param mojo The maven plugin Mojo
     * @throws MojoFailureException If the copy failed
     */
    public void copyWrapperFiles( GenerateMojo mojo ) throws MojoFailureException
    {
        try
        {
            if ( target.isOsArchI386() || target.isOsArchx86() )
            {
                processWrapperFile( mojo, "wrapper-linux-x86-32" );
            }
            // Linux x86_64 & amd64
            else if ( ( target.isOsArchX86_64() || target.isOsArchAmd64() ) )
            {
                processWrapperFile( mojo, "wrapper-linux-x86-64" );
            }
            else
            {
                mojo.getLog().info( "No wrapper files to copy for " + target.getOsArch() );
            }
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
        }
    }
    
    
    private void processWrapperFile( GenerateMojo mojo, String arch ) throws IOException
    {
        mojo.getLog().info( "Copying wrapper files for " + target.getOsArch() );
        
        MojoHelperUtils.copyBinaryFile( mojo, WRAPPER_BIN_PATH + arch,
            getClass().getResourceAsStream( WRAPPER_BIN_PATH + arch ),
            new File( getInstallationLayout().getBinDirectory(), "wrapper" ) );
        
        MojoHelperUtils.copyBinaryFile( mojo, WRAPPER_LIB_PATH + arch + ".so",
            getClass().getResourceAsStream( WRAPPER_LIB_PATH + arch + ".so" ),
            new File( getInstallationLayout().getLibDirectory(), "libwrapper.so" ) );
    }
}
