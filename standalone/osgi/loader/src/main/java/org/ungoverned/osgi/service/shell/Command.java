/*
 * Oscar - An implementation of the OSGi framework.
 * Copyright (c) 2004, Richard S. Hall
 * All rights reserved.
 *  
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *   * Neither the name of the ungoverned.org nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * Contact: Richard S. Hall (heavy@ungoverned.org)
 * Contributor(s):
 *
**/
package org.ungoverned.osgi.service.shell;

import java.io.PrintStream;

/**
 * This interface is used to define commands for the Oscar shell
 * service. Any bundle wishing to create commands for the Oscar
 * shell service simply needs to create a service object that
 * implements this interface and then register it with the OSGi
 * framework. The Oscar shell service automatically includes any
 * registered command services in its list of available commands.
**/
public interface Command
{
    /**
     * Returns the name of the command that is implemented by the
     * interface. The command name should not contain whitespace
     * and should also be unique.
     * @return the name of the command.
    **/
    public String getName();

    /**
     * Returns the usage string for the command. The usage string is
     * a short string that illustrates how to use the command on the
     * command line. This information is used when generating command
     * help information. An example usage string for the <tt>install</tt>
     * command is:
     * <pre>
     *     install <URL> [<URL> ...]
     * </pre>
     * @return the usage string for the command.
    **/
    public String getUsage();

    /**
     * Returns a short description of the command; this description
     * should be as short as possible. This information is used when
     * generating the command help information.
     * @return a short description of the command.
    **/
    public String getShortDescription();

    /**
     * Executes the command using the supplied command line, output
     * print stream, and error print stream.
     * @param line the complete command line, including the command name.
     * @param out the print stream to use for standard output.
     * @param err the print stream to use for standard error.
    **/
    public void execute(String line, PrintStream out, PrintStream err);
}
