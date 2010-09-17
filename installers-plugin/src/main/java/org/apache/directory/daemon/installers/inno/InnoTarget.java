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
package org.apache.directory.daemon.installers.inno;


import java.io.File;
import java.util.Calendar;

import org.apache.directory.daemon.installers.Target;


/**
 * An Inno installer target for Windows platforms.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InnoTarget extends Target
{
    private String prunmgrExecutablePath = "src\\main\\installers\\wrapper\\bin\\wrapper-windows-x86-32.exe";
    private String prunsrvExecutablePath = "src\\main\\installers\\prunsrv.exe";
    private File innoCompiler = new File( "C:\\Program Files\\Inno Setup 5\\ISCC.exe" );
    private File innoConfigurationFile;


    public InnoTarget()
    {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( System.currentTimeMillis() );
        setCopyrightYear( String.valueOf( cal.get( Calendar.YEAR ) ) );
    }


    public void setInnoCompiler( File innoCompiler )
    {
        this.innoCompiler = innoCompiler;
    }


    public File getInnoCompiler()
    {
        return innoCompiler;
    }


    public void setInnoConfigurationFile( File innoConfigurationFile )
    {
        this.innoConfigurationFile = innoConfigurationFile;
    }


    public File getInnoConfigurationFile()
    {
        return innoConfigurationFile;
    }


    public void setPrunmgrExecutablePath( String prunmgrExecutablePath )
    {
        this.prunmgrExecutablePath = prunmgrExecutablePath;
    }


    public String getPrunmgrExecutablePath()
    {
        return prunmgrExecutablePath;
    }


    public void setPrunsrvExecutablePath( String prunsrvExecutablePath )
    {
        this.prunsrvExecutablePath = prunsrvExecutablePath;
    }


    public String getPrunsrvExecutablePath()
    {
        return prunsrvExecutablePath;
    }
}
