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




/**
 * The superclass for all installer targets.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class Target
{
    /** The OS name for 'Any' */
    public static final String OS_NAME_ANY = "Any";
    /** The OS name for 'Linux' */
    public static final String OS_NAME_LINUX = "Linux";
    /** The OS name for 'Mac OS X' */
    public static final String OS_NAME_MAC_OS_X = "Mac OS X";
    /** The OS name for 'Solaris' */
    public static final String OS_NAME_SOLARIS = "Solaris";
    /** The OS name for 'Windows' */
    public static final String OS_NAME_WINDOWS = "Windows";

    /** The OS architecture for 'amd64' */
    public static final String OS_ARCH_AMD64 = "amd64";
    /** The OS architecture for 'Any' */
    public static final String OS_ARCH_ANY = "Any";
    /** The OS architecture for 'i386' */
    public static final String OS_ARCH_I386 = "i386";
    /** The OS architecture for 'sparc' */
    public static final String OS_ARCH_SPARC = "sparc";
    /** The OS architecture for 'x86' */
    public static final String OS_ARCH_X86 = "x86";
    /** The OS architecture for 'x86_64' */
    public static final String OS_ARCH_X86_64 = "x86_64";

    /** The id */
    private String id;

    /** The name of the operating system */
    private String osName;

    /** The architecture of the operating system */
    private String osArch;

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
    public String getOsArch()
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
        this.osArch = osArch;
    }


    /**
     * Gets the OS name.
     *
     * @return the OS name
     */
    public String getOsName()
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
        this.osName = osName;
    }


    /**
     * Indicates if the OS name is 'Linux'.
     * 
     * @return <tt>true</tt> if the OS architecture is Linux 
     */
    public boolean isOsNameLinux()
    {
        return Target.OS_NAME_LINUX.equalsIgnoreCase( osName );
    }


    /**
     * Indicates if the OS name is 'Mac OS X'.
     * 
     * @return <tt>true</tt> if the OS architecture is MAC OS X 
     */
    public boolean isOsNameMacOSX()
    {
        return Target.OS_NAME_MAC_OS_X.equalsIgnoreCase( osName );
    }


    /**
     * Indicates if the OS name is 'Solaris'.
     * 
     * @return <tt>true</tt> if the OS architecture is Solaris 
     */
    public boolean isOsNameSolaris()
    {
        return Target.OS_NAME_SOLARIS.equalsIgnoreCase( osName );
    }


    /**
     * Indicates if the OS name is 'Windows'.
     * 
     * @return <tt>true</tt> if the OS architecture is Windows 
     */
    public boolean isOsNameWindows()
    {
        return Target.OS_NAME_WINDOWS.equalsIgnoreCase( osName );
    }


    /**
     * Indicates if the OS architecture is 'amd64'.
     * 
     * @return <tt>true</tt> if the OS architecture is AMD64 
     */
    public boolean isOsArchAmd64()
    {
        return Target.OS_ARCH_AMD64.equalsIgnoreCase( osArch );
    }


    /**
     * Indicates if the OS architecture is 'i386'.
     * 
     * @return <tt>true</tt> if the OS architecture is I386 
     */
    public boolean isOsArchI386()
    {
        return Target.OS_ARCH_I386.equalsIgnoreCase( osArch );
    }


    /**
     * Indicates if the OS architecture is 'Sparc'.
     * 
     * @return <tt>true</tt> if the OS architecture is Sparc 
     */
    public boolean isOsArchSparc()
    {
        return Target.OS_ARCH_SPARC.equalsIgnoreCase( osArch );
    }


    /**
     * Indicates if the OS architecture is 'x86'.
     * 
     * @return <tt>true</tt> if the OS architecture is X86 
     */
    public boolean isOsArchx86()
    {
        return Target.OS_ARCH_X86.equalsIgnoreCase( osArch );
    }


    /**
     * Indicates if the OS architecture is 'x86_64'.
     * 
     * @return <tt>true</tt> if the OS architecture is X86_64 
     */
    public boolean isOsArchX86_64()
    {
        return Target.OS_ARCH_X86_64.equalsIgnoreCase( osArch );
    }
}
