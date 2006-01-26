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
package org.apache.directory.server.standalone.installers.inno;

import java.io.File;

import org.apache.directory.server.standalone.installers.Target;


/**
 * An Inno installer target for Windows platforms.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class InnoTarget extends Target
{
    private File innoCompiler = new File( "C:\\Program Files\\Inno Setup 5\\ISCC.exe" );
    private File innoConfigurationFile;


    public void setInnoCompiler(File innoCompiler)
    {
        this.innoCompiler = innoCompiler;
    }

    
    public File getInnoCompiler()
    {
        return innoCompiler;
    }


    public void setInnoConfigurationFile(File innoConfigurationFile)
    {
        this.innoConfigurationFile = innoConfigurationFile;
    }


    public File getInnoConfigurationFile()
    {
        return innoConfigurationFile;
    }
}
