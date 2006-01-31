/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.apache.launcher;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ungoverned.oscar.Oscar;

/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class Launcher
{
    /** the log for this class */
    private static final Logger log = LoggerFactory.getLogger( Launcher.class );

    /**  Description of the Field */
    private final static String DEFAULT_HOME_KEY = "launcher.home";
    private final static String DEFAULT_PROFILE = "launcher";

    /** Array of launchable program "names", together with classes which contain their main methods */
    private static String[] launchables = { "app1", "app2", "app3", "app4" };

    private static String[] mainClasses = { "com.myco.app1", "com.myco.app2", "com.myco.app3", "com.myco.app4" };

    private static String launcherHome = "";

    /**
     * The main program for the VtLauncher class.
     *
     * @param  argv  The command line arguments
     */
    public static void main( String[] argv )
    {
        if ( argv.length < 1 )
        {
            System.err.println( "Fatal error: must specify program to launch" );
        }

        launcherHome = System.getProperty( DEFAULT_HOME_KEY );
        if ( launcherHome == null )
        {
            throw new IllegalArgumentException( "launcher.home not set" );
        }

        // add properties for log4j.root and vtalk.root for those programs
        // that need it
        String sp = File.separator;
        String p = File.pathSeparator;
        System.setProperty( "log4j.root", launcherHome + sp + "lib" + sp + "log4j.jar" );
        System.setProperty( "vtalk.root", launcherHome + sp + "lib" + sp + "vt.jar" + p + launcherHome + sp + "lib"
                + sp + "userlib.jar" + p + launcherHome + sp + "lib" + sp + "comms.jar" + p + launcherHome + sp + "lib"
                + sp + "osgi.jar" );

        // Set the SAX driver property for XMLRPC to be the Piccolo driver. 
        // Its slightly more compliant than the default one (MinML) supplied 
        // with apache-xmlrpc but is slightly slower. Piccolo handles String 
        // that are set to all spaces, MinML compresses these to empty strings.
        // Piccolo is available from http://piccolo.sourceforge.net/.
        String saxDriver = System.getProperty( "sax.driver" );
        if ( ( saxDriver != null ) && ( saxDriver.trim().length() > 0 ) )
        {
            // Property passed through to the XML-RPC code XMLRpc.java
        }
        else
        {
            // Set the driver to piccolo
            System.setProperty( "sax.driver", "com.bluecast.xml.Piccolo" );
        }

        // Strip off launch target name from args
        String launchTarget = argv[ 0 ];
        String[] launchArgs = new String[ argv.length - 1 ];
        System.arraycopy( argv, 1, launchArgs, 0, argv.length - 1 );

        try
        {
            if ( launchTarget.equals( "launcher " ) )
            {
                launchVtmp( launchArgs );
            }
            else
                if ( launchTarget.startsWith( "vwin" ) )
                {
                    launchVwin( launchTarget, launchArgs );
                }
                else
                {
                    launchProg( launchTarget, launchArgs, null );
                }
        }
        catch ( Exception e )
        {
            System.err.println( "Error launching: " + launchTarget + "," + e );
            e.printStackTrace( System.err );
        }
    }

    private static void addJarURLs( List list, File dir ) throws Exception
    {
        File[] all = dir.listFiles();

        for ( int ix = 0; ix < all.length; ix++ )
        {
            // ick, sure there's a better way to do this
            if ( all[ ix ].toString().toLowerCase().endsWith( ".jar" ) )
            {
                list.add( all[ ix ].toURL() );
            }
        }
    }

    private static ClassLoader createClassLoader() throws Exception
    {
        //TODO: consider making this use/load encrypted class files

        List urlList = new ArrayList();
        addJarURLs( urlList, new File( launcherHome + File.separator + "bin" ) );
        addJarURLs( urlList, new File( launcherHome + File.separator + "lib" ) );

        // see if we can find tools.jar (used by VCOMP)
        File javaHome = new File( System.getProperty( "java.home" ) );
        File toolsJar = new File( javaHome, "lib" + File.separator + "tools.jar" );

        if ( !toolsJar.exists() )
        {
            toolsJar = new File( javaHome.getParent(), "lib" + File.separator + "tools.jar" );
        }
        //TODO: should we put out warning if not found?
        urlList.add( toolsJar.toURL() );

        URL[] urls = (URL[]) urlList.toArray( new URL[ 0 ] );
        return new URLClassLoader( urls );
    }

    private static void deleteOscarHome( String profile ) throws IOException
    {
        StringBuffer temp = new StringBuffer( 10 );

        String userHome = System.getProperty( "user.home" );
        File home = new File( userHome );

        temp.append( ".oscar" + File.separator + profile );
        File oscarHome = new File( home, temp.toString() );

        Launcher.deleteFiles( oscarHome );
    }

    /**
     * Delete all files and directories under the file.
     *
     * @param  file             root of files to delete
     * @exception  IOException  Exception on delete
     */
    private static void deleteFiles( File file ) throws IOException
    {
        File[] list = null;
        int idx = 0;

        if ( !file.exists() )
        {
            return;
        }

        if ( file.isDirectory() )
        {
            list = file.listFiles();

            for ( idx = 0; idx < list.length; idx++ )
            {
                deleteFiles( list[ idx ] );
            }
            if ( !file.delete() )
            {
                throw new IOException( "Unable to delete file " + file.getAbsolutePath() );
            }
        }
        else
        {
            if ( !file.delete() )
            {
                throw new IOException( "Unable to delete file " + file.getAbsolutePath() );
            }
        }
    }

    /**
     * Launcher for the VT/MP platform itself.
     *
     * @param  argv  The command line arguments
     */
    private static void launchVtmp( String[] argv ) throws Exception
    {
        String profile = DEFAULT_PROFILE;
        if ( argv.length > 0 )
        {
            profile = new String( argv[ 0 ] );
        }

        // always assume coldstart for now        
        try
        {
            Launcher.deleteOscarHome( profile );
        }
        catch ( IOException e )
        {
            log.debug( "Error removing bundle cache : " + e );
            //System.exit(-1);
        }

        // use same name for properties files and oscar profile
        launchOscar( profile, profile, true );
    }

    /**
     * Launcher for the VWIN.
     *
     * @param  argv  The command line arguments
     */
    private static void launchVwin( String target, String[] argv ) throws Exception
    {
        boolean coldstart = true;
        String clientConfig = null;

        int i = 0;

        while ( i < argv.length )
        {
            if ( argv[ i ].compareToIgnoreCase( "warmstart" ) == 0 )
            {
                coldstart = false;
            }
            else
            {
                clientConfig = argv[ i ];
                File clientConfigFile = new File( clientConfig );
                try
                {
                    File dir = clientConfigFile.getParentFile();
                    if ( dir != null && !dir.exists() )
                    {
                        dir.mkdirs();
                    }

                    clientConfigFile.createNewFile();
                    // Setup the property so that VWIN uses the
                    // specified client config
                    System.setProperty( "launcher.ui.vwin.property_file", clientConfigFile.getAbsolutePath() );
                }
                catch ( Exception e )
                {
                    System.err.println( "Error accessing client config file " + clientConfigFile + " : " + e );
                    return;
                }

            }
            i++;
        }

        // Try to use a profile
        int profileOccurence = 1;
        String profile = target;
        boolean profileInUse = false;
        if ( coldstart )
        {
            do
            {
                try
                {
                    Launcher.deleteOscarHome( profile );
                    profileInUse = false;
                }
                catch ( IOException e )
                {
                    profileInUse = true;
                    profile = target + "o" + profileOccurence;
                    profileOccurence++;
                    if ( profileOccurence > 100 )
                    {
                        log.debug( "Error starting profile: " + target + " : " + e );
                        return;
                    }

                }
            }
            while ( profileInUse );
        }

        launchOscar( target, profile, false );
    }

    /**
     * Launcher for Oscar
     */
    private static void launchOscar( String propsName, String profile, boolean embedded ) throws Exception
    {
        String sysProps = launcherHome + File.separator + "etc" + File.separator + propsName + ".system.properties";
        String bundleProps = launcherHome + File.separator + "etc" + File.separator + propsName + ".bundle.properties";

        // used for 1.0.2+ versions of Oscar
        System.setProperty( "oscar.cache.profile", profile );
        System.setProperty( "oscar.system.properties", sysProps );
        System.setProperty( "oscar.bundle.properties", bundleProps );
        System.setProperty( "oscar.strict.osgi", "false" );
        System.setProperty( "oscar.embedded.execution", embedded ? "true" : "false" );

        Oscar main = new Oscar();
    }

    /**
     * Launcher for all other VT/MP support programs
     *
     * @param  argv  The command line arguments
     */
    private static void launchProg( String target, String[] argv, ClassLoader loader ) throws Exception
    {
        //TODO: include debug detect/defeat code
        if ( loader == null )
        {
            loader = createClassLoader();
        }

        // for sax, xerces etc.
        Thread.currentThread().setContextClassLoader( loader );

        Class cls = null;

        if ( target.equals( "custom" ) )
        {
            // use -D specified main class
            cls = loader.loadClass( System.getProperty( "launch.custom" ) );
        }
        else
        {
            // find program's main class from lookup table 
            for ( int ix = 0; ix < mainClasses.length && cls == null; ix++ )
            {
                if ( launchables[ ix ].equals( target ) )
                {
                    cls = loader.loadClass( mainClasses[ ix ] );
                }
            }
        }

        if ( cls != null )
        {
            // launch the program's main method
            Method entry = cls.getMethod( "main", new Class[] { String[].class } );
            entry.invoke( null, new Object[] { argv } );
            return;
        }

        throw new ClassNotFoundException( "main class for " + target );
    }
}
