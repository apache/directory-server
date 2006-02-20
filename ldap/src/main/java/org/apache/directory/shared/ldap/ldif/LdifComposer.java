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

/*
 * $Id$
 * $Prologue$
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.ldif;


import org.apache.directory.shared.ldap.util.MultiMap;


/**
 * Builds or composes an Ldap Data Interchange Format representation of an
 * Entry.
 * 
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision$
 */
public interface LdifComposer
{
    /**
     * Builds or composes an Ldap Data Interchange Format representation of an
     * Entry. Entry either should be common or should not be referenced here due
     * to the fact that it is specific to the server side.
     * 
     * @param an_entry
     *            the entry to export to ldif
     * @return the ldif of an entry
     */
    String compose( MultiMap an_entry );
}
