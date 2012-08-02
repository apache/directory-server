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

package org.apache.directory.server.hub.core.store;


public class StoreSchemaConstants
{
    public static final String SYSTEM_ADMIN_DN = "uid=admin,ou=system";

    public static final String HUB_AT_COLL_CONTAINING = "ads-containing";
    public static final String HUB_AT_COLL_ITEM_INDEX = "ads-itemindex";
    public static final String HUB_AT_COMPONENT_NAME = "ads-instance";

    public static final String HUB_AT_MD_PID = "ads-meta-pid";
    public static final String HUB_AT_MD_VERSION = "ads-meta-version";
    public static final String HUB_AT_MD_CLASSNAME = "ads-meta-classname";
    public static final String HUB_AT_MD_IMPLEMENTS = "ads-meta-implements";
    public static final String HUB_AT_MD_EXTENDS = "ads-meta-extends";
    public static final String HUB_AT_MD_FACTORY = "ads-meta-factory";
    public static final String HUB_AT_MD_EXCLUSIVE = "ads-meta-immutable";
    public static final String HUB_AT_MD_PROP = "ads-meta-property";

    public static final String HUB_AT_PD_NAME = "ads-pd-name";
    public static final String HUB_AT_PD_TYPE = "ads-pd-type";
    public static final String HUB_AT_PD_DEFAULTVAL = "ads-pd-defaultvalue";
    public static final String HUB_AT_PD_DESCRIPTION = "ads-pd-description";
    public static final String HUB_AT_PD_MANDATORY = "ads-pd-mandatory";
    public static final String HUB_AT_PD_CONSTANT = "ads-pd-constant";
    public static final String HUB_AT_PD_CONTAINERFOR = "ads-pd-containerFor";

    public static final String HUB_OC_COMPONENT = "ads-component";
    public static final String HUB_OC_COLLECTION_ITEM = "ads-collection-item";
    public static final String HUB_OC_COLL_LIST = "ads-list";
    public static final String HUB_OC_COLL_SET = "ads-set";
    public static final String HUB_OC_COLL_ARRAY = "ads-array";

    public static final String HUB_OC_METADESC = "ads-meta-descriptor";
    public static final String HUB_OC_PROPERTYDESC = "ads-property-descriptor";
}
