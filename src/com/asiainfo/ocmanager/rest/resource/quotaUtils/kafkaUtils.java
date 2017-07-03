package com.asiainfo.ocmanager.rest.resource.quotaUtils;

import com.asiainfo.ocmanager.persistence.model.Quota;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;

import java.util.*;

/**
 * Created by yujin on 2017/6/29.
 */
public class kafkaUtils {
    private static final Properties props = new Properties();

    static{
        String currentClassPath = new HdfsUtils().getClass().getResource("/").getPath();
        String  jaasPath= currentClassPath.substring(0, currentClassPath.length() - 8) + "conf/kafka-jaas.conf";
        String  krbPath = currentClassPath.substring(0,currentClassPath.length() - 8) + "conf/krb5.conf";
        System.setProperty("java.security.auth.login.config", "conf/kafka-jaas.conf");
        System.setProperty("java.security.krb5.conf", "conf/krb5.conf");
        //System.setProperty("sun.security.krb5.debug", "true");
        props.put("security.protocol", "SASL_PLAINTEXT");
        System.out.println("begin to register for kerberos");
        props.put("bootstrap.servers", "zx-dn-10:6667,zx-dn-11:6667,zx-dn-12:6667,zx-dn-13:6667,zx-dn-14:6667,zx-bdi-01:6667,zx-bdi-02:6667,zx-bdi-03:6667");
        props.put("group.id", "group1");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        System.out.println("keytab is :" + props.getProperty("keyTab"));

    }
    public static Quota getKafkaQuota(String topicName){
        String path=Thread.currentThread().getContextClassLoader().getResource("").toString();
        path=path.replace('/', '\\'); // 将/换成\
        path=path.replace("file:", ""); //去掉file:
        path=path.replace("classes\\", ""); //去掉class\
        path=path.substring(1); //去掉第一个\,如 \D:\JavaWeb...
        path+="conf\\shixiuru.keytab";
        System.out.println("Kafka kafka-jaas.conf 文件中路径："+path);
        KafkaConsumer<byte[], byte[]> consumer = new KafkaConsumer<byte[], byte[]>(props);
        //consumer.subscribe(Arrays.asList(topicName));
        //Map<String, List<PartitionInfo>> listPartionInfoForTopic = consumer.listTopics();
        List<PartitionInfo> PartionInfoForTopic = consumer.partitionsFor(topicName);
        int partitionNum = PartionInfoForTopic.size();
        String partitionNumStr = String.valueOf("1");
        Quota partitionQuota= new Quota("partitionQuota",partitionNumStr,"","","kafka topic partiton num");




        String topic = "__consumer_offsets";
        //port
        int port = Integer.parseInt("6667");
        //查找的分区
        int partition = Integer.parseInt("23");
        // broker节点
        List<String> seeds = new ArrayList<String>();
        seeds.add("zx-dn-10");
        seeds.add("zx-dn-11");
        seeds.add("zx-dn-12");
        seeds.add("zx-dn-13");
        seeds.add("zx-dn-14");
        seeds.add("zx-bdi-01");
        seeds.add("zx-bdi-02");
        seeds.add("zx-bdi-03");




        String clientName = "Client_Leader_LookUp";
        /*for (String seedBroker : seeds) {
            SimpleConsumer consumer1 = null;
            try {
                consumer1 = new SimpleConsumer(seedBroker, port, 100000, 64 * 1024, clientName);
                List<Object> topics = new ArrayList<Object>();
                topics.add(topic);
                TopicMetadataRequest topicMetadataRequest = new TopicMetadataRequest(topics);
                TopicMetadataResponse topicMetadataResponse = consumer1.send(topicMetadataRequest);

                List<TopicMetadata> topicMetadatas = topicMetadataResponse.topicsMetadata();
                for (TopicMetadata topicMetadata : topicMetadatas) {
                    int a = Integer.valueOf(topicMetadata.sizeInBytes().toString());
                    System.out.println("此topic的大小为：" + a);
                }
            } catch (Exception e) {
                System.out.println("error communicating with broker [" + seedBroker + "] to find leader for [" + topic + ", " + partition + "] reason: " + e);
            } finally {
                if (consumer1 != null)
                    consumer1.close();
            }
        }*/
        return partitionQuota;
    }


}
