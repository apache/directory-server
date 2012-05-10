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
