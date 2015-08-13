/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.entity.webapp.tomcat;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.testng.Assert.assertNotNull;

import java.net.URL;

import org.apache.brooklyn.api.entity.proxying.EntitySpec;
import org.apache.brooklyn.entity.webapp.tomcat.TomcatServer;
import org.apache.brooklyn.test.HttpTestUtils;
import org.apache.brooklyn.test.TestResourceUnavailableException;
import org.testng.annotations.Test;

import brooklyn.entity.AbstractSoftlayerLiveTest;
import brooklyn.location.Location;
import brooklyn.test.Asserts;

import com.google.common.collect.ImmutableList;

/**
 * A simple test of installing+running on Softlayer, using various OS distros and versions. 
 */
public class TomcatServerSoftlayerLiveTest extends AbstractSoftlayerLiveTest {

    public String getTestWar() {
        TestResourceUnavailableException.throwIfResourceUnavailable(getClass(), "/hello-world.war");
        return "classpath://hello-world.war";
    }

    @Override
    protected void doTest(Location loc) throws Exception {
        final TomcatServer server = app.createAndManageChild(EntitySpec.create(TomcatServer.class)
                .configure("war", getTestWar()));
        
        app.start(ImmutableList.of(loc));
        
        String url = server.getAttribute(TomcatServer.ROOT_URL);
        
        HttpTestUtils.assertHttpStatusCodeEventuallyEquals(url, 200);
        HttpTestUtils.assertContentContainsText(url, "Hello");
        
        Asserts.succeedsEventually(new Runnable() {
            @Override public void run() {
                assertNotNull(server.getAttribute(TomcatServer.REQUEST_COUNT));
                assertNotNull(server.getAttribute(TomcatServer.ERROR_COUNT));
                assertNotNull(server.getAttribute(TomcatServer.TOTAL_PROCESSING_TIME));
                
                // TODO These appear not to be set in TomcatServerImpl.connectSensors
                //      See TomcatServerEc2LiveTest, where these are also not included.
//                assertNotNull(server.getAttribute(TomcatServer.MAX_PROCESSING_TIME));
//                assertNotNull(server.getAttribute(TomcatServer.BYTES_RECEIVED));
//                assertNotNull(server.getAttribute(TomcatServer.BYTES_SENT));
            }});
    }

    @Test(groups = {"Live", "Live-sanity"})
    @Override
    public void test_Ubuntu_12_0_4() throws Exception {
        super.test_Ubuntu_12_0_4();
    }
}
