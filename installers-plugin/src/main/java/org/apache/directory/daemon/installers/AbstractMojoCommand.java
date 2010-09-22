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
package org.apache.directory.daemon.installers;


import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.Expand;
import org.codehaus.plexus.util.FileUtils;


/**
 * A Mojo command pattern interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class AbstractMojoCommand<T extends Target>
{
    protected GenerateMojo mojo;
    protected T target;

    protected Map<String, Artifact> dependencyMap;
    protected Log log;


    public abstract void execute() throws MojoExecutionException, MojoFailureException;


    public abstract Properties getFilterProperties();


    public AbstractMojoCommand( GenerateMojo mojo, T target )
    {
        this.mojo = mojo;
        this.target = target;

        log = mojo.getLog();
        dependencyMap = new HashMap<String, Artifact>();

        for ( Iterator ii = mojo.getProject().getDependencyArtifacts().iterator(); ii.hasNext(); /* */)
        {
            Artifact artifact = ( Artifact ) ii.next();
            dependencyMap.put( artifact.getGroupId() + ":" + artifact.getArtifactId(), artifact );
        }
    }

    /**
     * Gets the directory associated with the target.
     *
     * @return
     *      the directory associated with the target
     */
    protected File getTargetDirectory()
    {
        return new File( mojo.getOutputDirectory(), target.getId() );
    }
}
