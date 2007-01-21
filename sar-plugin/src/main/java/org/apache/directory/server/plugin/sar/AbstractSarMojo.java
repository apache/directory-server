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
package org.apache.directory.server.plugin.sar;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.IOUtil;

public abstract class AbstractSarMojo
    extends AbstractMojo
{

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The directory containing generated classes.
     *
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     * @readonly
     */
    private File classesDirectory;

    /**
     * The directory where the sar is built.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}"
     * @required
     */
    private File sarDirectory;

    /**
     * The location of the jboss-service.xml file.  If it is present in the META-INF
     * directory in src/main/resources with that name then it will automatically be 
     * included.  Otherwise this parameter must be set.
     *
     * @parameter 
     */
    private File jbossServiceFile;

    /**
     * The directory where to put the libs.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}/lib"
     * @required
     */
    private File libDirectory;

    /**
     * 
     */
    public abstract void execute()
        throws MojoExecutionException;

    /**
     * 
     * @return
     */
    public MavenProject getProject()
    {

        return project;
    }

    /**
     * 
     * @return
     */
    public File getSarDirectory()
    {

        return sarDirectory;
    }
    
    /**
     * 
     * @throws MojoExecutionException
     */
    public void buildExplodedSAR()
        throws MojoExecutionException
    {
        buildExplodedSAR( Collections.EMPTY_SET );
    }
    
    
    /**
     * 
     * @throws MojoExecutionException
     */
    public void buildExplodedSAR( Set excludes )
        throws MojoExecutionException
    {
        getLog().info( "Exploding sar..." );

        if ( excludes == null )
        {
            excludes = Collections.EMPTY_SET;
        }
        
        sarDirectory.mkdirs();
        libDirectory.mkdirs();
        try
        {
            getLog().info( "Assembling sar " + project.getArtifactId() + " in " + sarDirectory );

            if ( classesDirectory.exists() && ( !classesDirectory.equals( sarDirectory ) ) )
            {
                FileUtils.copyDirectoryStructure( classesDirectory, sarDirectory );
            }
            
            File jbossServiceFileTarget = new File( sarDirectory, "META-INF" );
            jbossServiceFileTarget = new File( jbossServiceFileTarget, "jboss-service.xml" );
            if ( ! jbossServiceFileTarget.exists() )
            {
                if ( ! jbossServiceFileTarget.getParentFile().exists() )
                {
                    jbossServiceFileTarget.getParentFile().mkdirs();
                }
                
                if ( jbossServiceFile == null || ! jbossServiceFile.exists() )
                {
                    throw new MojoExecutionException( "Could not find the jboss-service.xml file." );
                }
                else 
                {
                    FileUtils.copyFile( jbossServiceFile, jbossServiceFileTarget );
                }
            }
            
            Set artifacts = project.getArtifacts();
            List rejects = new ArrayList();
            getLog().info( "");
            getLog().info( "    Including artifacts: ");
            getLog().info( "    -------------------");
            for ( Iterator iter = artifacts.iterator(); iter.hasNext(); )
            {
                Artifact artifact = (Artifact) iter.next();
                ScopeArtifactFilter filter = new ScopeArtifactFilter( Artifact.SCOPE_RUNTIME );
                if ( !artifact.isOptional() && filter.include( artifact ) )
                {
                    String type = artifact.getType();
                    String descriptor = artifact.getGroupId() + ":" + artifact.getArtifactId();

                    if ( "jar".equals( type ) && ! excludes.contains( descriptor ) )
                    {
                        getLog().info( "        o " + descriptor );

                        // copy jar skipping maven .xml files which would
                        // upset the JBoss JARDeployer.
                        ZipInputStream zis = new ZipInputStream(new FileInputStream(artifact.getFile()));
                        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
                                new File(libDirectory, artifact.getFile().getName())));

                        try {
                            ZipEntry ze = null;
                            while(null != (ze = zis.getNextEntry())) {
                                String name = ze.getName();
                                getLog().debug( "           - " + name );
                                if(name.startsWith("META-INF/maven") && 
                                    name.endsWith(".xml")) {
                                    getLog().info( "           ! skipping " + name );
                                    continue;
                                }
                                zos.putNextEntry(ze);
                                IOUtil.copy(zis, zos);
                            }
                        } finally {
                            zis.close();
                            zos.close();
                        }                    
                    }
                    else
                    {
                        rejects.add( artifact );
                    }
                }
            }
            
            if ( ! excludes.isEmpty() )
            {
                getLog().info( "" );
                getLog().info( "    Excluded artifacts: ");
                getLog().info( "    ------------------");
                for ( int ii = 0; ii < rejects.size(); ii++ )
                {
                    getLog().info( "        o " + rejects.get( ii ) );
                }
            }
            else
            {
                getLog().info( "No artifacts have been excluded.");
            }
            getLog().info( "" );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Could not explode sar...", e );
        }
    }
}
