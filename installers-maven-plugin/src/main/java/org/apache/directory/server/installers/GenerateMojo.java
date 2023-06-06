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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.server.installers.archive.ArchiveInstallerCommand;
import org.apache.directory.server.installers.archive.ArchiveTarget;
import org.apache.directory.server.installers.bin.BinInstallerCommand;
import org.apache.directory.server.installers.bin.BinTarget;
import org.apache.directory.server.installers.deb.DebInstallerCommand;
import org.apache.directory.server.installers.deb.DebTarget;
import org.apache.directory.server.installers.macosxpkg.MacOsXPkgInstallerCommand;
import org.apache.directory.server.installers.macosxpkg.MacOsXPkgTarget;
import org.apache.directory.server.installers.nsis.NsisInstallerCommand;
import org.apache.directory.server.installers.nsis.NsisTarget;
import org.apache.directory.server.installers.rpm.RpmInstallerCommand;
import org.apache.directory.server.installers.rpm.RpmTarget;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;


/**
 * Creates platform specific installation layout images.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class GenerateMojo extends AbstractMojo
{
    /**
     * The target directory into which the mojo creates os and platform 
     * specific images.
     */
    @Parameter(defaultValue = "${project.build.directory}/installers")
    private File outputDirectory;

    /**
     * The associated maven project.
     */
    @Parameter(property = "project", readonly = true, required = true)
    private MavenProject project;

    /**
     * The RPM installer targets.
     */
    @Parameter
    private RpmTarget[] rpmTargets;

    /**
     * The Mac OS X installer targets.
     */
    @Parameter
    private MacOsXPkgTarget[] macOsXPkgTargets;

    /**
     * The NSIS installer targets.
     */
    @Parameter
    private NsisTarget[] nsisTargets;

    /**
     * The Debian installer targets.
     */
    @Parameter
    private DebTarget[] debTargets;

    /**
     * The Binary installer targets.
     */
    @Parameter
    private BinTarget[] binTargets;

    /**
     * The Archive installers targets.
     */
    @Parameter
    private ArchiveTarget[] archiveTargets;

    /**
     *  The dpkg utility executable.
     */
    @Parameter(property = "installers.dpkg", defaultValue = "/usr/bin/dpkg")
    private File dpkgUtility;

    /**
     *  The PackageMaker utility executable.
     */
    @Parameter(
            property = "installers.packageMaker",
            defaultValue = "/Developer/Applications/Utilities/PackageMaker.app/Contents/MacOS/PackageMaker")
    private File packageMakerUtility;

    /**
     *  The makensis utility executable.
     */
    @Parameter(
            property = "installers.makensis",
            defaultValue = "/usr/bin/makensis")
    private File makensisUtility;

    /**
     *  The rpmbuild utility executable.
     */
     @Parameter(
             property = "installers.rpmbuild",
             defaultValue = "/usr/bin/rpmbuild")
    private File rpmbuildUtility;

    /** The list containing all the targets */
    private List<Target> allTargets = new ArrayList<>();


    /**
     * {@inheritDoc}
     */
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Collecting all targets 
        collectAllTargets();

        // Returning if there is no target to build 
        if ( allTargets.isEmpty() )
        {
            getLog().info( "No installers to generate." );

            return;
        }

        getLog().info( "-------------------------------------------------------" );

        // Creating the output directory
        getLog().info( "Creating the putput directory : " + outputDirectory.getAbsolutePath() );
        FileUtils.mkdir( outputDirectory.getAbsolutePath() );

        // Generating installers for all targets
        for ( Target target : allTargets )
        {
            getLog().info( "Executing target '" + target.getId() + "'" );
            getLog().info( "--------------------" );
            getLog().info( "OS Name: " + target.getOsName() );
            getLog().info( "OS Arch: " + target.getOsArch() );
            getLog().info( "--------------------" );

            // Archive target
            if ( target instanceof ArchiveTarget )
            {
                ArchiveInstallerCommand archiveCmd = new ArchiveInstallerCommand( this, ( ArchiveTarget ) target );
                archiveCmd.execute();
            }

            // Bin target
            if ( target instanceof BinTarget )
            {
                BinInstallerCommand binCmd = new BinInstallerCommand( this, ( BinTarget ) target );
                binCmd.execute();
            }

            // Deb target
            if ( target instanceof DebTarget )
            {
                DebInstallerCommand debCmd = new DebInstallerCommand( this, ( DebTarget ) target );
                debCmd.execute();
            }

            // Mac OS X PKG target
            if ( target instanceof MacOsXPkgTarget )
            {
                MacOsXPkgInstallerCommand pkgCmd = new MacOsXPkgInstallerCommand( this, ( MacOsXPkgTarget ) target );
                pkgCmd.execute();
            }

            // NSIS target
            if ( target instanceof NsisTarget )
            {
                NsisInstallerCommand nsisCmd = new NsisInstallerCommand( this, ( NsisTarget ) target );
                nsisCmd.execute();
            }

            // RPM target
            if ( target instanceof RpmTarget )
            {
                RpmInstallerCommand rpmCmd = new RpmInstallerCommand( this, ( RpmTarget ) target );
                rpmCmd.execute();
            }

            getLog().info( "-------------------------------------------------------" );
        }
    }


    /**
     * Collects all targets. A target is a plugin configuration element
     * where we declare a tuple with the target's ID, the target
     * name, architecture, os name and archive typelike in :
     * <pre>
     * &lt;nsisTargets&gt;
     *   &lt;nsisTarget&gt;
     *     &lt;id&gt;apacheds-win32&lt;/id&gt;
     *     &lt;finalName&gt;apacheds-${project.version}.exe&lt;/finalName&gt;
     *   &lt;/nsisTarget&gt;
     * &lt;/nsisTargets&gt;
     * </pre>
     * We have targets for windows, RPM, Deb, macOSX, binary and archive,
     * and we may have more than one, depending on the compression scheme (zip, 
     * gz, bz2).
     */
    private void collectAllTargets()
    {
        addAllTargets( allTargets, nsisTargets ); // For Windows
        addAllTargets( allTargets, rpmTargets ); // For RPM base linux
        addAllTargets( allTargets, debTargets ); // For Debian based Linux
        addAllTargets( allTargets, macOsXPkgTargets ); // For Mac OSX
        addAllTargets( allTargets, binTargets ); // Pure linux 
        addAllTargets( allTargets, archiveTargets ); // tar
    }


    /**
     * Adds an array of targets to the given list.
     *
     * @param list the list of targets
     * @param array an array of targets
     */
    private void addAllTargets( List<Target> list, Target[] array )
    {
        if ( ( list != null ) && ( array != null ) )
        {
            list.addAll( Arrays.asList( array ) );
        }
    }


    /**
     * Gets the output directory.
     *
     * @return the output directory
     */
    public File getOutputDirectory()
    {
        return outputDirectory;
    }


    /**
     * Gets the associated Maven project.
     *
     * @return the associated Maven project
     */
    public MavenProject getProject()
    {
        return project;
    }


    /**
     * Gets the dpkg utility.
     *
     * @return the dpkg utility
     */
    public File getDpkgUtility()
    {
        return dpkgUtility;
    }


    /**
     * Gets the dpkg utility.
     *
     * @return the dpkg utility
     */
    public File getPackageMakerUtility()
    {
        return packageMakerUtility;
    }


    /**
     * Gets the makensis utility.
     *
     * @return
     *      the dpkg utility
     */
    public File getMakensisUtility()
    {
        return makensisUtility;
    }


    public File getRpmbuildUtility()
    {
        return rpmbuildUtility;
    }
}
