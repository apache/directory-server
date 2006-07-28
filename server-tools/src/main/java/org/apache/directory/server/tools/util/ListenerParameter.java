/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.server.tools.util;


import org.apache.directory.server.tools.ToolCommandListener;


/**
 * This Class defines a ListenerParameter used in the command call to pass
 * arguments to the Command Executor.
 * 
 * 
 */
public class ListenerParameter
{
    private String name;
    private ToolCommandListener listener;


    public ListenerParameter( String name, ToolCommandListener listener )
    {
        this.name = name;
        this.listener = listener;
    }


    public ToolCommandListener getListener()
    {
        return listener;
    }


    public String getName()
    {
        return name;
    }
}
