alter system set db_2k_cache_size=200M scope=both;
-- presume OMF else alter system set db_create_file_dest='<path to>/oradata' scope=both;
create tablespace dsorapart_def datafile size 10M;
create temporary tablespace dsorapart_tmp tempfile size 100M;
create tablespace dsorapart_dn datafile size 10M;
create tablespace dsorapart_idx datafile size 100M;
create tablespace dsorapart_entry datafile size 100M blocksize 2K;
create tablespace dsorapart_blob datafile size 100M blocksize 2K;
create tablespace dsorapart_uclu datafile size 100M;
create tablespace dsorapart_uidx datafile size 100M;
create tablespace dsorapart_utab datafile size 100M;

create user dsorapart identified by dsorapart
default tablespace dsorapart_def
temporary tablespace dsorapart_tmp
quota unlimited on dsorapart_dn
quota unlimited on dsorapart_idx
quota unlimited on dsorapart_entry
quota unlimited on dsorapart_blob
quota unlimited on dsorapart_uclu
quota unlimited on dsorapart_uidx
quota unlimited on dsorapart_utab;

grant connect, resource, create any table, create any index, create any cluster to dsorapart;