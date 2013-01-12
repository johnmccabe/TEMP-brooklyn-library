package brooklyn.entity.webapp

import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import brooklyn.entity.basic.Entities
import brooklyn.location.basic.LocalhostMachineProvisioningLocation
import brooklyn.test.HttpTestUtils
import brooklyn.test.entity.TestApplication
import brooklyn.test.entity.TestApplicationImpl

public class ElasticJavaWebAppServiceIntegrationTest {

    TestApplication app;
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        app = new TestApplicationImpl();
        
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }

    @Test(groups = "Integration")
    public void testFactory() {
        ElasticJavaWebAppService svc =
            new ElasticJavaWebAppService.Factory().newEntity(app, war: "classpath://hello-world.war");
        app.start([new LocalhostMachineProvisioningLocation()]);
        String url = svc.getAttribute(ElasticJavaWebAppService.ROOT_URL);
        Assert.assertNotNull(url);
        HttpTestUtils.assertContentEventuallyContainsText(url, "Hello");
    }
}
