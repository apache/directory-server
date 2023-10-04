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
package org.apache.directory.server.installers.rpm;


import org.apache.directory.server.installers.GenerateMojo;
import org.apache.directory.server.installers.Target;
import org.apache.directory.server.installers.TargetArch;
import org.apache.directory.server.installers.TargetName;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


/**
 * An Rpm package target. The default OsName parameter is Linux.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RpmTarget extends Target
{
    /** The tool to use to generate RPM files */
    private String rpmbuildUtility = "/usr/bin/rpmbuild";
    
    public String getRpmbuildUtility()
    {
        return rpmbuildUtility;
    }

    public void setRpmbuildUtility( String rpmbuildUtility )
    {
        this.rpmbuildUtility = rpmbuildUtility;
    }

    /**
     * Creates a new instance of RpmTarget.
     */
    public RpmTarget()
    {
        setOsName( TargetName.OS_NAME_LINUX );
        setOsArch( TargetArch.OS_ARCH_I386 );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void execute( GenerateMojo mojo ) throws MojoExecutionException, MojoFailureException
    {
        RpmInstallerCommand rpmCmd = new RpmInstallerCommand( mojo, this );
        rpmCmd.execute();
    }
}
