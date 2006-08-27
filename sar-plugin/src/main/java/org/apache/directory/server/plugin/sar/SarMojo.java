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
import java.io.IOException;
import java.util.Set;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.jar.JarArchiver;
import org.codehaus.plexus.archiver.jar.ManifestException;

/**
 * Build a sar.
 * 
 * @goal sar
 * @phase package
 * @requiresDependencyResolution runtime
 */
public class SarMojo extends AbstractSarMojo
{

  /**
   * The directory for the generated sar.
   * 
   * @parameter expression="${project.build.directory}"
   * @required
   */
  private String outputDirectory;

  /**
   * The name of the generated sar.
   * 
   * @parameter expression="${project.build.finalName}"
   * @required
   */
  private String sarName;

  /**
   * The Jar archiver.
   * 
   * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#jar}"
   * @required
   */
  private JarArchiver jarArchiver;

  /**
   * The maven archive configuration to use.
   * 
   * @parameter
   */
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  /**
   * Artifacts excluded from packaging within the generated sar file.  Use 
   * artifactId:groupId in nested exclude tags.
   * 
   * @parameter
   */
  private Set excludes;
  
  /**
   * Executes the SarMojo on the current project.
   * 
   * @throws MojoExecutionException
   *           if an error occured while building the webapp
   */
  public void execute()
    throws MojoExecutionException
  {

    File sarFile = new File(outputDirectory, sarName + ".sar");

    try {
      performPackaging(sarFile);
    }
    catch (Exception e) {
      throw new MojoExecutionException("Error assembling sar", e);
    }
  }

  /**
   * Generates the sar.
   * 
   * @param sarFile the target sar file
   * @throws IOException
   * @throws ArchiverException
   * @throws ManifestException
   * @throws DependencyResolutionRequiredException
   */
  private void performPackaging(File sarFile)
    throws IOException,
    ArchiverException,
    ManifestException,
    DependencyResolutionRequiredException,
    MojoExecutionException
  {

    buildExplodedSAR( excludes );

    // generate sar file
    getLog().info("Generating sar " + sarFile.getAbsolutePath());
    MavenArchiver archiver = new MavenArchiver();
    archiver.setArchiver(jarArchiver);
    archiver.setOutputFile(sarFile);
    jarArchiver.addDirectory(getSarDirectory());

    // create archive
    archiver.createArchive(getProject(), archive);
    getProject().getArtifact().setFile(sarFile);
  }
}
