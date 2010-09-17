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
package org.apache.directory.daemon.installers.nsis;

import org.apache.directory.daemon.installers.Target;

import java.io.File;
import java.util.Calendar;

/**
 * A Nullsoft Installer System (NSIS) installer for the Windows platform
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NsisTarget extends Target
{
    private String wrapperExecutablePath = "src/main/installers/wrapper/bin/wrapper-windows-x86-32.exe";
    private File nsisCompiler = new File( "/usr/local/share/nsis/makensis" );
    private File NsisConfigurationFile;


    public NsisTarget()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( System.currentTimeMillis() );
        setCopyrightYear( String.valueOf( cal.get( Calendar.YEAR ) ) );
    }


    public void setNsisCompiler( File nsisCompiler)
    {
        this.nsisCompiler = nsisCompiler;
    }


    public File getNsisCompiler()
    {
        return nsisCompiler;
    }


    public void setNsisConfigurationFile( File nsisConfigurationFile )
    {
        this.NsisConfigurationFile = nsisConfigurationFile;
    }


    public File getNsisConfigurationFile()
    {
        return NsisConfigurationFile;
    }


    public void setWrapperExecutablePath( String wrapperExecutablePath)
    {
        this.wrapperExecutablePath = wrapperExecutablePath;
    }


    public String getWrapperExecutablePath()
    {
        return wrapperExecutablePath;
    }


}
