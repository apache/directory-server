set sqlprefix off;

define default_tablespace=dsorapart_def;
define dn_tablespace=dsorapart_dn;
define index_tablespace=dsorapart_idx;
define entry_tablespace=dsorapart_entry;
define blob_tablespace=dsorapart_blob;
define user_index_tablespace=dsorapart_uidx;
define user_cluster_tablespace=dsorapart_uclu;
define user_table_tablespace=dsorapart_utab;

begin
 for c_cur in (select attribute from indexed_attribute) loop
  indexer.drop_index(c_cur.attribute);
 end loop;
end;
/

drop table t_message cascade constraints;

create table t_message 
(	message varchar2(4000 byte), 
	d       timestamp default systimestamp
);

drop table dn cascade constraints;

create table dn
(
    parentdn   varchar2(1024),
    rdn        varchar2(440),
    entryid    number,
    updn       varchar2(1024),
    constraint pk_dn
    primary key (parentdn,rdn)
)
organization index
tablespace &dn_tablespace;

alter table dn add constraint uq_entryid
unique (entryid)
using index tablespace &index_tablespace;

drop sequence seq_entryid;

create sequence seq_entryid;

drop table bvalue cascade constraint;

create table bvalue
(
    bvalueid   number primary key,
    value      blob,
    hash       varchar2(255)
)
organization index
tablespace &blob_tablespace;

create index bvalue_hash_idx on bvalue (hash) tablespace &index_tablespace;

drop sequence seq_bvalueid;

create sequence seq_bvalueid;

drop table cvalue cascade constraint;

create table cvalue
(
    cvalueid   number primary key,
    value      clob,
    hash       varchar2(255)
)
organization index
tablespace &blob_tablespace;

create index cvalue_hash_idx on cvalue (hash) tablespace &index_tablespace;

drop sequence seq_cvalueid;

create sequence seq_cvalueid;

drop cluster entry_cluster including tables;
create cluster entry_cluster (entryid number) tablespace &entry_tablespace;

create index idx_entry_cluster on cluster entry_cluster tablespace &index_tablespace;

create table eattributes
(
    entryid    number,
    name       varchar2(255),
    type       varchar2(1),
    ssvalue    varchar2(4000),
    sbvalue    raw(2000),
    lsvalue    number,
    lbvalue    number,
    constraint fk_sattribute
    foreign key (entryid)
    references dn (entryid)
    on delete cascade
)
cluster entry_cluster (entryid);


drop sequence seq_materialization;
create sequence seq_materialization;

drop table indexed_attribute;

create table indexed_attribute
(
    attribute       varchar2(255) primary key,
    type            number,
    index_type      number,
    equality        varchar2(255)
)
organization index;

drop table filter_materialization;

create table filter_materialization 
(	
	basedn     varchar2(1500), 
	scope      number, 
	filter     varchar2(1500), 
	materialization_id number, 
	constraint pk_materialization primary key (basedn, scope, filter)
)
organization index;

drop type ldap_entry_table;
drop type ldap_entry;
drop type ldap_attribute_table;
drop type ldap_attribute;

create or replace type vc_arr as table of varchar2(2000);
/

create or replace type ldap_attribute as object
(
     name      varchar2(255),
     type      varchar2(1),
     ssvalue   varchar2(4000),
     sbvalue   raw(2000),
     lsvalue   number(32,0),
     lbvalue   number(32,0)
);
/

create or replace type ldap_attribute_table as table of ldap_attribute;
/

create or replace type ldap_entry as object
(
    dn         varchar2(1024),
    updn       varchar2(1024),
    attrs      ldap_attribute_table
);
/

create or replace type ldap_entry_table as table of ldap_entry;
/

create or replace package util
as

    type rsplitteddn is record
    (
        parentdn  varchar2(1024),
        rdn       varchar2(255)
    );

    type vc_arr is table of varchar2(32767) index by binary_integer;

    type num_tb is table of number;

    type vc_tb is table of varchar2(32767);

    function split(p_buffer varchar2, p_sep varchar2)
    return vc_arr;

    function in_num(p_in_list varchar2)
    return num_tb
    pipelined;

    function in_vc(p_in_list varchar2)
    return vc_tb
    pipelined;

    function splitdn(p_reversed_dn varchar2)
    return rsplitteddn;

end util;
/

show errors;

create or replace package body util as

  function split(p_buffer varchar2, p_sep varchar2)
  return vc_arr
  is
      idx      pls_integer;
      list     varchar2(32767) := p_buffer;
      splits   vc_arr;
      cnt      pls_integer:=1;
  begin
      loop
          idx := instr(list,p_sep);
          if idx > 0 then
              splits(cnt):= substr(list,1,idx-1);
              cnt:=cnt+1;
              list := substr(list,idx+length(p_sep));
          else
              splits(cnt):= list;
              exit;
          end if;
      end loop;
      return splits;
  end split;


  function in_num(p_in_list varchar2)
  return num_tb
  pipelined
  as
    v_splits  vc_arr:= split(p_in_list,',');
  begin

    for i in 1..v_splits.count loop
      pipe row(to_number(trim(v_splits(i))));
    end loop;

  end in_num;

  function in_vc(p_in_list varchar2)
  return vc_tb
  pipelined
  as
    v_splits  vc_arr:= split(p_in_list,',');
  begin

    for i in 1..v_splits.last loop
      pipe row(v_splits(i));
    end loop;

  end in_vc;

  function splitdn(p_reversed_dn varchar2)
  return rsplitteddn
  as
   v_splitted_dn  rsplitteddn;
   v_last_comma   pls_integer;
  begin
     v_last_comma:= instr(p_reversed_dn,',',-1);
     v_splitted_dn.parentdn:= nvl(substr(p_reversed_dn,1,v_last_comma),',');
     v_splitted_dn.rdn:= substr(p_reversed_dn,v_last_comma+1);
     return v_splitted_dn;
  end;

end util;
/

show errors;

create or replace package converter
as

  VARCHAR2_SQL_TYPE   constant number:=0;
  NUMBER_SQL_TYPE     constant number:=1;
  DATE_SQL_TYPE       constant number:=2;

  function to_sql_type(p_equality varchar2)
  return number;

  function caseExactMatch(p_value varchar2)
  return varchar2
  deterministic;

  function caseIgnoreMatch(p_value varchar2)
  return varchar2
  deterministic;

  function generalizedTimeMatch(p_value varchar2)
  return date
  deterministic;

  function numericOidMatch(p_value varchar2)
  return varchar2
  deterministic;

  function objectIdentifierMatch(p_value varchar2)
  return varchar2
  deterministic;

  function nameOrNumericIdMatch(p_value varchar2)
  return varchar2
  deterministic;

