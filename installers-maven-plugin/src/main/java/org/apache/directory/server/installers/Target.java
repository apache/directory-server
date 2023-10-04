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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * The superclass for all installer targets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class Target
{
    /** The id */
    private String id;

    /** The name of the operating system */
    private TargetName osName;

    /** The architecture of the operating system */
    private TargetArch osArch;

    /** The final name of the installer file */
    private String finalName;


    /**
     * Gets the final name.
     *
     * @return the final name
     */
    public String getFinalName()
    {
        return finalName;
    }


    /**
     * Sets the final name.
     *
     * @param finalName the final name
     */
    public void setFinalName( String finalName )
    {
        this.finalName = finalName;
    }


    /**
     * Gets the id.
     *
     * @return the id
     */
    public String getId()
    {
        return id;
    }


    /**
     * Sets the id.
     *
     * @param id the id
     */
    public void setId( String id )
    {
        this.id = id;
    }


    /**
     * Gets the OS architecture.
     *
     * @return the OS architecture
     */
    public TargetArch getOsArch()
    {
        return osArch;
    }


    /**
     * Sets the OS architecture.
     *
     * @param osArch the OS architecture
     */
    public void setOsArch( String osArch )
    {
        this.osArch = TargetArch.valueOf( osArch );
    }


    /**
     * Sets the OS architecture.
     *
     * @param osArch the OS architecture
     */
    public void setOsArch( TargetArch osArch )
    {
        this.osArch = osArch;
    }


    /**
     * Gets the OS name.
     *
     * @return the OS name
     */
    public TargetName getOsName()
    {
        return osName;
    }


    /**
     * Sets the OS name.
     *
     * @param osName the OS name
     */
    public void setOsName( String osName )
    {
        this.osName = TargetName.valueOf( osName );
    }


    /**
     * Sets the OS name.
     *
     * @param osName the OS name
     */
    public void setOsName( TargetName osName )
    {
        this.osName = osName;
    }


    /**
     * Indicates if the OS name is 'Linux'.
     * 
     * @return <tt>true</tt> if the OS architecture is Linux 
     */
    public boolean isOsNameLinux()
    {
        return osName == TargetName.OS_NAME_LINUX;
    }


    /**
     * Indicates if the OS name is 'Mac OS X'.
     * 
     * @return <tt>true</tt> if the OS architecture is MAC OS X 
     */
    public boolean isOsNameMacOSX()
    {
        return osName == TargetName.OS_NAME_MAC_OS_X;
    }


    /**
     * Indicates if the OS name is 'Solaris'.
     * 
     * @return <tt>true</tt> if the OS architecture is Solaris 
     */
    public boolean isOsNameSolaris()
    {
        return osName == TargetName.OS_NAME_SOLARIS;
    }


    /**
     * Indicates if the OS name is 'Windows'.
     * 
     * @return <tt>true</tt> if the OS architecture is Windows 
     */
    public boolean isOsNameWindows()
    {
        return osName == TargetName.OS_NAME_WINDOWS;
    }


    /**
     * Indicates if the OS architecture is 'amd64'.
     * 
     * @return <tt>true</tt> if the OS architecture is AMD64 
     */
    public boolean isOsArchAmd64()
    {
        return osArch == TargetArch.OS_ARCH_AMD64;
    }


    /**
     * Indicates if the OS architecture is 'i386'.
     * 
     * @return <tt>true</tt> if the OS architecture is I386 
     */
    public boolean isOsArchI386()
    {
        return osArch == TargetArch.OS_ARCH_I386;
    }


    /**
     * Indicates if the OS architecture is 'Sparc'.
     * 
     * @return <tt>true</tt> if the OS architecture is Sparc 
     */
    public boolean isOsArchSparc()
    {
        return osArch == TargetArch.OS_ARCH_SPARC;
    }


    /**
     * Indicates if the OS architecture is 'x86'.
     * 
     * @return <tt>true</tt> if the OS architecture is X86 
     */
    public boolean isOsArchx86()
    {
        return osArch == TargetArch.OS_ARCH_X86;
    }


    /**
     * Indicates if the OS architecture is 'x86_64'.
     * 
     * @return <tt>true</tt> if the OS architecture is X86_64 
     */
    public boolean isOsArchX86_64()
    {
        return osArch == TargetArch.OS_ARCH_X86_64;
    }
    
    
    public abstract void execute( GenerateMojo mojo ) throws MojoExecutionException, MojoFailureException;
}
