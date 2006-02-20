/*
 *   @(#) $Id$
 *
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
package org.apache.directory.shared.ldap.aci;


/**
 * An enumeration that represents all micro-operations that makes up LDAP
 * operations.
 * 
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class MicroOperation
{
    // Permissions that may be used in conjunction with any component of
    // <tt>ProtectedItem</tt>s.
    public static final MicroOperation ADD = new MicroOperation( "Add" );

    public static final MicroOperation DISCLOSE_ON_ERROR = new MicroOperation( "DiscloseOnError" );

    public static final MicroOperation READ = new MicroOperation( "Read" );

    public static final MicroOperation REMOVE = new MicroOperation( "Remove" );

    // Permissions that may be used only in conjunction with the entry
    // component.
    public static final MicroOperation BROWSE = new MicroOperation( "Browse" );

    public static final MicroOperation EXPORT = new MicroOperation( "Export" );

    public static final MicroOperation IMPORT = new MicroOperation( "Import" );

    public static final MicroOperation MODIFY = new MicroOperation( "Modify" );

    public static final MicroOperation RENAME = new MicroOperation( "Rename" );

    public static final MicroOperation RETURN_DN = new MicroOperation( "ReturnDN" );

    // Permissions that may be used in conjunction with any component,
    // except entry, of <tt>ProtectedItem</tt>s.
    public static final MicroOperation COMPARE = new MicroOperation( "Compare" );

    public static final MicroOperation FILTER_MATCH = new MicroOperation( "FilterMatch" );

    public static final MicroOperation INVOKE = new MicroOperation( "Invoke" );

    private final String name;


    private MicroOperation(String name)
    {
        this.name = name;
    }


    /**
     * Returns the name of this micro-operation.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
}
