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
package org.apache.directory.mavibot.btree;


/**
 * Command line options for bulk loader.
 * 
 * Here are the various options :
 * <ul>
 * <li>-c : The configuration directory</li>
 * <li>-clean : delete the content of the output directory</li>
 * <li>-h : gives the list of possible options</li>
 * <li>-i : the LDIF file to be loaded</li>
 * <li>-n : the number of keys stored in each node</li>
 * <li>-o : the directory where the resulting partition will be stored</li>
 * <li>-rid : the replica ID</li>
 * <li>-verify : check that we have loaded all the entries in the MAsterTable</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public enum Option
{
    HELP("-h", "Prints the details of the options"),
    
    INPUT_FILE("-i", "Path of the LDIF file to be used as input"),

    OUT_DIR("-o", "Path of the directory where the data files will be stored"),

    CLEAN_OUT_DIR("-clean", "Deletes the output directory's contents if present"),
    
    NUM_KEYS_PER_NODE("-n", "(optional) The number of keys to be present in each node, default is 16"),

    DS_RID("-rid", "(optional) The RID value to be used in the entryCSN values, default is 1"),

    CONFIG_DIR("-c", "The configuration partition directory"),

    VERIFY_MASTER_TABLE("-verify", "(optional) Verifies the master table by just browsing (entries are not verified)"),
    
    UNKNOWN(null, "Unknown Option");

    private String text;
    private String desc;


    private Option( String text, String desc )
    {
        this.text = text;
        this.desc = desc;
    }


    public String getText()
    {
        return text;
    }


    public String getDesc()
    {
        return desc;
    }


    public static Option getOpt( String opt )
    {
        if ( opt == null )
        {
            return UNKNOWN;
        }

        opt = opt.trim();

        if ( opt.equalsIgnoreCase( HELP.text ) )
        {
            return HELP;
        }

        if ( opt.equalsIgnoreCase( VERIFY_MASTER_TABLE.text ) )
        {
            return VERIFY_MASTER_TABLE;
        }

        if ( opt.equalsIgnoreCase( INPUT_FILE.text ) )
        {
            return INPUT_FILE;
        }

        if ( opt.equalsIgnoreCase( OUT_DIR.text ) )
        {
            return OUT_DIR;
        }

        if ( opt.equalsIgnoreCase( CLEAN_OUT_DIR.text ) )
        {
            return CLEAN_OUT_DIR;
        }

        if ( opt.equalsIgnoreCase( NUM_KEYS_PER_NODE.text ) )
        {
            return NUM_KEYS_PER_NODE;
        }

        if ( opt.equalsIgnoreCase( CONFIG_DIR.text ) )
        {
            return CONFIG_DIR;
        }

        return UNKNOWN;
    }
}
