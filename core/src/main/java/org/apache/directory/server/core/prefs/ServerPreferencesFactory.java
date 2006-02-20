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
package org.apache.directory.server.core.prefs;


import java.util.prefs.Preferences;
import java.util.prefs.PreferencesFactory;

import org.apache.directory.shared.ldap.NotImplementedException;


/**
 * A preferences factory implementation.  Currently the userRoot() preferences
 * are not available and will throw NotImplementedExceptions.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ServerPreferencesFactory implements PreferencesFactory
{
    public Preferences systemRoot()
    {
        return new ServerSystemPreferences();
    }


    public Preferences userRoot()
    {
        throw new NotImplementedException(
            "userRoot() in org.apache.directory.server.prefs.ServerPreferencesFactory not implemented!" );
    }
}
