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
package org.apache.directory.daemon.installers.rpm;


import java.io.File;

import org.apache.directory.daemon.installers.Target;


/**
 * An Rpm package target.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RpmTarget extends Target
{
    private File rpmBuilder = new File( "/usr/bin/rpmbuild" );
    private File rpmSpecificationFile;
    private File rpmTopDir;
    private boolean doSudo = false;


    public void setRpmBuilder( File rpmBuilder )
    {
        this.rpmBuilder = rpmBuilder;
    }


    public File getRpmBuilder()
    {
        return rpmBuilder;
    }


    public void setRpmSpecificationFile( File rpmConfigurationFile )
    {
        this.rpmSpecificationFile = rpmConfigurationFile;
    }


    public File getRpmSpecificationFile()
    {
        return rpmSpecificationFile;
    }


    public void setDoSudo( boolean doSudo )
    {
        this.doSudo = doSudo;
    }


    public boolean isDoSudo()
    {
        return doSudo;
    }

    public File getRpmTopDir()
    {
        return rpmTopDir;
    }

    public void setRpmTopDir(File rpmTopDir)
    {
        this.rpmTopDir = rpmTopDir;
    }
}
