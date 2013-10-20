/*
 * Copyright 2012-2013 by Cloudsoft Corp.
 */
package brooklyn.entity.nosql.cassandra;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.MethodEffector;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.BasicAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.time.Duration;

import com.google.common.base.Supplier;
import com.google.common.collect.Multimap;

/**
 * A cluster of {@link CassandraNode}s based on {@link DynamicCluster} which can be resized by a policy if required.
 * <p>
 * Note that due to how Cassandra assumes ports are the same across a cluster, 
 * it is NOT possible to deploy a cluster to localhost.
 */
@Catalog(name="Apache Cassandra Database Cluster", description="Cassandra is a highly scalable, eventually consistent, distributed, structured key-value store which provides a ColumnFamily-based data model richer than typical key/value systems", iconUrl="classpath:///cassandra-logo.jpeg")
@ImplementedBy(CassandraClusterImpl.class)
public interface CassandraCluster extends DynamicCluster {

    @SetFromFlag("clusterName")
    BasicAttributeSensorAndConfigKey<String> CLUSTER_NAME = new BasicAttributeSensorAndConfigKey<String>(String.class, "cassandra.cluster.name", "Name of the Cassandra cluster", "BrooklynCluster");

    @SetFromFlag("snitchName")
    ConfigKey<String> ENDPOINT_SNITCH_NAME = ConfigKeys.newStringConfigKey("cassandra.cluster.snitchName", "Type of the Cassandra snitch", "SimpleSnitch");

    @SetFromFlag("seedSupplier")
    ConfigKey<Supplier<Set<Entity>>> SEED_SUPPLIER = (ConfigKey) ConfigKeys.newConfigKey(Supplier.class, "cassandra.cluster.seedSupplier", "For determining the seed nodes", null);

    /** Additional time after the nodes in the cluster are up when starting before announcing the cluster as up;
     * Useful to ensure nodes have synchronized.  */
    // on 1.2.2 this could be as much as 120s when using 2 seed nodes, 
    // or just a few seconds with 1 seed node;
    // on 1.2.9 it seems a few seconds is sufficient even with 2 seed nodes
    @SetFromFlag("delayBeforeAdvertisingCluster")
    ConfigKey<Duration> DELAY_BEFORE_ADVERTISING_CLUSTER = ConfigKeys.newConfigKey(Duration.class, "cassandra.cluster.delayBeforeAdvertisingCluster", "Type of the Cassandra snitch", Duration.TEN_SECONDS);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    AttributeSensor<Multimap<String,Entity>> DATACENTER_USAGE = (AttributeSensor)Sensors.newSensor(Map.class, "cassandra.cluster.datacenterUsages", "Current set of datacenters in use, with nodes in each");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    AttributeSensor<Set<String>> DATACENTERS = (AttributeSensor)Sensors.newSensor(Set.class, "cassandra.cluster.datacenters", "Current set of datacenters in use");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    AttributeSensor<Set<Entity>> CURRENT_SEEDS = (AttributeSensor)Sensors.newSensor(Set.class, "cassandra.cluster.seeds.current", "Current set of seeds to use to bootstrap the cluster");
    
    AttributeSensor<String> HOSTNAME = Sensors.newStringSensor("cassandra.cluster.hostname", "Hostname to connect to cluster with");

    AttributeSensor<Integer> THRIFT_PORT = Sensors.newIntegerSensor("cassandra.cluster.thrift.port", "Cassandra Thrift RPC port to connect to cluster with");
    
    AttributeSensor<Long> FIRST_NODE_STARTED_TIME_UTC = Sensors.newLongSensor("cassandra.cluster.first.node.started.utc", "Time (UTC) when the first node was started");
    
    AttributeSensor<Integer> SCHEMA_VERSION_COUNT = Sensors.newIntegerSensor("cassandra.cluster.schema.versions.count", 
            "Number of different schema versions in the cluster; should be 1 for a healthy cluster, 0 when off; " +
            ">=2 indicats a Schema Disagreement Error (and keyspace access may fail)");

