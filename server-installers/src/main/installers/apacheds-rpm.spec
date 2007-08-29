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

Summary: ${app.display.name} Server
Name: ${app}
Version: ${app.version}
Release: ${app.release}
License: ${app.license.type}
Group: System Environment/Daemons
URL: ${app.url}
Source0: %{name}-%{version}.tar.gz
BuildRoot: %{_tmppath}/%{name}-%{version}-%{release}-root

%description
${app.description}

%define adsdata %{_localstatedir}/lib/%{name}
%define adshome /opt/%{name}-%{version}

%prep
echo $RPM_BUILD_ROOT
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT
cp -rf ${image.basedir} $RPM_BUILD_ROOT/%{name}-%{version}
cd $RPM_BUILD_ROOT
tar -zcvf %{_topdir}/SOURCES/%{name}-%{version}.tar.gz %{name}-%{version}

%setup -q

%build
cd $RPM_BUILD_ROOT/%{name}-%{version}

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT%{adshome}/bin
mkdir -p $RPM_BUILD_ROOT%{adshome}/conf
mkdir -p $RPM_BUILD_ROOT%{adshome}/lib/ext
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/conf
mkdir -p $RPM_BUILD_ROOT%{adsdata}/default/partitions
mkdir -p $RPM_BUILD_ROOT%{_localstatedir}/log/%{name}/default
mkdir -p $RPM_BUILD_ROOT%{_localstatedir}/run/%{name}/default
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/init.d
mkdir -p $RPM_BUILD_ROOT%{_sysconfdir}/sysconfig
${mk.docs.dirs}
${mk.sources.dirs}

pwd
install -m 755 ${image.basedir}/bin/%{name} $RPM_BUILD_ROOT%{adshome}/bin/%{name}
install -m 644 ${image.basedir}/lib/wrapper.jar $RPM_BUILD_ROOT%{adshome}/lib/wrapper.jar
install -m 644 ${image.basedir}/lib/bootstrapper.jar $RPM_BUILD_ROOT%{adshome}/lib/bootstrapper.jar
install -m 644 ${image.basedir}/lib/libwrapper.so $RPM_BUILD_ROOT%{adshome}/lib/libwrapper.so
#install -m 644 ${image.basedir}/lib/logger.jar $RPM_BUILD_ROOT%{adshome}/lib/logger.jar
install -m 644 ${image.basedir}/bin/apacheds-tools.jar $RPM_BUILD_ROOT%{adshome}/bin/apacheds-tools.jar
install -m 755 ${image.basedir}/bin/apacheds-tools.sh $RPM_BUILD_ROOT%{adshome}/bin/apacheds-tools.sh
install -m 600 ${image.basedir}/conf/server.xml $RPM_BUILD_ROOT%{adsdata}/default/conf/server.xml
install -m 644 ${image.basedir}/conf/apacheds.conf $RPM_BUILD_ROOT%{adshome}/conf/apacheds.conf
install -m 644 ${image.basedir}/conf/apacheds-default.conf $RPM_BUILD_ROOT%{adsdata}/default/conf/apacheds.conf
install -m 644 ${image.basedir}/conf/log4j.properties $RPM_BUILD_ROOT%{adsdata}/default/conf/log4j.properties
install -m 744 ${image.basedir}/bin/${server.init} $RPM_BUILD_ROOT/etc/init.d/%{name}
install -m 644 ${image.basedir}/conf/apacheds-sysconfig.conf $RPM_BUILD_ROOT%{_sysconfdir}/sysconfig/apacheds
install -m 644 ${image.basedir}/${app.license.name} $RPM_BUILD_ROOT%{adshome}
install -m 644 ${image.basedir}/${app.readme.name} $RPM_BUILD_ROOT%{adshome}
install -m 644 ${image.basedir}/${app.icon} $RPM_BUILD_ROOT%{adshome}
${install.append.libs}
${install.docs}
${install.sources}
${install.notice.file}

%clean
rm -rf $RPM_BUILD_ROOT

%pre
mkdir -p $RPM_BUILD_ROOT%{_localstatedir}/lib/%{name}
%{_sbindir}/groupadd apacheds >/dev/null 2>&1 || :
%{_sbindir}/useradd -g apacheds -d %{adsdata} apacheds >/dev/null 2>&1 || :

%post
/sbin/chkconfig --add %{name}

%files
%defattr(-,apacheds,apacheds,-)
#%doc ${app.license.name} ${app.readme.name}
%config %attr(0755, root, root) /etc/init.d/%{name}
%{adshome}
%{adshome}/bin
%{adshome}/lib
%{adshome}/lib/ext
%{adsdata}
%{adsdata}
%{adsdata}/default
%{adsdata}/default/conf
%{adsdata}/default/partitions
%{_localstatedir}/log/%{name}
%{_localstatedir}/log/%{name}/default
%{_localstatedir}/run/%{name}
%{_localstatedir}/run/%{name}/default
%{adshome}/bin/%{name}
%{adshome}/lib/wrapper.jar
%{adshome}/lib/bootstrapper.jar
%{adshome}/bin/apacheds-tools.jar
%{adshome}/bin/apacheds-tools.sh
#%{adshome}/lib/logger.jar
%config %{adsdata}/default/conf/log4j.properties
%config %{adsdata}/default/conf/apacheds.conf
%config %{adsdata}/default/conf/server.xml
%config %{_sysconfdir}/sysconfig/apacheds
%{adshome}/${app.readme.name}
%{adshome}/${app.license.name}
%{adshome}/${app.icon}
${verify.append.libs}
${verify.docs}
${verify.sources}
${verify.notice.file}
