package org.apache.directory.server.standalone.installers;


import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;


public interface MojoCommand
{
    public abstract void execute() throws MojoExecutionException, MojoFailureException;
}
