/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.directory.server.standalone.installers;


import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.directory.server.standalone.daemon.InstallationLayout;


/**
 * The superclass for all installer targets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
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

    // required stuff
    private String id;
    private String osName;
    private String osArch;
    private String osFamily;
    private String osVersion;
    private String daemonFramework;
    private File loggerConfigurationFile;
    private File serverConfigurationFile;
    private File bootstrapperConfiguraitonFile;
    
    private InstallationLayout layout;
    private List libArtifacts;
    
    
    private Application application;
    
    
    public void setApplication( Application application )
    {
        this.application = application;
    }
    
    
    public Application getApplication()
    {
        return application;
    }
    
    
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


    public void setOsVersion(String osVersion)
    {
        this.osVersion = osVersion.toLowerCase( Locale.US );
    }


    public String getOsVersion()
    {
        if ( osVersion == null )
        {
            return null;
        }
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

    
    public void setLayout(InstallationLayout layout)
    {
        this.layout = layout;
    }


    public InstallationLayout getLayout()
    {
        return layout;
    }


    public void setLibArtifacts(List libArtifacts)
    {
        this.libArtifacts = libArtifacts;
    }


    public List getLibArtifacts()
    {
        return libArtifacts;
    }
}
