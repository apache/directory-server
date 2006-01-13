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
package org.apache.ldap.common.message;


/**
 * A subentry request control for listing subentries in a search operation.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubentryRequestControl extends ControlImpl
{
    private static final long serialVersionUID = -8683670301958448735L;
    private final boolean subentryVisibility;


    public SubentryRequestControl( boolean subentryVisibility )
    {
        this.subentryVisibility = subentryVisibility;
    }


    public byte[] getEncodedValue()
    {
        return new byte[0];
    }


    public boolean getSubentryVisibility()
    {
        return subentryVisibility;
    }
}
