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
 * A Mojo command pattern abstract class.
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

    /** The log4j.properties file name */
    protected static final String LOG4J_PROPERTIES_FILE = "log4j.properties";

    /** The config.ldif file */
    protected static final String CONFIG_LDIF_FILE = "config.ldif";

    /** The wrapper-instance.conf file */
    protected static final String WRAPPER_INSTANCE_CONF_FILE = "wrapper-instance.conf";

    /** The 'default' name */
    protected static final String DEFAULT = "default";

    /** The instances directory */
    protected static final String INSTANCES = "instances";

    /** The instances default directory */
    protected static final String INSTANCE_DEFAULT_DIR = INSTANCES + "/" + DEFAULT;

    /** The 'apacheds' name */
    protected static final String APACHEDS = "apacheds";

    /** The apacheds- prefix */
    protected static final String APACHEDS_DASH = APACHEDS + "-";

    /** The chmod command */
    protected static final String CHMOD = "chmod";

    /** The rights for a command */
    protected static final String RWX_RX_RX = "755";

    /** The os.name property key */
    protected static final String OS_NAME = "os.name";

    /** The local path where the installers are created */
    protected static final String INSTALLERS_PATH = "/org/apache/directory/server/installers/";

    /** The commented wrapper java command */
    protected static final String WRAPPER_JAVA_COMMAND = "# wrapper.java.command=<path-to-java-executable>";

    /** The property keys we are using */
    protected static final String ARCH_PROP = "arch";
    protected static final String INSTALLATION_DIRECTORY_PROP = "installation.directory";
    protected static final String INSTANCES_DIRECTORY_PROP = "instances.directory";
    protected static final String DOUBLE_QUOTE_PROP = "double.quote";
    protected static final String USER_PROP = "user";
    protected static final String GROUP_PROP = "group";
    protected static final String WRAPPER_JAVA_COMMAND_PROP = "wrapper.java.command";
    protected static final String FINAL_NAME_PROP = "finalName";
    protected static final String VERSION_PROP = "version";


    /**
     * Creates a new instance of AbstractMojoCommand.
     *
     * @param mojo the associated mojo
     * @param target the associated target
     */
    public AbstractMojoCommand( GenerateMojo mojo, T target )
    {
        this.mojo = mojo;
        this.target = target;

        log = mojo.getLog();
    }


    /**
     * Executes the command.
     *
     * @throws MojoExecutionException If the execution failed
     * @throws MojoFailureException If the execution failed
     */
    public abstract void execute() throws MojoExecutionException, MojoFailureException;


    /**
     * Gets the filter properties.
     *
     * @return the filter properties
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
     * @return the installation directory file
     */
    public abstract File getInstallationDirectory();


    /**
     * Get the instance directory file.
     *
     * @return the instance directory file
     */
    public abstract File getInstanceDirectory();


    /**
     * Gets the directory associated with the target.
     *
     * @return the directory associated with the target
     */
    protected File getTargetDirectory()
    {
        return new File( mojo.getOutputDirectory(), target.getId() );
    }


    /**
     * Creates both installation and instance layouts.
     * 
     * @throws MojoFailureException If the InstanceLayout cannot be created
     * @throws IOException If the InstanceLayout cannot be created
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
     * @throws MojoFailureException If the InstanceLayout cannot be created
     * @throws IOException If the InstanceLayout cannot be created
     */
    public void createLayouts( boolean includeWrapperDependencies ) throws MojoFailureException, IOException
    {
        log.info( "Creating the installation layout" );
        createInstallationLayout( includeWrapperDependencies );

        log.info( "Creating the instance layout" );
        createInstanceLayout();
    }


    /**
     * Creates the installation layout.
     *      
     * @throws MojoFailureException If the InstanceLayout cannot be created
     * @throws IOException If the InstanceLayout cannot be created
     */
    protected void createInstallationLayout() throws MojoFailureException, IOException
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
     * @throws MojoFailureException If the creation failed
     * @throws IOException If the layout cannot be created
     */
    protected void createInstallationLayout( boolean includeWrapperDependencies ) throws MojoFailureException,
        IOException
    {
        // Getting the installation layout and creating directories
        InstallationLayout installationLayout = getInstallationLayout();

        // Create the installation layout directories
        installationLayout.mkdirs();

        // Copying dependencies artifacts to the lib folder of the installation layout
        MojoHelperUtils.copyDependencies( mojo, installationLayout, includeWrapperDependencies );

        // Copying the LICENSE and NOTICE files
        MojoHelperUtils.copyBinaryFile( mojo, INSTALLERS_PATH + "LICENSE",
            getClass().getResourceAsStream( INSTALLERS_PATH + "LICENSE" ),
            new File( installationLayout.getInstallationDirectory(), "LICENSE" ) );
        MojoHelperUtils.copyBinaryFile( mojo, INSTALLERS_PATH + "NOTICE",
            getClass().getResourceAsStream( INSTALLERS_PATH + "NOTICE" ),
            new File( installationLayout.getInstallationDirectory(), "NOTICE" ) );

        // Copying the 'apacheds' shell script (only for Linux, Solaris or Mac OS X)
        if ( target.isOsNameLinux() || target.isOsNameSolaris() || target.isOsNameMacOSX() )
        {
            MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + APACHEDS,
                getClass().getResourceAsStream( INSTALLERS_PATH + APACHEDS ),
                new File( installationLayout.getBinDirectory(), APACHEDS ), true );

            MojoHelperUtils.exec( new String[]
                { CHMOD, RWX_RX_RX, APACHEDS }, installationLayout.getBinDirectory(), false );
        }

        // Copying the wrappers files (native wrapper executable and library [.jnilib, .so, .dll])
        copyWrapperFiles( mojo );

        // Copying the wrapper configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties,
            INSTALLERS_PATH + "wrapper-installation.conf",
            getClass().getResourceAsStream( INSTALLERS_PATH + "wrapper-installation.conf" ),
            new File( installationLayout.getConfDirectory(), "wrapper.conf" ), true );

    }


    public abstract void copyWrapperFiles( GenerateMojo mojo ) throws MojoFailureException;


    /**
     * Creates the instance layout.
     *
     * @throws IOException If the InatsnaceLayout cannot be created
     */
    protected void createInstanceLayout() throws IOException
    {
        // Getting the instance layout and creating directories
        InstanceLayout instanceLayout = getInstanceLayout();

        instanceLayout.mkdirs();

        // Copying the log4j.properties file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + LOG4J_PROPERTIES_FILE,
            getClass().getResourceAsStream( INSTALLERS_PATH + LOG4J_PROPERTIES_FILE ),
            new File( instanceLayout.getConfDirectory(), LOG4J_PROPERTIES_FILE ), true );

        // Copying the wrapper configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE,
            getClass().getResourceAsStream( INSTALLERS_PATH + WRAPPER_INSTANCE_CONF_FILE ),
            new File( instanceLayout.getConfDirectory(), WRAPPER_INSTANCE_CONF_FILE ), true );

        // Copying ApacheDS LDIF configuration file
        MojoHelperUtils.copyAsciiFile( mojo, filterProperties, INSTALLERS_PATH + CONFIG_LDIF_FILE,
            getClass().getResourceAsStream( INSTALLERS_PATH + CONFIG_LDIF_FILE ),
            new File( instanceLayout.getConfDirectory(), CONFIG_LDIF_FILE ), false );
    }


    /**
     * Gets the installation layout.
     *
     * @return the installation layout
     */
    protected InstallationLayout getInstallationLayout()
    {
        return new InstallationLayout( getInstallationDirectory() );
    }


    /**
     * Gets the instance layout.
     *
     * @return the instance layout
     */
    protected InstanceLayout getInstanceLayout()
    {
        File instanceDirectory = getInstanceDirectory();

        return new InstanceLayout( instanceDirectory );
    }
}
