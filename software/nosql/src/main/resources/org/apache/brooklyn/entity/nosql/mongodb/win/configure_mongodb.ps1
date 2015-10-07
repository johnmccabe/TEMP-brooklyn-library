[#ftl]
#!ps1
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#
#netsh advfirewall firewall add rule name=MongoDB dir=in protocol=tcp action=allow localport=27017 remoteip=any profile=any
#( Get-WmiObject -Namespace "root\Microsoft\SqlServer\ComputerManagement11" -Query "Select * from ServerNetworkProtocolProperty where ProtocolName='Tcp' and IPAddressName='IPAll' and PropertyName='TcpPort'" ).SetStringValue("27017")

New-Item c:\data\db -type directory -force
New-Item c:\data\log -type directory -force

set serviceName=MongoDB${config['mongodb.instance.name']}

& 'C:\Program Files\MongoDB\Server\3.0\bin\mongod' '--rest' '--dbpath=C:\data\db' '--logpath=c:\data\log\service.log' '--install' '--serviceName=MongoDB${config['mongodb.instance.name']}' '--serviceDisplayName=MongoDB${config['mongodb.instance.name']}'