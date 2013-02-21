package brooklyn.entity;

import java.util.Map;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import brooklyn.config.BrooklynProperties;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.Entities;
import brooklyn.location.Location;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.entity.TestApplication;
import brooklyn.util.MutableMap;

import com.google.common.collect.ImmutableMap;

/**
 * Runs a test with many different distros and versions.
 */
public abstract class AbstractEc2LiveTest {
    
    // FIXME Currently have just focused on test_Debian_6; need to test the others as well!

    // TODO No nice fedora VMs
    
    // TODO Instead of this sub-classing approach, we could use testng's "provides" mechanism
    // to say what combo of provider/region/flags should be used. The problem with that is the
    // IDE integration: one can't just select a single test to run.
    
    public static final String PROVIDER = "aws-ec2";
    public static final String REGION_NAME = "us-east-1";
    public static final String LOCATION_SPEC = PROVIDER + (REGION_NAME == null ? "" : ":" + REGION_NAME);
    public static final String TINY_HARDWARE_ID = "t1.micro";
    public static final String SMALL_HARDWARE_ID = "m1.small";
    
    protected BrooklynProperties brooklynProperties;
    protected ManagementContext ctx;
    
    protected TestApplication app;
    protected Location jcloudsLocation;
    
    @BeforeMethod(alwaysRun=true)
    public void setUp() throws Exception {
        // Don't let any defaults from brooklyn.properties (except credentials) interfere with test
        brooklynProperties = BrooklynProperties.Factory.newDefault();
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-description-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-name-regex");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".image-id");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".inboundPorts");
        brooklynProperties.remove("brooklyn.jclouds."+PROVIDER+".hardware-id");

        // Also removes scriptHeader (e.g. if doing `. ~/.bashrc` and `. ~/.profile`, then that can cause "stdin: is not a tty")
        brooklynProperties.remove("brooklyn.ssh.config.scriptHeader");
        
        ctx = new LocalManagementContext(brooklynProperties);
        app = ApplicationBuilder.builder(TestApplication.class).manage(ctx);
    }

    @AfterMethod(alwaysRun=true)
    public void tearDown() throws Exception {
        if (app != null) Entities.destroyAll(app);
    }
    
    @Test(groups = {"Live"})
    public void test_Debian_6() throws Exception {
        // release codename "squeeze"
        // Image: {id=us-east-1/ami-2946a740, providerId=ami-2946a740, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=debian, arch=paravirtual, version=6.0, description=alestic-64/debian-6.0-squeeze-base-64-20090804.manifest.xml, is64Bit=true}, description=alestic-64/debian-6.0-squeeze-base-64-20090804.manifest.xml, version=20090804, status=AVAILABLE[available], loginUser=ubuntu, userMetadata={owner=063491364108, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-2946a740", "loginUser", "root", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_Debian_5() throws Exception {
        // release codename "lenny"
        // Image: {id=us-east-1/ami-2d46a744, providerId=ami-2d46a744, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=debian, arch=paravirtual, version=5.0, description=alestic-64/debian-5.0-lenny-base-64-20090804.manifest.xml, is64Bit=true}, description=alestic-64/debian-5.0-lenny-base-64-20090804.manifest.xml, version=20090804, status=AVAILABLE[available], loginUser=ubuntu, userMetadata={owner=063491364108, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-2d46a744", "loginUser", "root", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_Ubuntu_10_0() throws Exception {
        // Image: {id=us-east-1/ami-5e008437, providerId=ami-5e008437, name=RightImage_Ubuntu_10.04_x64_v5.8.8.3, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=ubuntu, arch=paravirtual, version=10.04, description=rightscale-us-east/RightImage_Ubuntu_10.04_x64_v5.8.8.3.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_Ubuntu_10.04_x64_v5.8.8.3.manifest.xml, version=5.8.8.3, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-5e008437", "loginUser", "root", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_Ubuntu_12_0() throws Exception {
        // Image: {id=us-east-1/ami-1970da70, providerId=ami-1970da70, name=RightImage_Ubuntu_12.04_x64_v5.8.8, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=ubuntu, arch=paravirtual, version=12.04, description=rightscale-us-east/RightImage_Ubuntu_12.04_x64_v5.8.8.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_Ubuntu_12.04_x64_v5.8.8.manifest.xml, version=5.8.8, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-1970da70", "loginUser", "root", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_CentOS_6_3() throws Exception {
        // Image: {id=us-east-1/ami-7d7bfc14, providerId=ami-7d7bfc14, name=RightImage_CentOS_6.3_x64_v5.8.8.5, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=centos, arch=paravirtual, version=6.0, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_CentOS_6.3_x64_v5.8.8.5.manifest.xml, version=5.8.8.5, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-7d7bfc14", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_CentOS_5_6() throws Exception {
        // Image: {id=us-east-1/ami-49e32320, providerId=ami-49e32320, name=RightImage_CentOS_5.6_x64_v5.7.14, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=centos, arch=paravirtual, version=5.6, description=rightscale-us-east/RightImage_CentOS_5.6_x64_v5.7.14.manifest.xml, is64Bit=true}, description=rightscale-us-east/RightImage_CentOS_5.6_x64_v5.7.14.manifest.xml, version=5.7.14, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=instance-store, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-49e32320", "hardwareId", SMALL_HARDWARE_ID));
    }

    @Test(groups = {"Live"})
    public void test_Red_Hat_Enterprise_Linux_6() throws Exception {
        // Image: {id=us-east-1/ami-d37cfbba, providerId=ami-d37cfbba, name=RightImage_RHEL_6.3_x64_v5.8.8.5, location={scope=REGION, id=us-east-1, description=us-east-1, parent=aws-ec2, iso3166Codes=[US-VA]}, os={family=rhel, arch=paravirtual, version=6.0, description=411009282317/RightImage_RHEL_6.3_x64_v5.8.8.5, is64Bit=true}, description=Built by RightScale, version=5.8.8.5, status=AVAILABLE[available], loginUser=root, userMetadata={owner=411009282317, rootDeviceType=ebs, virtualizationType=paravirtual, hypervisor=xen}}
        runTest(ImmutableMap.of("imageId", "us-east-1/ami-d37cfbba", "hardwareId", SMALL_HARDWARE_ID));
    }

    protected void runTest(Map<?,?> flags) throws Exception {
        Map<?,?> jcloudsFlags = MutableMap.builder()
                .putAll(flags)
                .build();
        jcloudsLocation = ctx.getLocationRegistry().resolve(LOCATION_SPEC, jcloudsFlags);

        doTest(jcloudsLocation);
    }
    
    protected abstract void doTest(Location loc) throws Exception;
}