    AttributeSensor<Long> READ_PENDING = Sensors.newLongSensor("cassandra.cluster.read.pending", "Current pending ReadStage tasks");
    AttributeSensor<Integer> READ_ACTIVE = Sensors.newIntegerSensor("cassandra.cluster.read.active", "Current active ReadStage tasks");
    AttributeSensor<Long> WRITE_PENDING = Sensors.newLongSensor("cassandra.cluster.write.pending", "Current pending MutationStage tasks");
    AttributeSensor<Integer> WRITE_ACTIVE = Sensors.newIntegerSensor("cassandra.cluster.write.active", "Current active MutationStage tasks");
    
    AttributeSensor<Long> THRIFT_PORT_LATENCY_PER_NODE = Sensors.newLongSensor("cassandra.cluster.thrift.latency.perNode", "Latency for thrift port connection  averaged over all nodes (ms)");
    AttributeSensor<Double> READS_PER_SECOND_LAST_PER_NODE = Sensors.newDoubleSensor("cassandra.reads.perSec.last.perNode", "Reads/sec (last datapoint) averaged over all nodes");
    AttributeSensor<Double> WRITES_PER_SECOND_LAST_PER_NODE = Sensors.newDoubleSensor("cassandra.write.perSec.last.perNode", "Writes/sec (last datapoint) averaged over all nodes");
    AttributeSensor<Double> PROCESS_CPU_TIME_FRACTION_LAST_PER_NODE = Sensors.newDoubleSensor("cassandra.cluster.metrics.processCpuTime.fraction.perNode", "Fraction of CPU time used (percentage reported by JMX), averaged over all nodes");

    AttributeSensor<Double> READS_PER_SECOND_IN_WINDOW_PER_NODE = Sensors.newDoubleSensor("cassandra.reads.perSec.windowed.perNode", "Reads/sec (over time window) averaged over all nodes");
    AttributeSensor<Double> WRITES_PER_SECOND_IN_WINDOW_PER_NODE = Sensors.newDoubleSensor("cassandra.writes.perSec.windowed.perNode", "Writes/sec (over time window) averaged over all nodes");
    AttributeSensor<Double> THRIFT_PORT_LATENCY_IN_WINDOW_PER_NODE = Sensors.newDoubleSensor("cassandra.thrift.latency.windowed.perNode", "Latency for thrift port (ms, over time window) averaged over all nodes");
    AttributeSensor<Double> PROCESS_CPU_TIME_FRACTION_IN_WINDOW_PER_NODE = Sensors.newDoubleSensor("cassandra.cluster.metrics.processCpuTime.fraction.windowed", "Fraction of CPU time used (percentage, over time window), averaged over all nodes");

    MethodEffector<Void> UPDATE = new MethodEffector<Void>(CassandraCluster.class, "update");

    /** sets the number of nodes used to seed the cluster;
     *  v1.2.2 is buggy and requires a big delay for 2 nodes both seeds to reconcile, 
     *  see http://stackoverflow.com/questions/6770894/schemadisagreementexception/18639005
     *  and posts to cassandra mailing list. (Alex, 9 Sept 2013)
     *  <p>
     *  with v1.2.9 this seems fine, with just a few seconds' delay after starting */
    public static final int DEFAULT_SEED_QUORUM = 2;
    
    /** can insert a delay after the first node comes up;
     * is not needed with 1.2.9 (and does not help with the bug in 1.2.2) */
    public static final Duration DELAY_AFTER_FIRST = Duration.ZERO;
    
    /** whether to wait for the first node to start up */
    // not sure whether this is needed or not; need to test in env where not all nodes are seed nodes,
    // what happens if non-seed nodes start before the seed nodes ?
    public static final boolean WAIT_FOR_FIRST = true;
    
    @Effector(description="Updates the cluster members")
    void update();
    
    /**
     * The name of the cluster.
     */
    String getClusterName();

    Collection<Entity> gatherPotentialSeeds();
}
