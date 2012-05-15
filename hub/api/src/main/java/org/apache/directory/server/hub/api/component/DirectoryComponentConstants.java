/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.hub.api.component;

import org.apache.directory.server.hub.api.meta.DCPropertyDescription;
import org.apache.directory.server.hub.api.meta.DCPropertyType;

public class DirectoryComponentConstants
{
    public static final String DC_PROP_INNER_RECONF_NAME = "ads-inner-reconfiguration";
    public static final String DC_VAL_NULL = "null";
    public static final String DC_PROP_ITEM_PREFIX = "__id_";
    public static final String DC_PROP_ITEM_INDEX_NAME = "ads-collectionindex";

    public static final String DC_COLL_OC_LIST = "ads-collection-list";
    public static final String DC_COLL_OC_SET = "ads-collection-set";
    public static final String DC_COLL_OC_ARRAY = "ads-collection-array";
    
    public static final String DC_LIST_PROP_TYPE = "ads-list-containing";
    public static final String DC_SET_PROP_TYPE = "ads-set-containing";
    public static final String DC_ARRAY_PROP_TYPE = "ads-array-containing";
    
    public static DCPropertyDescription itemDescription = new DCPropertyDescription( DCPropertyType.REFERENCE, "item",
        "", "null", "", false, "" );
}
