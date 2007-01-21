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
package org.apache.directory.server.tools.request;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.directory.server.tools.ToolCommand;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.util.ListenerParameter;


/**
 * Interface that defines the operations available for Command Line commands
 *
 */
public interface ToolCommandCL extends ToolCommand
{
    /**
     * Returns the available options for the command
     * @return available options for the command
     */
    public Options getOptions();


    /**
     * Executes the command.
     * @param cmd the command line from the user
     * @param listeners the listeners
     * @throws Exception
     */
    public void execute( CommandLine cmd, ListenerParameter[] listeners ) throws Exception;


    /**
     * Processes the options given by the user.
     * Checks and verifies the data entered by the user.
     * Converts the Options given by the user into Parameters and stores the result in the "params" List
     * @param cmd the command line from the user
     * @throws Exception
     */
    public void processOptions( CommandLine cmd ) throws Exception;


    /**
     * Returns the ToolCommandExecutorStub of the command
     * @return the ToolCommandExecutorStub
     */
    public ToolCommandExecutorStub getStub();
}
