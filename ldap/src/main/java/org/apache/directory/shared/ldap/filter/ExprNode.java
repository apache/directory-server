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
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.directory.shared.ldap.filter;


/**
 * Root expression node interface which all expression nodes in the filter
 * expression tree implement.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public interface ExprNode
{
    /**
     * Gets an annotation on the tree by key.
     * 
     * @param a_key
     *            the annotation key.
     * @return the annotation value.
     */
    Object get( Object a_key );


    /**
     * Sets a annotation key to a value.
     * 
     * @param a_key
     *            the annotation key.
     * @param a_value
     *            the annotation value.
     */
    void set( Object a_key, Object a_value );


    /**
     * Tests to see if this node is a leaf or branch node.
     * 
     * @return true if the node is a leaf,false otherwise
     */
    boolean isLeaf();


    /**
     * Recursively appends this String representation of this node and its
     * descendents in prefix notation to a buffer.
     * 
     * @param a_buf
     *            the buffer to append to.
     */
    StringBuffer printToBuffer( StringBuffer a_buf );


    /**
     * Element/node accept method for visitor pattern.
     * 
     * @param a_visitor
     *            the filter expression tree structure visitor
     */
    void accept( FilterVisitor a_visitor );
}
