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
package org.apache.directory.server.installers;


import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.directory.server.InstallationLayout;
import org.apache.directory.server.core.api.InstanceLayout;
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
     * Creates both installation and instance layouts.
     * 
     * @throws MojoFailureException
     * @throws IOException
     */
    public void createLayouts() throws MojoFailureException, IOException
    {
        createLayouts( true );
    }


    /**
     * Creates both installation and instance layouts.
     *
     * @param includeWrapperDependencies
     *      <code>true</code> if wrapper dependencies are included,
     *      <code>false</code> if wrapper dependencies are excluded
     *      
     * @throws MojoFailureException
     * @throws IOException
     */
    public void createLayouts( boolean includeWrapperDependencies ) throws MojoFailureException, IOException
    {
        createInstallationLayout( includeWrapperDependencies );
        createInstanceLayout();
    }


    /**
     * Creates the installation layout.
     *      
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void createInstallationLayout() throws MojoFailureException,
        IOException
    {
        createInstallationLayout( true );
    }


    /**
     * Creates the installation layout.
     *
     * @param includeWrapperDependencies
     *      <code>true</code> if wrapper dependencies are included,
     *      <code>false</code> if wrapper dependencies are excluded
     *      
     * @throws MojoFailureException
     * @throws IOException
     */
    protected void createInstallationLayout( boolean includeWrapperDependencies ) throws MojoFailureException,
        IOException
    {
        // Getting the installation layout and creating directories
        InstallationLayout installationLayout = getInstallationLayout();
        installationLayout.mkdirs();

        // Copying dependencies artifacts to the lib folder of the installation layout
        MojoHelperUtils.copyDependencies( mojo, installationLayout, includeWrapperDependencies );

        // Copying the LICENSE and NOTICE files
        MojoHelperUtils.copyBinaryFile(
            getClass().getResourceAsStream( "/org/apache/directory/server/installers/LICENSE" ),
            new File( installationLayout.getInstallationDirectory(), "LICENSE" ) );
        MojoHelperUtils.copyBinaryFile(
            getClass().getResourceAsStream( "/org/apache/directory/server/installers/NOTICE" ),
            new File( installationLayout.getInstallationDirectory(),
                "NOTICE" ) );

        // Copying the 'apacheds' shell script (only for Linux, Solaris or Mac OS X)
        if ( target.isOsNameLinux() || target.isOsNameSolaris() || target.isOsNameMacOSX() )
        {
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
                getClass().getResourceAsStream( "/org/apache/directory/server/installers/apacheds" ),
                new File( installationLayout.getBinDirectory(), "apacheds" ), true );

            MojoHelperUtils.exec( new String[]
                { "chmod", "755", "apacheds" }, installationLayout.getBinDirectory(), false );
        }

        // Copying the wrappers files (native wrapper executable and library [.jnilib, .so, .dll])
        copyWrapperFiles();

        // Copying the wrapper configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass()
                .getResourceAsStream( "/org/apache/directory/server/installers/wrapper-installation.conf" ),
            new File( installationLayout.getConfDirectory(), "wrapper.conf" ), true );
    }


    /**
     * Creates the instance layout.
     *
     * @throws IOException
     */
    protected void createInstanceLayout() throws IOException
    {
        // Getting the instance layout and creating directories
        InstanceLayout instanceLayout = getInstanceLayout();
        instanceLayout.mkdirs();

        // Copying the log4j.properties file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass().getResourceAsStream( "/org/apache/directory/server/installers/log4j.properties" ),
            new File( instanceLayout.getConfDirectory(), "log4j.properties" ), true );

        // Copying the wrapper configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass().getResourceAsStream( "/org/apache/directory/server/installers/wrapper-instance.conf" ),
            new File( instanceLayout.getConfDirectory(), "wrapper.conf" ), true );

        // Copying ApacheDS LDIF configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            getClass().getResourceAsStream( "/org/apache/directory/server/installers/config.ldif" ),
            new File( instanceLayout.getConfDirectory(), "config.ldif" ), false );
    }


    /**
     * Gets the installation layout.
     *
     * @return
     *      the installation layout
     */
    protected InstallationLayout getInstallationLayout()
    {
        return new InstallationLayout( getInstallationDirectory() );
    }


    /**
     * Gets the instance layout.
     *
     * @return
     *      the instance layout
     */
    protected InstanceLayout getInstanceLayout()
    {
        return new InstanceLayout( getInstanceDirectory() );
    }


    /**
     * Copies wrapper files to the installation layout.
     *
     * @param installationLayout
     *      the installation layout
     * @throws MojoFailureException
     */
    private void copyWrapperFiles()
        throws MojoFailureException
    {
        try
        {
            // Mac OS X x86
            if ( target.isOsNameMacOSX() && target.isOsArchx86() )
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/bin/wrapper-macosx-universal-32" ), new File(
                    getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-macosx-universal-32.jnilib" ),
                    new File( getInstallationLayout().getLibDirectory(),
                        "libwrapper.jnilib" ) );
            }

            // Mac OS X x86_64
            if ( target.isOsNameMacOSX() && target.isOsArchX86_64() )
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/bin/wrapper-macosx-universal-64" ), new File(
                    getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-macosx-universal-64.jnilib" ),
                    new File( getInstallationLayout().getLibDirectory(),
                        "libwrapper.jnilib" ) );
            }

            // Linux i386 & x86
            if ( target.isOsNameLinux() && ( target.isOsArchI386() || target.isOsArchx86() ) )
            {
                MojoHelperUtils.copyBinaryFile(
                    getClass().getResourceAsStream(
                        "/org/apache/directory/server/installers/wrapper/bin/wrapper-linux-x86-32" ),
                    new File( getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-linux-x86-32.so" ),
                    new File( getInstallationLayout().getLibDirectory(), "libwrapper.so" ) );
            }

            // Linux x86_64 & amd64
            if ( target.isOsNameLinux() && ( target.isOsArchX86_64() || target.isOsArchAmd64() ) )
            {
                MojoHelperUtils.copyBinaryFile(
                    getClass().getResourceAsStream(
                        "/org/apache/directory/server/installers/wrapper/bin/wrapper-linux-x86-64" ),
                    new File( getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-linux-x86-64.so" ),
                    new File( getInstallationLayout().getLibDirectory(), "libwrapper.so" ) );
            }

            // Solaris x86
            if ( target.isOsNameSolaris() && target.isOsArchx86() )
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/bin/wrapper-solaris-x86-32" ),
                    new File( getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-solaris-x86-32.so" ), new File(
                    getInstallationLayout().getLibDirectory(),
                    "libwrapper.so" ) );
            }

            // Solaris Sparc
            if ( target.isOsNameSolaris() && target.isOsArchSparc() )
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/bin/wrapper-solaris-sparc-32" ),
                    new File( getInstallationLayout().getBinDirectory(), "wrapper" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/libwrapper-solaris-sparc-32.so" ),
                    new File(
                        getInstallationLayout().getLibDirectory(),
                        "libwrapper.so" ) );
            }

            // Windows
            if ( target.isOsNameWindows() )
            {
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/bin/wrapper-windows-x86-32.exe" ),
                    new File( getInstallationLayout().getBinDirectory(), "wrapper.exe" ) );
                MojoHelperUtils.copyBinaryFile( getClass().getResourceAsStream(
                    "/org/apache/directory/server/installers/wrapper/lib/wrapper-windows-x86-32.dll" ), new File(
                    getInstallationLayout().getLibDirectory(),
                    "wrapper.dll" ) );
            }
        }
        catch ( IOException e )
        {
            throw new MojoFailureException( "Failed to copy Tanuki binary files to lib and bin directories" );
        }
    }
}
