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
package org.apache.directory.server.standalone.installers.izpack;

import java.io.File;

import org.apache.directory.server.standalone.installers.Target;


/**
 * An IzPack multiplatform installer target.
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class IzPackTarget extends Target
{
    private File izPackInstallFile;
    private File izPackShortcutsWindowsFile;
    private File izPackShortcutsUnixFile;
    private File izPackUserInputFile;
    
    
    public void setIzPackInstallFile(File izPackInstallFile)
    {
        this.izPackInstallFile = izPackInstallFile;
    }
    
    
    public File getIzPackInstallFile()
    {
        return izPackInstallFile;
    }
    
    
    public void setIzPackShortcutsWindowsFile(File izPackShortcutsWindowsFile)
    {
        this.izPackShortcutsWindowsFile = izPackShortcutsWindowsFile;
    }
    
    
    public File getIzPackShortcutsWindowsFile()
    {
        return izPackShortcutsWindowsFile;
    }
    
    
    public void setIzPackShortcutsUnixFile(File izPackShortcutsUnixFile)
    {
        this.izPackShortcutsUnixFile = izPackShortcutsUnixFile;
    }
    
    
    public File getIzPackShortcutsUnixFile()
    {
        return izPackShortcutsUnixFile;
    }
    
    
    public void setIzPackUserInputFile(File izPackUserInputFile)
    {
        this.izPackUserInputFile = izPackUserInputFile;
    }
    
    
    public File getIzPackUserInputFile()
    {
        return izPackUserInputFile;
    }
}
