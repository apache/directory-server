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


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.server.InstallationLayout;
import org.apache.directory.server.InstanceLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;


/**
 * A Mojo command pattern interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractMojoCommand<T extends Target>
{
    /** The filter properties */
    protected Properties filterProperties = new Properties( System.getProperties() );

    /** The associated mojo */
    protected GenerateMojo mojo;

    /** The associated target */
    protected T target;

    /** The logger */
    protected Log log;


    /**
     * Creates a new instance of AbstractMojoCommand.
     *
     * @param mojo
     *      the associated mojo
     * @param target
     *      the associated target
     */
    public AbstractMojoCommand( GenerateMojo mojo, T target )
    {
        this.mojo = mojo;
        this.target = target;

        log = mojo.getLog();

        initializeFilterProperties();
    }


    /**
     * Executes the command.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public abstract void execute() throws MojoExecutionException, MojoFailureException;


    /**
     * Gets the filter properties.
     *
     * @return
     *      the filter properties
     */
    public Properties getFilterProperties()
    {
        return filterProperties;
    }


    /**
     * Initializes filter properties.
     */
    protected void initializeFilterProperties()
    {
        filterProperties.putAll( mojo.getProject().getProperties() );
    }


    /**
     * Gets the installation directory file.
     *
     * @return
     *      the installation directory file
     */
    public abstract File getInstallationDirectory();


    /**
     * Get the instance directory file.
     *
     * @return
     *      the instance directory file
     */
    public abstract File getInstanceDirectory();


    /**
     * Gets the directory associated with the target.
     *
     * @return
     *      the directory associated with the target
     */
    protected File getTargetDirectory()
    {
        return new File( mojo.getOutputDirectory(), target.getId() );
    }


    /**
     * Creates installation layout and copies files to it.
     *
     * @param mojo
     *      the mojo
     * @throws Exception
     */
    public void copyCommonFiles( GenerateMojo mojo ) throws Exception
    {
        // Creating the installation layout and directories
        InstallationLayout installationLayout = new InstallationLayout( getInstallationDirectory() );
        installationLayout.mkdirs();

        // Creating the instance layout and directories
        InstanceLayout instanceLayout = new InstanceLayout( getInstanceDirectory() );
        instanceLayout.mkdirs();

        MojoHelperUtils.copyDependencies( mojo, installationLayout );

        // Copying the LICENSE and NOTICE files
        MojoHelperUtils.copyBinaryFile(
                getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/LICENSE" ),
                new File( installationLayout.getInstallationDirectory(), "LICENSE" ) );
        MojoHelperUtils.copyBinaryFile(
                getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/NOTICE" ),
                new File( installationLayout.getInstallationDirectory(),
                    "NOTICE" ) );

        // Copying wrapper files
        copyWrapperFiles( installationLayout, instanceLayout );

        // Copying the log4j.properties file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/log4j.properties" ),
            new File( instanceLayout.getConfDirectory(), "log4j.properties" ), true );

        // Copying the 'apacheds' script
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/apacheds.init" ),
            new File( installationLayout.getBinDirectory(), "apacheds" ), true );
    }


    /**
     * Copies wrapper files to the installation layout.
     *
     * @param installationLayout
     *      the installation layout
     * @param instanceLayout
     * @throws MojoFailureException
     */
    private void copyWrapperFiles( InstallationLayout installationLayout, InstanceLayout instanceLayout )
        throws MojoFailureException
    {
        // Mac OS X x86
        if ( target.isOsNameMacOSX() && target.isOsArchx86() )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-macosx-universal-32" ), new File(
                        installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-macosx-universal-32.jnilib" ),
                    new File( installationLayout.getLibDirectory(),
                        "libwrapper.jnilib" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Mac OS X x86_64
        if ( target.isOsNameMacOSX() && target.isOsArchX86_64() )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-macosx-universal-64" ), new File(
                        installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-macosx-universal-64.jnilib" ),
                    new File( installationLayout.getLibDirectory(),
                        "libwrapper.jnilib" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Linux i386 & x86
        if ( target.isOsNameLinux() && ( target.isOsArchI386() || target.isOsArchx86() ) )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile(
                    getClass().getResourceAsStream(
                        "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-linux-x86-32" ),
                    new File( installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-linux-x86-32.so" ),
                    new File( installationLayout.getLibDirectory(), "libwrapper.so" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Linux x86_64 & amd64
        if ( target.isOsNameLinux() && ( target.isOsArchX86_64() || target.isOsArchAmd64() ) )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile(
                    getClass().getResourceAsStream(
                        "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-linux-x86-64" ),
                    new File( installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-linux-x86-64.so" ),
                    new File( installationLayout.getLibDirectory(), "libwrapper.so" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Solaris x86
        if ( target.isOsNameSolaris() && target.isOsArchx86() )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-solaris-x86-32" ),
                    new File( installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-solaris-x86-32.so" ), new File(
                        installationLayout.getLibDirectory(),
                    "libwrapper.so" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Solaris Sparc
        if ( target.isOsNameSolaris() && target.isOsArchSparc() )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-solaris-sparc-32" ),
                    new File( installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/libwrapper-solaris-sparc-32.so" ), new File(
                        installationLayout.getLibDirectory(),
                    "libwrapper.so" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Windows
        if ( target.isOsNameWindows() )
        {
            try
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/bin/wrapper-windows-x86-32.exe" ),
                    new File( installationLayout.getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/daemon/installers/wrapper/lib/wrapper-windows-x86-32.dll" ), new File(
                        installationLayout.getLibDirectory(),
                    "libwrapper.so" ) );
            }
            catch ( IOException e )
            {
                throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
            }
        }

        // Wrapper configuration files
        try
        {
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/wrapper-installation.conf" ),
                new File( installationLayout.getConfDirectory(), "wrapper.conf" ), true );
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                getClass().getResourceAsStream( "/org/apache/directory/daemon/installers/wrapper-instance.conf" ),
                new File( instanceLayout.getConfDirectory(), "wrapper.conf" ), true );
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
        }
    }
}