end converter;
/

show errors;

create or replace package body converter
as

  function to_sql_type(p_equality varchar2)
  return number
  as
  begin
     if p_equality = 'caseExactMatch' then
       return VARCHAR2_SQL_TYPE;
     elsif p_equality = 'caseIgnoreMatch' then
       return VARCHAR2_SQL_TYPE;
     elsif p_equality = 'generalizedTimeMatch' then
       return DATE_SQL_TYPE;
     elsif p_equality = 'numericOidMatch' then
       return VARCHAR2_SQL_TYPE;
     elsif p_equality = 'objectIdentifierMatch' then
       return VARCHAR2_SQL_TYPE;
     elsif p_equality = 'nameOrNumericIdMatch' then
       return VARCHAR2_SQL_TYPE;
     else
       raise_application_error(-20001,'Unknown equlity: '''||p_equality||'''');
     end if;

     return null;

  end to_sql_type;

  function caseExactMatch(p_value varchar2)
  return varchar2
  deterministic
  as
  begin
    return p_value;
  end caseExactMatch;

  function caseIgnoreMatch(p_value varchar2)
  return varchar2
  deterministic
  as
  begin
    return lower(p_value);
  end caseIgnoreMatch;

  function generalizedTimeMatch(p_value varchar2)
  return date
  deterministic
  as
  begin
    return to_date(substr(p_value,1,14),'YYYYMMDDHH24MISS'); -- awful TODO: real function
  end generalizedTimeMatch;

  function numericOidMatch(p_value varchar2)
  return varchar2
  deterministic
  as
  begin
    return lower(trim(p_value));
  end numericOidMatch;

  function objectIdentifierMatch(p_value varchar2)
  return varchar2
  deterministic
  as
  begin
    return lower(trim(p_value));
  end objectIdentifierMatch;

  function nameOrNumericIdMatch(p_value varchar2)
  return varchar2
  deterministic
  as
  begin
    return p_value;
  end nameOrNumericIdMatch;


end converter;
/

show errors;

create or replace package indexer
as

 INDEX_TYPE_NORMAL     constant number:= 0;
 INDEX_TYPE_CLUSTERED  constant number:= 1;
 INDEX_TYPE_UNIQUE     constant number:= 2;

 cursor indexed_attribute(p_name varchar2)
 is
 (select equality from indexed_attribute where attribute= p_name);


  procedure create_index(p_attribute     varchar2,
                         p_equality      varchar2,
                         p_index_type    number default INDEX_TYPE_NORMAL,
                         p_index_reverse boolean default false);

  procedure drop_index(p_attribute varchar2);

  procedure insert_value(p_entryid number,
                         p_name    varchar2,
                         p_value   varchar2);

  procedure update_value(p_entryid   number,
                         p_name      varchar2,
                         p_old_value varchar2,
                         p_new_value varchar2);

  procedure delete_value(p_entryid number,
                         p_name    varchar2,
                         p_value   varchar2);

end indexer;
/

show errors;

create or replace package filter
as

  /* TODO: find a better way to bind variables of filters ?
   *       or tune on need, keep an eye on v$sqlarea
   */

  type refdn is record
  (
      entryid   number,
      dn        varchar2(2000),
      updn      varchar2(2000)
  );

  type refdn_tb is table of refdn;

  type att is record
  (
      entryid   number,
      dn        varchar2(2000),
      name      varchar2(255),
      value     varchar2(4000),
      bvalue    blob
  );

  type att_tb is table of att;

  function parse(p_filter       varchar2,
                 p_basedn       varchar2,
                 p_scope        number,
                 p_countlimit   number default null,
                 p_use_mat      number default 1)
  return varchar2;

  function toids(p_filter       varchar2,
                 p_basedn       varchar2,
                 p_scope        number,
                 p_countlimit   number default null,
                 p_use_mat      number default 1)
  return refdn_tb
  pipelined;

  procedure materialize_ids(p_filter       varchar2,
                            p_basedn       varchar2,
                            p_scope        number);

  procedure remove_materialized_ids(p_filter       varchar2,
                                    p_basedn       varchar2,
                                    p_scope        number);

  procedure insert_value(p_materialization_id number,
                         p_entryid number,
                         p_value   varchar2);

  procedure update_value(p_materialization_id number,
                         p_entryid   number,
                         p_old_value varchar2,
                         p_new_value varchar2);

  procedure delete_value(p_materialization_id number,
                         p_entryid number,
                         p_value   varchar2);


end filter;
/

show errors;

create or replace package body indexer
as

  /*
   * ORA-01031:
   * grant create any table to apacheds
   * grant create any index to apacheds
   */
  procedure create_index(p_attribute    varchar2,
                         p_equality     varchar2,
                         p_index_type   number default INDEX_TYPE_NORMAL,
                         p_index_reverse boolean default false)
  as
   v_attribute varchar2(255):= upper(p_attribute);
   v_type      varchar2(20);
   v_equality  varchar2(255):= p_equality;
   v_oid       varchar2(255);
   v_reverse   varchar2(255);
   v_sql_type  number;
  begin

    v_sql_type:= converter.to_sql_type(v_equality);

     if v_sql_type = converter.varchar2_sql_type then
       v_type:= 'varchar2(4000)';
     elsif v_sql_type = converter.number_sql_type then
       v_type:= 'number';
     else
       v_type:= 'date';
     end if;

     if p_index_reverse then
       v_reverse:= 'REVERSE';
     end if;

     if p_index_type = INDEX_TYPE_CLUSTERED then
       execute immediate 'create cluster "'||v_attribute||'$C" (value '||v_type||') tablespace dsorapart_uidx';
       execute immediate 'create index "'||v_attribute||'$I" on cluster "'||v_attribute||'$C" '||v_reverse||' tablespace dsorapart_uclu';
       execute immediate 'create table "'||v_attribute||'" (entryid, value) cluster "'||v_attribute||'$C" (value) as select entryid, converter.'||v_equality||'(ssvalue) from eattributes where name= '''||lower(v_attribute)||''' order by 2';
     else
       execute immediate 'create table "'||v_attribute||'" (entryid, value) tablespace dsorapart_utab as select entryid, converter.'||v_equality||'(ssvalue) from eattributes where name= '''||lower(v_attribute)||''' order by 2';

       if p_index_type = INDEX_TYPE_UNIQUE then
          execute immediate 'create unique index "'||v_attribute||'$I" on "'||v_attribute||'" (value) '||v_reverse||' tablespace dsorapart_uidx';
       else
          execute immediate 'create index "'||v_attribute||'$I" on "'||v_attribute||'" (value) '||v_reverse||' tablespace dsorapart_uidx';
       end if;

     end if;

--       execute immediate 'insert into "'||v_attribute||'" select entryid, converter.'||v_equality||'(value) from sattribute where name= '''||lower(v_attribute)||''' order by 2';

     insert into indexed_attribute (attribute,type,index_type,equality) values (v_attribute,v_sql_type,p_index_type,v_equality);
     commit;

  end create_index;

  procedure drop_index(p_attribute varchar2)
  as
    v_index_type  number;
  begin

    select index_type
      into v_index_type
      from indexed_attribute
     where attribute= upper(p_attribute);

    if v_index_type = 1 then
      execute immediate 'drop cluster "'||upper(p_attribute)||'$C" including tables';
    else
      execute immediate 'drop table "'||upper(p_attribute)||'"';
    end if;

    delete indexed_attribute where attribute= upper(p_attribute);

    commit;
  end;

  procedure insert_value(p_entryid number,
                         p_name    varchar2,
                         p_value   varchar2)
  as
   v_name varchar2(255):= upper(p_name);
  begin
     for c_cur in indexed_attribute(v_name) loop

        execute immediate 'insert into "'||v_name||'" values (:entryid,converter.'||c_cur.equality||'(:value))'
        using p_entryid,p_value;

     end loop;
  end;

  procedure update_value(p_entryid   number,
                         p_name      varchar2,
                         p_old_value varchar2,
                         p_new_value varchar2)
  as
   v_name varchar2(255):= upper(p_name);
  begin
     for c_cur in indexed_attribute(v_name) loop

        execute immediate 'update "'||v_name||'" set value= converter.'||c_cur.equality||'(:newvalue) where value= converter.'||c_cur.equality||'(:oldvalue) and entryid= :entryid'
        using p_new_value,p_old_value,p_entryid;

     end loop;
  end;

  procedure delete_value(p_entryid number,
                         p_name    varchar2,
                         p_value   varchar2)
  as
   v_name varchar2(255):= upper(p_name);
  begin
     for c_cur in indexed_attribute(v_name) loop

        execute immediate 'delete "'||v_name||'" where value= converter.'||c_cur.equality||'(:oldvalue) and entryid= :entryid'
        using p_value,p_entryid;

     end loop;
  end;


end indexer;
/

show errors;

create or replace package body filter
as

  type refCur is ref cursor;
  type node_arr is table of dbms_xmldom.DOMNode index by binary_integer;

  v_bind_variable          varchar2(4000);
  v_query_type             number:= 0;
  v_presence_node          boolean:= false;

  procedure log(p_message varchar2)
  as pragma autonomous_transaction;
  begin
    insert into t_message (message) values (p_message);
    commit;
  end;

  function toliteral(p_value varchar2, p_equality varchar2)
  return varchar2
  as
  begin
      return 'converter.'||p_equality||'('''||p_value||''')';
  end;

  function tobindv(p_equality varchar2)
  return varchar2
  as
  begin
      return 'converter.'||p_equality||'(:bv)';
  end;

  function get_childs(n dbms_xmldom.DOMNode)
  return node_arr
  as
     nl        dbms_xmldom.DOMNodeList;
     ch        dbms_xmldom.DOMNode:= dbms_xslprocessor.selectsinglenode(n,'./children');
     len       pls_integer;
     v_childs  node_arr;
  begin

      if not dbms_xmldom.isnull(ch) then

        nl:= dbms_xmldom.getchildnodes(ch);
        len:= dbms_xmldom.getlength(nl)-1;

        for i in 0..len loop
           v_childs(i+1):= dbms_xmldom.item(nl, i);
        end loop;

      end if;

      return v_childs;
  end;

  procedure add_att(p_atts       in out nocopy util.vc_arr,
                    p_attribute  varchar2)
  as
  begin

      for i in 1..p_atts.count loop
        if p_atts(i)= p_attribute then
          return;
        end if;
      end loop;

      p_atts(p_atts.count+1):= p_attribute;

  end;


  procedure parse_node(p_query      in out nocopy varchar2,
                       p_atts       in out nocopy util.vc_arr,
                       n            dbms_xmldom.DOMNode)
  as
   v_node_name      varchar2(255):= dbms_xmldom.getnodename(n);
   v_attribute      varchar2(255);
   v_attribute_eq   varchar2(255);
   v_value          varchar2(2000);
   v_childs         node_arr:= get_childs(n);
  begin

      --dbms_output.put_line(p_query);

      if v_childs.count= 0 then -- leaf

             v_attribute:= upper(dbms_xmldom.getnodevalue(dbms_xslprocessor.selectsinglenode(n,'./attribute/text()')));

             if not v_node_name= 'PresenceNode' then

               v_value:= dbms_xmldom.getnodevalue(dbms_xslprocessor.selectsinglenode(n,'./value/text()'));
               add_att(p_atts,upper(v_attribute));

                begin
                   select equality
                     into v_attribute_eq
                     from indexed_attribute
                    where attribute= v_attribute;
                exception
                 when no_data_found then
                   raise_application_error(-20001,'attribute '''||lower(v_attribute)||''' is not indexed');
                end;

             end if;

             if v_node_name= 'ApproximateNode' then
    		    	 --SOUNDEX
    		    	 raise_application_error(-20001,'Approximate search not implemented');
             elsif v_node_name= 'ExtensibleNode' then
    		    	 --MATCHING RULES
    		    	 raise_application_error(-20001,'Extensible search not implemented');
             elsif v_node_name= 'GreaterEqNode' then
                p_query:= p_query||'"'||v_attribute||'".VALUE >= '||toliteral(v_value,v_attribute_eq);
             elsif v_node_name= 'LessEqNode' then
                p_query:= p_query||'"'||v_attribute||'".VALUE <= '||toliteral(v_value,v_attribute_eq);
             elsif v_node_name= 'EqualityNode' then
                p_query:= p_query||'"'||v_attribute||'".VALUE = '||toliteral(v_value,v_attribute_eq);
             elsif v_node_name= 'PresenceNode' then
                p_query:= p_query||'EXISTS (SELECT 1 FROM EATTRIBUTES WHERE NAME= '''||lower(v_attribute)||''' AND ENTRYID= DN.ENTRYID AND ROWNUM < 2)';
                v_presence_node:= true;
             elsif v_node_name= 'SubstringNode' then
                p_query:= p_query||'"'||v_attribute||'".VALUE LIKE replace('||toliteral(v_value,v_attribute_eq)||',''*'',''%'')';
    		     else
                raise_application_error(-20001,'Unknown leaf node name: '''||v_node_name||'''');
             end if;

      else -- non leaf

        if v_node_name = 'OrNode' then

           p_query:= p_query||'( ';

           parse_node(p_query,p_atts,v_childs(1));

           for i in 2..v_childs.count loop
             p_query:= p_query||' OR ';
             parse_node(p_query,p_atts,v_childs(i));
           end loop;

           p_query:= p_query||' )';

        elsif v_node_name = 'AndNode' then


           p_query:= p_query||'( ';

           parse_node(p_query,p_atts,v_childs(1));

           for i in 2..v_childs.count loop
             p_query:= p_query||' AND ';
             parse_node(p_query,p_atts,v_childs(i));
           end loop;

           p_query:= p_query||' )';

        elsif v_node_name = 'NotNode' then

           p_query:= p_query||'NOT ( ';

           parse_node(p_query,p_atts,v_childs(1));

           p_query:= p_query||' )';

        else
         raise_application_error(-20001,'Unknown node name: '''||v_node_name||'''');
        end if;

      end if;

  end;

  procedure build_query(p_query        in out nocopy varchar2,
                        p_atts         util.vc_arr,
                        p_basedn       varchar2,
                        p_scope        number,
                        p_query_type   number)
  as
  begin

    if p_query_type = 2 then -- materialize

      p_query:= ') ),
        fl as (SELECT /*+materialize*/ DISTINCT ENTRYID
                 FROM "'||p_atts(1)||'"
                WHERE ('||p_query||'))
   select dns.*
     from dns,
          fl
    where dns.entryid=fl.entryid
 )';

      if p_scope = 0 then -- base

        p_query:= 'DN.RDN= :rdn AND DN.PARENTDN= :parentdn '||p_query;

      elsif p_scope = 1 then -- one

        p_query:= 'DN.PARENTDN= :parentdn '||p_query;

      else -- sub

        p_query:= '( (DN.RDN= :rdn AND DN.PARENTDN= :parentdn) OR DN.PARENTDN LIKE :parentdn||''%'') '||p_query;

      end if;


      p_query:= 'select * from (

   with dns as (SELECT /*+materialize*/ ENTRYID, PARENTDN||RDN DN, UPDN FROM DN  WHERE ('||p_query;

    else -- default query

          for i in 1..p_atts.count loop

               p_query:= '"'||p_atts(i)||'".ENTRYID= DN.ENTRYID AND '||p_query;

          end loop;

          if p_scope = 0 then -- base

            p_query:= 'DN.RDN= :rdn AND DN.PARENTDN= :parentdn AND '||p_query;

          elsif p_scope = 1 then -- one

            p_query:= 'DN.PARENTDN= :parentdn AND '||p_query;

          else -- sub

            p_query:= '( (DN.RDN= :rdn AND DN.PARENTDN= :parentdn) OR DN.PARENTDN LIKE :parentdn||''%'') AND '||p_query;

          end if;

          p_query:= ' WHERE '||p_query;

          for i in 1..p_atts.count loop
           p_query:= ', "'||p_atts(i)||'" '||p_query;
          end loop;

          p_query:= 'SELECT DISTINCT DN.ENTRYID, DN.PARENTDN||DN.RDN DN, DN.UPDN FROM DN'||p_query;

    end if;

  end;


  function parse(p_filter       varchar2,
                 p_basedn       varchar2,
                 p_scope        number,
                 p_countlimit   number default null,
                 p_use_mat      number default 1)
  return varchar2
  as
    parser             dbms_xmlparser.parser;
    doc                dbms_xmldom.DOMDocument;
    nl                 dbms_xmldom.DOMNodeList;
    n                  dbms_xmldom.DOMNode;
    v_query            varchar2(32767);
    v_atts             util.vc_arr;
    v_childs           node_arr;
    v_node_name        varchar2(255);
    v_attribute        varchar2(255);
    v_attribute_eq     varchar2(255);
    v_index_type       number:= 0;
    v_matid            number;
  begin

    --dbms_profiler.start_profiler('parse');

    begin
      select materialization_id
        into v_matid
        from filter_materialization
       where filter= p_filter
         and scope= p_scope
         and basedn= p_basedn
         and rownum < 1+p_use_mat;
         
      v_query:= 'SELECT * FROM "M$'||v_matid||'" ';
      
    exception
     when no_data_found then 
    
          v_query_type:= 0;
          v_presence_node:= false;
          parser := dbms_xmlparser.newparser;
          dbms_xmlparser.parsebuffer(parser, p_filter);
          doc := dbms_xmlparser.getdocument(parser);
          n:= dbms_xmldom.makenode(doc);
          n:= dbms_xslprocessor.selectsinglenode(n,'/node()');
          v_childs:= get_childs(n);
      
          if v_childs.count > 0 then
            parse_node(v_query,v_atts,n);
          else  -- build a query with a bind variable (a lot of queries should be like this)
      
             v_node_name:= dbms_xmldom.getnodename(n);
             v_attribute:= upper(dbms_xmldom.getnodevalue(dbms_xslprocessor.selectsinglenode(n,'./attribute/text()')));
      
             if not v_node_name= 'PresenceNode' then
                begin
                   select equality,
                          index_type
                     into v_attribute_eq,
                          v_index_type
                     from indexed_attribute
                    where attribute= v_attribute;
                exception
                 when no_data_found then
                   raise_application_error(-20001,'attribute '''||lower(v_attribute)||''' is not indexed');
                end;
             end if;
      
             if v_node_name= 'ApproximateNode' then
               --SOUNDEX
               raise_application_error(-20001,'Approximate search not implemented');
             elsif v_node_name= 'ExtensibleNode' then
               --MATCHING RULES
               raise_application_error(-20001,'Extensible search not implemented');
             elsif v_node_name= 'GreaterEqNode' then
                v_query:= v_query||'"'||v_attribute||'".VALUE >= '||tobindv(v_attribute_eq);
             elsif v_node_name= 'LessEqNode' then
                v_query:= v_query||'"'||v_attribute||'".VALUE <= '||tobindv(v_attribute_eq);
             elsif v_node_name= 'EqualityNode' then
                v_query:= v_query||'"'||v_attribute||'".VALUE = '||tobindv(v_attribute_eq);
      
                if v_index_type = indexer.index_type_unique then
                  v_query_type:= 1;
                end if;
      
             elsif v_node_name= 'PresenceNode' then
                v_query:= v_query||'EXISTS (SELECT 1 FROM EATTRIBUTES WHERE NAME= :bv AND ENTRYID= DN.ENTRYID AND ROWNUM < 2)';
                v_presence_node:= false;
             elsif v_node_name= 'SubstringNode' then
                v_query:= v_query||'"'||v_attribute||'".VALUE LIKE replace('||tobindv(v_attribute_eq)||',''*'',''%'')';
             else
                raise_application_error(-20001,'Unknown leaf node name: '''||v_node_name||'''');
             end if;
      
             if v_node_name= 'PresenceNode' then
               v_bind_variable:= lower(v_attribute);
             else
               v_bind_variable:= dbms_xmldom.getnodevalue(dbms_xslprocessor.selectsinglenode(n,'./value/text()'));
               add_att(v_atts,upper(v_attribute));
             end if;
      
          end if;
      
          if v_atts.count = 1 and v_query_type = 0 and not v_presence_node then
      
            v_query_type:= 2; -- materialize views filter and dns an then join
      
          end if;
      
          build_query(v_query,v_atts,p_basedn,p_scope,v_query_type);
      
          dbms_xmlparser.freeparser(parser);
          dbms_xmldom.freedocument(doc);

    end;
    
    if p_countlimit > 0 then
       v_query:= 'SELECT * FROM ('||v_query||') WHERE ROWNUM < '||to_char(p_countlimit+1);
    end if;

    --dbms_profiler.stop_profiler;

    return v_query;

  exception
   when others then
    dbms_xmlparser.freeparser(parser);
    dbms_xmldom.freedocument(doc);
    --dbms_profiler.stop_profiler;
    raise;
  end;

  function toids(p_filter       varchar2,
                 p_basedn       varchar2,
                 p_scope        number,
                 p_countlimit   number default null,
                 p_use_mat      number default 1)
  return refdn_tb
  pipelined
  as
   v_parentdn   dn.parentdn%type:= p_basedn||',';
   v_refdn      refdn;
  begin

     --dbms_profiler.start_profiler('toids');


     if p_filter is null then -- no filter (objectclass=*)

         if p_scope= 0 then  -- base

            declare
              v_rdn        dn.rdn%type;
              v_lastcomma  pls_integer:= instr(p_basedn,',',-1);
            begin

              v_parentdn:= nvl(substr(p_basedn,1,v_lastcomma),',');
              v_rdn:= substr(p_basedn,v_lastcomma+1);

              for c_cur in (select entryid, parentdn||rdn dn, updn
                              from dn
                             where rdn= v_rdn
                               and parentdn= v_parentdn)
              loop
                v_refdn.entryid:= c_cur.entryid;
                v_refdn.dn:= c_cur.dn;
                v_refdn.updn:= c_cur.updn;
                pipe row (v_refdn);
              end loop;

            end;

         elsif p_scope= 1 then  -- one

              for c_cur in (select entryid, parentdn||rdn dn, updn
                              from dn
                             where parentdn= v_parentdn)
              loop
                v_refdn.entryid:= c_cur.entryid;
                v_refdn.dn:= c_cur.dn;
                v_refdn.updn:= c_cur.updn;
                pipe row (v_refdn);
              end loop;

         else --sub

            declare
              v_rdn        dn.rdn%type;
              v_lastcomma  pls_integer:= instr(p_basedn,',',-1);
            begin

              v_parentdn:= nvl(substr(p_basedn,1,v_lastcomma),',');
              v_rdn:= substr(p_basedn,v_lastcomma+1);

              for c_cur in (select entryid, parentdn||rdn dn, updn
                              from dn
                             where rdn= v_rdn
                               and parentdn= v_parentdn)
              loop
                v_refdn.entryid:= c_cur.entryid;
                v_refdn.dn:= c_cur.dn;
                v_refdn.updn:= c_cur.updn;
                pipe row (v_refdn);
              end loop;

              v_parentdn:= p_basedn||',';

              for c_cur in (select entryid, parentdn||rdn dn, updn
                              from dn
                             where parentdn like v_parentdn||'%')
              loop
                v_refdn.entryid:= c_cur.entryid;
                v_refdn.dn:= c_cur.dn;
                v_refdn.updn:= c_cur.updn;
                pipe row (v_refdn);
              end loop;

            end;

         end if;

     else

        declare
          c_cur refCur;
          procedure open_cur
          as
             v_parentdn   dn.parentdn%type:= p_basedn||',';
             v_rdn        dn.rdn%type;
             v_lastcomma  pls_integer:= instr(p_basedn,',',-1);
             procedure open_base_cur
             as
             begin -- complex filter no bv
                open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                using v_rdn,
                      v_parentdn;
             exception
               when others then -- simple node
                  begin
                      open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                      using v_rdn,
                            v_parentdn,
                            v_bind_variable;
                  exception
                     when others then  -- presence node
                            open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                            using v_rdn,
                                  v_parentdn,
                                  v_bind_variable,
                                  v_bind_variable;
                  end;
             end open_base_cur;
             procedure open_one_cur
             as
             begin -- complex filter no bv
                    open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                    using v_parentdn;
             exception
               when others then -- simple node
                  begin
                    open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                    using v_parentdn,
                          v_bind_variable;
                  exception
                     when others then  -- presence node
                            open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                            using v_parentdn,
                                  v_bind_variable,
                                  v_bind_variable;
                  end;
             end open_one_cur;
             procedure open_sub_cur
             as
             begin -- complex filter no bv
                  open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                  using v_rdn,
                        v_parentdn,
                        p_basedn||',';
             exception
               when others then -- simple node
                  begin
                      open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                      using v_rdn,
                            v_parentdn,
                            p_basedn||',',
                            v_bind_variable;
                  exception
                     when others then  -- presence node
                            open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat)
                            using v_rdn,
                                  v_parentdn,
                                  p_basedn||',',
                                  v_bind_variable,
                                  v_bind_variable;
                  end;
             end open_sub_cur;
          begin

                  if p_scope= 0 then  -- base


                      v_parentdn:= nvl(substr(p_basedn,1,v_lastcomma),',');
                      v_rdn:= substr(p_basedn,v_lastcomma+1);

                      open_base_cur;

                  elsif p_scope= 1 then  -- one
                      open_one_cur;
                  else  -- sub

                      v_parentdn:= nvl(substr(p_basedn,1,v_lastcomma),',');
                      v_rdn:= substr(p_basedn,v_lastcomma+1);

                      open_sub_cur;

                  end if;
          
          exception
           when others then -- materialized
                 open c_cur for parse(p_filter,p_basedn,p_scope,p_countlimit,p_use_mat);
          end open_cur;
        begin

          open_cur;
          loop

            fetch c_cur into v_refdn;
            exit when c_cur%notfound;

            pipe row (v_refdn);

          end loop;
          close c_cur;

        end;


     end if;

     --dbms_profiler.stop_profiler;

     return;
  end toids;

  procedure materialize_ids(p_filter       varchar2,
                            p_basedn       varchar2,
                            p_scope        number)
  as
    v_matid            number;
    parser             dbms_xmlparser.parser;
    doc                dbms_xmldom.DOMDocument;
    nl                 dbms_xmldom.DOMNodeList;
    n                  dbms_xmldom.DOMNode;
    v_query            varchar2(32767);
    v_atts             util.vc_arr;
  begin

     begin
     select materialization_id
       into v_matid
       from filter_materialization
      where basedn= p_basedn
        and scope= p_scope
        and filter= p_filter;
      raise_application_error(-20001,'filter materialization already exists');
     exception
      when no_data_found then
       null;
     end;

      parser := dbms_xmlparser.newparser;
      dbms_xmlparser.parsebuffer(parser, p_filter);
      doc := dbms_xmlparser.getdocument(parser);
      n:= dbms_xmldom.makenode(doc);
      n:= dbms_xslprocessor.selectsinglenode(n,'/node()');
      
  
      parse_node(v_query,v_atts,n);
      
      dbms_xmlparser.freeparser(parser);
      dbms_xmldom.freedocument(doc);
      
     for i in 1..v_atts.count loop
       begin
         select 1
           into v_matid
           from indexed_attribute
          where attribute= upper(v_atts(i));
       exception
         when no_data_found then
            raise_application_error(-20001,'attribute '''||lower(v_atts(i))||''' must be indexed to materialize this filter');
       end;
     end loop;
  
     select seq_materialization.nextval
       into v_matid
       from dual;
  
     execute immediate 'create table "M$'||v_matid||'" tablespace testds_utab as select * from table(filter.toids('''||p_filter||''','''||p_basedn||''','||p_scope||'))';
     execute immediate 'alter table "M$'||v_matid||'" add constraint "PK$'||v_matid||'" primary key (entryid) using index tablespace testds_uidx';

     for i in 1..v_atts.count loop
        execute immediate 'create or replace trigger "TM$'||v_matid||'$'||i||'"
after insert or delete or update on "'||v_atts(i)||'"
for each row 
begin
  
  if inserting then
    filter.insert_value('||v_matid||',:new.entryid,:new.value);
  elsif updating then
    filter.update_value('||v_matid||',:old.entryid,:old.value,:new.value);
  else
    filter.delete_value('||v_matid||',:old.entryid,:old.value);
  end if;
  
end;';
      execute immediate 'alter trigger "TM$'||v_matid||'$'||i||'" enable';
     end loop;
  
     insert into filter_materialization (basedn,scope,filter,materialization_id)
     values (p_basedn,p_scope,p_filter,v_matid);

  exception
   when others then
    begin
    execute immediate 'drop table "M$'||v_matid||'" cascade constraints purge';
    exception
    when others then
     null;
    end;
    raise;
  end;

  procedure remove_materialized_ids(p_filter       varchar2,
                                    p_basedn       varchar2,
                                    p_scope        number)
  as
    v_matid  number;
  begin
  
     select materialization_id
       into v_matid
       from filter_materialization
      where basedn= p_basedn
        and scope= p_scope
        and filter= p_filter;
       
     for c_cur in (select trigger_name from user_triggers where trigger_name like 'TM$'||to_char(v_matid)||'$%') loop
        execute immediate 'drop trigger "'||c_cur.trigger_name||'"';
     end loop;

     execute immediate 'drop table "M$'||v_matid||'" cascade constraints';
     
     delete filter_materialization
      where materialization_id= v_matid;

     commit;
  
  exception
   when no_data_found then
     raise_application_error(-20001,'materialization not found');
  end;
  
  
  procedure refresh_materialization(p_materialization_id number,
                                    p_entryid number)
  as
    v_in_materialization boolean:= false;
    v_in_filter          boolean:= false;
    v_dummy              number;
  begin
      
      begin
        execute immediate 'select 1 from "M$'||p_materialization_id||'" where entryid= :entryid'
        into v_dummy
        using p_entryid;
        v_in_materialization:= true;
      exception
       when no_data_found then
         null;
      end;
      
      declare
        v_filter filter_materialization%rowtype;
      begin
      
        select *
          into v_filter
          from filter_materialization
         where materialization_id= p_materialization_id;
         
        select 1
          into v_dummy
          from table(filter.toids(v_filter.filter,v_filter.basedn,v_filter.scope,null,0))
         where entryid= p_entryid;
         
        v_in_filter:= true;
      exception
       when no_data_found then
         null;
      end;
      
      if v_in_materialization and not v_in_filter then
        execute immediate 'delete "M$'||p_materialization_id||'" where entryid= :entryid'
        using p_entryid;
      elsif v_in_filter and not v_in_materialization then
        execute immediate 'insert into "M$'||p_materialization_id||'" select entryid, parentdn||rdn dn from dn where entryid= :entryid'
        using p_entryid;
      end if;
      
  end;

  procedure insert_value(p_materialization_id number,
                         p_entryid number,
                         p_value   varchar2)
  as
  begin
      
      -- there is of course a finest way
      refresh_materialization(p_materialization_id,p_entryid);
      
  end;

  procedure update_value(p_materialization_id number,
                         p_entryid   number,
                         p_old_value varchar2,
                         p_new_value varchar2)
  as
  begin

      -- there is of course a finest way
      refresh_materialization(p_materialization_id,p_entryid);

  end;

  procedure delete_value(p_materialization_id number,
                         p_entryid number,
                         p_value   varchar2)
  as
  begin

      -- there is of course a finest way
      refresh_materialization(p_materialization_id,p_entryid);

  end;

end filter;
/

show errors;

create or replace trigger trg_indexer
after insert or delete or update on eattributes
for each row 
begin
  
  if inserting then
    indexer.insert_value(:new.entryid,:new.name,:new.ssvalue);
  elsif updating then
    indexer.update_value(:old.entryid,:old.name,:old.ssvalue,:new.ssvalue);
  else
    indexer.delete_value(:old.entryid,:old.name,:old.ssvalue);
  end if;
  
end;
/

show errors;

create or replace package partition_facade
as

  type clob_value is record (
     cvalueid      number,
     value         clob  
  );

  type clob_value_tb is table of clob_value;

  type blob_value is record (
     bvalueid      number,
     value         blob  
  );

  type blob_value_tb is table of blob_value;

  type clob_tb is table of clob;

  type blob_tb is table of blob;

  function lookup_dn(p_dn varchar2)
  return ldap_entry;

  function list(p_dn varchar2)
  return ldap_entry_table
  pipelined;

  function search(p_base varchar2, p_scope number, p_filter varchar2, p_returning_attributes vc_arr, p_limit number)
  return ldap_entry_table
  pipelined;

  procedure add(p_entry ldap_entry);

  procedure modify(p_entry ldap_entry);

  procedure delete(p_dn varchar2);
 
  procedure move(p_parent varchar2, p_new_updn varchar2, p_dn varchar2);

  procedure move_and_rename(p_parent varchar2, p_rdn varchar2, p_new_updn varchar2, p_dn varchar2);

  procedure rename(p_rdn varchar2, p_new_updn varchar2, p_dn varchar2);

  function read_clob(p_cvalueid number)
  return clob_tb
  pipelined;

  function read_blob(p_bvalueid number)
  return blob_tb
  pipelined;

  procedure write_clob(p_hash varchar2, p_cvalueid out number, p_clob out clob);

  procedure write_blob(p_hash varchar2, p_bvalueid out number, p_blob out blob);

end;
/

show errors;

create or replace package body partition_facade
as

  v_empty_entry     constant   ldap_entry:= ldap_entry('','',ldap_attribute_table());

  procedure log(p_message varchar2)
  as pragma autonomous_transaction;
  begin
    insert into t_message (message) values (p_message);
    commit;
  end;

  function lookup_dn(p_dn varchar2)
  return ldap_entry
  as
   v_entry           ldap_entry:= v_empty_entry;
   v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin

     for c_cur in (select * from dn where parentdn = v_splitted_dn.parentdn and rdn= v_splitted_dn.rdn) loop

        v_entry.dn:= c_cur.parentdn||c_cur.rdn;
        v_entry.updn:= c_cur.updn;

        for c_att in (select * from eattributes where entryid= c_cur.entryid) loop

          v_entry.attrs.extend(1);
          v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
        
        end loop;

        return v_entry;

     end loop;
    
   return null;
  end;

  function list(p_dn varchar2)
  return ldap_entry_table
  pipelined
  as
   v_entry           ldap_entry:= v_empty_entry;
   v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin

     for c_cur in (select * from dn where parentdn = p_dn||',') loop

        v_entry.dn:= c_cur.parentdn||c_cur.rdn;
        v_entry.updn:= c_cur.updn;

        for c_att in (select * from eattributes where entryid= c_cur.entryid) loop

          v_entry.attrs.extend(1);
          v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
        
        end loop;

        pipe row (v_entry);
        v_entry:= v_empty_entry;

     end loop;

    return;
  end list;

  function search(p_base varchar2, p_scope number, p_filter varchar2, p_returning_attributes vc_arr, p_limit number)
  return ldap_entry_table
  pipelined
  as
    v_star         boolean:= false; -- all user attrs
    v_plus         boolean:= false; -- all operational attrs
    v_others       vc_arr:= p_returning_attributes; -- specific attributes
    v_find_attr    vc_arr:= vc_arr();
    v_last_entryid number;
    v_last_aname   varchar2(255);
    v_entry        ldap_entry:= v_empty_entry;
  begin

/*
     log('base: '||p_base);
     log('scope: '||p_scope);
     log('filter: '||p_filter);
     log('limit: '||p_limit);
     log('p_returning_attributes.count: '||p_returning_attributes.count);

     for i in 1..p_returning_attributes.count loop
        log('p_returning_attributes('||i||'): '||p_returning_attributes(i));
     end loop;
      --dbms_profiler.start_profiler('toatts');
*/
      for i in 1..v_others.count loop
        if v_others(i) = '+' then
          v_plus:= true;
        elsif v_others(i) = '*' then
          v_star:= true;
        elsif v_others(i) = 'ref' then
          null; -- remove ref
        else
          v_find_attr.extend(1);
          v_find_attr(v_find_attr.count):= v_others(i);
        end if;
      end loop;

      -- hot
      for c_cur in (select entryid, dn, updn from table(filter.toids(p_filter,p_base,p_scope,p_limit))) loop

        v_entry.dn:= c_cur.dn;
        v_entry.updn:= c_cur.updn;

        if v_star and v_plus then

          for c_att in (select *
                          from eattributes a
                         where entryid= c_cur.entryid
                       order by 1)
          loop
            v_entry.attrs.extend(1);
            v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
          end loop;

        else

          if v_star then

                  v_others.delete(1);

                  for c_att in (select *
                                  from eattributes a
                                 where type= 'u'
                                   and entryid= c_cur.entryid
                              order by 1)
                  loop
                    v_entry.attrs.extend(1);
                    v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
                  end loop;

          end if;

          if v_plus then

                  v_others.delete(1);

                  for c_att in (select *
                                  from eattributes a
                                 where type is null
                                   and entryid= c_cur.entryid
                              order by 1)
                  loop
                    v_entry.attrs.extend(1);
                    v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
                  end loop;

          end if;

          if v_find_attr.count > 0 then

            if v_find_attr.count = 1 then

                  for c_att in (select *
                                  from eattributes a
                                 where name= v_find_attr(1)
                                   and entryid= c_cur.entryid
                              order by 1)
                  loop
                    v_entry.attrs.extend(1);
                    v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
                  end loop;

            else

                 --hot
                  for c_att in (with names
                                 as
                                 (select column_value name from table(v_find_attr))
                                 select a.*
                                   from eattributes a,
                                        names b
                                  where a.name= b.name
                                    and a.entryid= c_cur.entryid)
                  loop
                    v_entry.attrs.extend(1);
                    v_entry.attrs(v_entry.attrs.last):= ldap_attribute(c_att.name,c_att.type,c_att.ssvalue,c_att.sbvalue,c_att.lsvalue,c_att.lbvalue);
                  end loop;

            end if;

          end if;

        end if;

        pipe row (v_entry);
        v_entry:= v_empty_entry;

      end loop;

      --dbms_profiler.stop_profiler;

    return;   
  end;


  procedure add(p_entry ldap_entry)
  as
   v_entryid       number;
   v_splitted_dn   util.rsplitteddn;
  begin

/*
     log('dn: '||p_entry.dn);
     log('updn: '||p_entry.updn);
     log('attrs.count: '||p_entry.attrs.count);

     for i in 1..p_entry.attrs.count loop
        log('attrs('||p_entry.attrs(i).name||'): '||p_entry.attrs(i).ssvalue);
     end loop;
*/
     v_splitted_dn:= util.splitdn(p_entry.dn);

     insert into dn (parentdn,rdn,entryid,updn)
          values (v_splitted_dn.parentdn,
                  v_splitted_dn.rdn,
                  seq_entryid.nextval,
                  p_entry.updn)
       returning entryid into v_entryid;


     for i in 1..p_entry.attrs.last loop
       insert into eattributes (entryid,name,type,ssvalue,sbvalue,lsvalue,lbvalue)
       values (v_entryid,p_entry.attrs(i).name,
                         p_entry.attrs(i).type,
                         p_entry.attrs(i).ssvalue,
                         p_entry.attrs(i).sbvalue,
                         p_entry.attrs(i).lsvalue,
                         p_entry.attrs(i).lbvalue);
     end loop;

     commit;

--     return v_entryid;

  end;

  procedure modify(p_entry ldap_entry)
  as
    v_splitted_dn     util.rsplitteddn:= util.splitdn(p_entry.dn);
  begin
      for c_cur in (select entryid from dn where parentdn = v_splitted_dn.parentdn||',' and rdn = v_splitted_dn.rdn) loop

         delete eattributes where entryid= c_cur.entryid;

         for i in 1..p_entry.attrs.last loop
           insert into eattributes (entryid,name,type,ssvalue,sbvalue,lsvalue,lbvalue)
           values (c_cur.entryid,p_entry.attrs(i).name,
                                 p_entry.attrs(i).type,
                                 p_entry.attrs(i).ssvalue,
                                 p_entry.attrs(i).sbvalue,
                                 p_entry.attrs(i).lsvalue,
                                 p_entry.attrs(i).lbvalue);
         end loop;

      end loop;
     commit;

  end;

  procedure delete(p_dn varchar2)
  as
    v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin
     delete dn where parentdn = v_splitted_dn.parentdn and rdn = v_splitted_dn.rdn;
     commit;
  end;

  procedure move(p_parent varchar2, p_new_updn varchar2, p_dn varchar2)
  as
    v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin
    update dn
       set parentdn = p_parent||',',
           updn = p_new_updn
     where parentdn = v_splitted_dn.parentdn
       and rdn = v_splitted_dn.rdn;
    commit;
  end;

  procedure move_and_rename(p_parent varchar2, p_rdn varchar2, p_new_updn varchar2, p_dn varchar2)
  as
    v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin
    update dn
       set parentdn = p_parent||',',
           rdn = p_rdn,
           updn = p_new_updn
     where parentdn = v_splitted_dn.parentdn
       and rdn = v_splitted_dn.rdn;
    commit;
  end;

  procedure rename(p_rdn varchar2, p_new_updn varchar2, p_dn varchar2)
  as
    v_splitted_dn     util.rsplitteddn:= util.splitdn(p_dn);
  begin
    update dn
       set rdn = p_rdn,
           updn = p_new_updn
     where parentdn = v_splitted_dn.parentdn
       and rdn = v_splitted_dn.rdn;
    commit;
  end;

  function read_clob(p_cvalueid number)
  return clob_tb
  pipelined
  as
   v_clob clob;
  begin
    
    select value
      into v_clob
      from cvalue
     where cvalueid= p_cvalueid;

    pipe row (v_clob);
    return;
  exception
   when no_data_found then
    return;
  end;

  function read_blob(p_bvalueid number)
  return blob_tb
  pipelined
  as
   v_blob blob;
  begin
    
    select value
      into v_blob
      from bvalue
     where bvalueid= p_bvalueid;

    pipe row (v_blob);
    return;
  exception
   when no_data_found then
    return;
  end;

  procedure write_clob(p_hash varchar2, p_cvalueid out number, p_clob out clob)
  as
  begin

    select cvalueid,
           value
      into p_cvalueid,
           p_clob
      from cvalue
     where hash= p_hash;

  exception
   when no_data_found then
    insert into cvalue (cvalueid,value,hash)
         values (seq_cvalueid.nextval,empty_clob,p_hash)
      returning cvalueid, value into p_cvalueid,
                                     p_clob;
  end;

  procedure write_blob(p_hash varchar2, p_bvalueid out number, p_blob out blob)
  as
  begin
    select bvalueid,
           value
      into p_bvalueid,
           p_blob
      from bvalue
     where hash= p_hash;

  exception
   when no_data_found then
    insert into bvalue (bvalueid,value,hash)
         values (seq_bvalueid.nextval,empty_blob,p_hash)
      returning bvalueid, value into p_bvalueid,
                                     p_blob;
  end;
  
end partition_facade;
/

show errors;


/* index objectclass */
begin
 indexer.create_index('2.5.4.0','objectIdentifierMatch',indexer.index_type_clustered);
end;
/


exit;
