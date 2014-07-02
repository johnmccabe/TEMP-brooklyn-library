package brooklyn.entity.nosql.couchdb;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.test.EntityTestUtils;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.text.Strings;

import com.google.common.collect.ImmutableList;

/**
 * CouchDB live tests.
 *
 * Test the operation of the {@link CouchDBNode} class using the jclouds {@code rackspace-cloudservers-uk}
 * and {@code aws-ec2} providers, with different OS images. The tests use the {@link JcouchdbSupport#jcouchdbTest(CouchDBNode)} method
 * to exercise the node, and will need to have {@code brooklyn.jclouds.provider.identity} and {@code .credential}
 * set, usually in the {@code .brooklyn/brooklyn.properties} file.
 */
public class CouchDBNodeLiveTest extends AbstractCouchDBNodeTest {

    private static final Logger log = LoggerFactory.getLogger(CouchDBNodeLiveTest.class);

    @DataProvider(name = "virtualMachineData")
    public Object[][] provideVirtualMachineData() {
        return new Object[][] { // ImageName, Provider, Region
            new Object[] { "ubuntu", "aws-ec2", "eu-west-1" },
            new Object[] { "Ubuntu 12.0", "rackspace-cloudservers-uk", "" },
            new Object[] { "CentOS 6.2", "rackspace-cloudservers-uk", "" },
        };
    }

    @Test(groups = "Live", dataProvider = "virtualMachineData")
    protected void testOperatingSystemProvider(String imageName, String provider, String region) throws Exception {
        log.info("Testing CouchDB on {}{} using {}", new Object[] { provider, Strings.isNonEmpty(region) ? ":" + region : "", imageName });

        Map<String, String> properties = MutableMap.of("image-name-matches", imageName);
        testLocation = app.getManagementContext().getLocationRegistry()
                .resolve(provider + (Strings.isNonEmpty(region) ? ":" + region : ""), properties);

        couchdb = app.createAndManageChild(EntitySpec.create(CouchDBNode.class)
                .configure("httpPort", "12345+")
                .configure("clusterName", "TestCluster"));
        app.start(ImmutableList.of(testLocation));
        EntityTestUtils.assertAttributeEqualsEventually(couchdb, Startable.SERVICE_UP, true);

        JcouchdbSupport jcouchdb = new JcouchdbSupport(couchdb);
        jcouchdb.jcouchdbTest();
    }
}
