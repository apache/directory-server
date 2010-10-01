#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#

Summary: ApacheDS
Name: apacheds
Version: ${version}
Release: 1
License: ASL 2.0
Group: System Environment/Daemons
URL: http://directory.apache.org
Source: apacheds-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

%description
ApacheDS ${version}

%define adsdata /var/lib/%{name}-%{version}
%define adshome /opt/%{name}-%{version}

%prep
%setup -q

%build

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{adshome}/bin
mkdir -p $RPM_BUILD_ROOT%{adshome}/conf
mkdir -p $RPM_BUILD_ROOT%{adshome}/lib/
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/conf
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/log
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/partitions
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/run
mkdir -p $RPM_BUILD_ROOT/etc/init.d

# Server files
install -m 755 ${build.dir}/%{name}-%{version}/server/LICENSE $RPM_BUILD_ROOT%{adshome}/LICENSE
install -m 755 ${build.dir}/%{name}-%{version}/server/NOTICE $RPM_BUILD_ROOT%{adshome}/NOTICE
install -m 755 ${build.dir}/%{name}-%{version}/server/bin/apacheds $RPM_BUILD_ROOT%{adshome}/bin/apacheds
install -m 755 ${build.dir}/%{name}-%{version}/server/bin/wrapper $RPM_BUILD_ROOT%{adshome}/bin/wrapper
install -m 644 ${build.dir}/%{name}-%{version}/server/conf/wrapper.conf $RPM_BUILD_ROOT%{adshome}/conf/wrapper.conf
${install.libs}

# Instance files
install -m 644 ${build.dir}/%{name}-%{version}/instances/default/conf/config.ldif $RPM_BUILD_ROOT%{adsdata}/default/conf/config.ldif
install -m 644 ${build.dir}/%{name}-%{version}/instances/default/conf/log4j.properties $RPM_BUILD_ROOT%{adsdata}/default/conf/log4j.properties
install -m 644 ${build.dir}/%{name}-%{version}/instances/default/conf/wrapper.conf $RPM_BUILD_ROOT%{adsdata}/default/conf/wrapper.conf

# Init script
install -m 755 ${build.dir}/%{name}-%{version}/etc-initd-script $RPM_BUILD_ROOT/etc/init.d/apacheds-%{version}-default

%clean
rm -rf $RPM_BUILD_ROOT

%pre
groupadd --system apacheds >/dev/null 2>&1 || :
useradd --system -g apacheds -d %{adsdata} apacheds >/dev/null 2>&1 || :

%post
#/sbin/chkconfig --add %{name}

%files
%defattr(-,apacheds,apacheds,-)
%config %attr(0755, root, root) /etc/init.d/apacheds-%{version}-default
%{adshome}
%{adshome}/LICENSE
%{adshome}/NOTICE
%{adshome}/bin
%{adshome}/bin/%{name}
%{adshome}/bin/wrapper
%{adshome}/conf
%config %{adshome}/conf/wrapper.conf
%{adshome}/lib
${files.libs}
%{adsdata}
%{adsdata}/default
%{adsdata}/default/conf
%{adsdata}/default/log
%{adsdata}/default/partitions
%{adsdata}/default/run
%config %{adsdata}/default/conf/config.ldif
%config %{adsdata}/default/conf/log4j.properties
%config %{adsdata}/default/conf/wrapper.conf
