package org.apache.directory.server.standalone.installers;


import java.io.File;
import java.util.Locale;


public class Target
{
    /**
     * Possible values:<br />
     *    <ul>
     *      <li>dos</li>
     *      <li>mac</li>
     *      <li>netware</li>
     *      <li>os/2</li>
     *      <li>tandem</li>
     *      <li>unix</li>
     *      <li>windows</li>
     *      <li>win9x</li>
     *      <li>z/os</li>
     *      <li>os/400</li>
     *    </ul>
     */
    public final static String[] OS_FAMILIES = new String[] { 
        "dos", "mac", "netware", "os/2", "tandem", "unix", "windows", "win9x", "z/os", "os/400" };
    public final static String[] OPERATING_SYSTEMS = new String[] { "Linux", "SunOS", "Windows", "Mac OS X" };
    public final static String[] ARCHITECTURES = new String[] { "intel", "sparc", "ppc" };
    public final static String[] DAEMON_FRAMEWORKS = new String[] { "jsvc", "procrun" };
    public final static String[] INSTALLERS = new String[] { "izpack", "inno", "rpm", "deb", "pkg" };

    // required stuff
    private String id;
    private String osName;
    private String osArch;
    private String osFamily;
    private String daemonFramework;
    private String installer;
    
    // optional stuff
    private String minVersion;
    private String maxVersion;
    private String osVersion;
    private File loggerConfigurationFile;
    private File serverConfigurationFile;
    private File bootstrapperConfiguraitonFile;
    
    
    public void setOsName( String osName )
    {
        this.osName = osName.toLowerCase( Locale.US );
    }
    
    
    public String getOsName()
    {
        return osName;
    }


    public void setOsArch( String osArch )
    {
        this.osArch = osArch.toLowerCase( Locale.US );
    }


    public String getOsArch()
    {
        return osArch;
    }


    public void setDaemonFramework(String daemonFramework)
    {
        this.daemonFramework = daemonFramework.toLowerCase( Locale.US );
    }


    public String getDaemonFramework()
    {
        return daemonFramework;
    }


    public void setInstaller(String installer)
    {
        this.installer = installer.toLowerCase( Locale.US );
    }


    public String getInstaller()
    {
        return installer;
    }


    public void setMaxVersion(String maxVersion)
    {
        this.maxVersion = maxVersion.toLowerCase( Locale.US );
    }


    public String getMaxVersion()
    {
        return maxVersion;
    }


    public void setMinVersion(String minVersion)
    {
        this.minVersion = minVersion.toLowerCase( Locale.US );
    }


    public String getMinVersion()
    {
        return minVersion.toLowerCase( Locale.US );
    }


    public void setOsVersion(String osVersion)
    {
        this.osVersion = osVersion.toLowerCase( Locale.US );
    }


    public String getOsVersion()
    {
        return osVersion.toLowerCase( Locale.US );
    }


    public void setId(String id)
    {
        this.id = id;
    }


    public String getId()
    {
        return id;
    }


    public void setLoggerConfigurationFile(File loggerConfigurationFile)
    {
        this.loggerConfigurationFile = loggerConfigurationFile;
    }


    public File getLoggerConfigurationFile()
    {
        return loggerConfigurationFile;
    }


    public void setServerConfigurationFile(File serverConfigurationFile)
    {
        this.serverConfigurationFile = serverConfigurationFile;
    }


    public File getServerConfigurationFile()
    {
        return serverConfigurationFile;
    }


    public void setBootstrapperConfiguraitonFile(File bootstrapperConfiguraitonFile)
    {
        this.bootstrapperConfiguraitonFile = bootstrapperConfiguraitonFile;
    }


    public File getBootstrapperConfiguraitonFile()
    {
        return bootstrapperConfiguraitonFile;
    }


    public void setOsFamily(String osFamily)
    {
        this.osFamily = osFamily;
    }


    public String getOsFamily()
    {
        return osFamily;
    }


    /**
     * Determines if the target OS family matches the given OS family.
     * @param family the family to check for
     * @return true if the target OS matches
     */
    public boolean isFamily( String family )
    {
        return isOs( family, null, null, null );
    }

    
    /**
     * Determines if the target OS matches the given OS name.
     *
     * @param name the OS name to check for
     * @return true if the target OS matches
     */
    public boolean isName( String name )
    {
        return isOs( null, name, null, null );
    }
    

    /**
     * Determines if the target OS matches the given OS architecture.
     *
     * @param arch the OS architecture to check for
     * @return true if the target OS matches
     */
    public boolean isArch( String arch )
    {
        return isOs( null, null, arch, null );
    }
    

    /**
     * Determines if the target OS matches the given OS version.
     *
     * @param version the OS version to check for
     * @return true if the OS matches
     */
    public boolean isVersion( String version )
    {
        return isOs( null, null, null, version );
    }

    
    /**
     * Determines if the OS matches the target's OS family, name, architecture and version
     *
     * @param family   The OS family
     * @param name   The OS name
     * @param arch   The OS architecture
     * @param version   The OS version
     * @return true if the OS matches
     */
    public boolean isOs( String family, String name, String arch, String version )
    {
        boolean retValue = false;

        if ( family != null || name != null || arch != null || version != null )
        {
            boolean isFamily = true;
            boolean isName = true;
            boolean isArch = true;
            boolean isVersion = true;

            if ( family != null )
            {
                if ( family.equals( "windows" ) )
                {
                    isFamily = osName.indexOf( "windows" ) > -1;
                }
                else if ( family.equals( "os/2" ) )
                {
                    isFamily = osName.indexOf( "os/2" ) > -1;
                }
                else if ( family.equals( "netware" ) )
                {
                    isFamily = osName.indexOf( "netware" ) > -1;
                }
                else if ( family.equals( "dos" ) )
                {
                    isFamily = !isFamily( "netware" );
                }
                else if ( family.equals( "mac" ) )
                {
                    isFamily = osName.indexOf( "mac" ) > -1;
                }
                else if ( family.equals( "tandem" ) )
                {
                    isFamily = osName.indexOf( "nonstop_kernel" ) > -1;
                }
                else if ( family.equals( "unix" ) )
                {
                    isFamily = !isFamily( "openvms" )
                        && ( !isFamily( "mac" ) || osName.endsWith( "x" ) );
                }
                else if ( family.equals( "win9x" ) )
                {
                    isFamily = isFamily( "windows" )
                        && ( osName.indexOf( "95" ) >= 0
                        || osName.indexOf( "98" ) >= 0
                        || osName.indexOf( "me" ) >= 0
                        || osName.indexOf( "ce" ) >= 0 );
                }
                else if ( family.equals( "z/os" ) )
                {
                    isFamily = osName.indexOf( "z/os" ) > -1
                        || osName.indexOf( "os/390" ) > -1;
                }
                else if ( family.equals( "os/400" ) )
                {
                    isFamily = osName.indexOf( "os/400" ) > -1;
                }
                else if ( family.equals( "openvms" ) )
                {
                    isFamily = osName.indexOf( "openvms" ) > -1;
                }
            }
            if ( name != null )
            {
                isName = name.equals( osName );
            }
            if ( arch != null )
            {
                isArch = arch.equals( osName );
            }
            if ( version != null )
            {
                isVersion = version.equals( osVersion );
            }
            retValue = isFamily && isName && isArch && isVersion;
        }
        return retValue;
    }
}
