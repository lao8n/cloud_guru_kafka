package com.linuxacademy.ccdak.clients;

import java.util.Duration;
import java.util.Arrays;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.ConsumerRecord;


public class ConsumerMain {

    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("bootstrap.servers", "localhost:9092");
        props.setProperty("group.id", "group1");
        props.setProperty("enable.auto.commit", "false");
        props.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test_topic1", "test_topic2"));

        while(true){
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100)); // can poll more than one record at a time
            for (ConsumerRecord<String, String> record : records){
                System.out.println(
                    "key=" + record.key() + 
                    ", value=" + record.value() + 
                    ", topic=" + record.topic() + 
                    ", partition=" + record.partition() + 
                    ", offset=" + record.offset()
                );
                consumer.commitSync(); // manually sync as enable.auto.commit is false
            }
        }
    }

}
