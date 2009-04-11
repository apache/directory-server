/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.cursor;


/**
 * An enumeration to represent the various states of a Cursor.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum CursorStateEnum
{
    /** the Cursor has been created and so has not been positioned yet */
    JUST_OPENED,
    
    /** the Cursor is positioned just before the first element */
    BEFORE_FIRST,
    
    /** the Cursor is positioned just after the last element */
    AFTER_LAST,
    
    /** the Cursor is positioned just before an element but not on any element */
    BEFORE_INNER,
    
    /** the Cursor is positioned just after an element but not on any element */
    AFTER_INNER,
    
    /** the Cursor is positioned on the first element */
    ON_FIRST,
    
    /** the Cursor is positioned on the last element */
    ON_LAST,
    
    /** the Cursor is positioned on an element */
    ON_INNER,
    
    /** the Cursor is closed and not operations can be performed on it */
    CLOSED
}
