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
package org.apache.directory.server.core.partition.impl.btree;


import java.io.IOException;


/**
 * Interface used to abstract underlying btree used.  Implementors can use
 * this interface to reuse Cursors implemented in this package.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 491471 $
 */
public interface TupleBrowserFactory
{
    long size() throws IOException;

    /**
     * Gets a TupleBrowser positioned just before the first entry if one
     * exists.  The getNext() method should return the first entry from the
     * browser.
     *
     * @return the browser postitioned before the first entry
     * @throws IOException if there are errors accessing the underlying btree
     */
    TupleBrowser beforeFirst() throws IOException;

    /**
     * Gets a TupleBrowser positioned just after the last entry if one
     * exists.  The getPrevious() method should return the last entry from
     * the  browser.
     *
     * @return the browser postitioned after the last entry
     * @throws IOException if there are errors accessing the underlying btree
     */
    TupleBrowser afterLast() throws IOException;


    /**
     * Gets a new browser positioned just before a specific key.  If the key
     * is not present then the browser is position at the "greatest" key it
     * can find just before the key argument.
     *
     * @param key the key to be positioned in front of
     * @return the tuple positioned browser
     * @throws IOException if there are errors accessing the underlying btree
     */
    TupleBrowser beforeKey( Object key ) throws IOException;
}