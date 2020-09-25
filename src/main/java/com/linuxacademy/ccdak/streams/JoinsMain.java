package com.linuxacademy.ccdak.streams;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public class JoinsMain {

    public static void main(String[] args) {
        // Set up the configuration.
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "joins-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source stream.
        final StreamsBuilder builder = new StreamsBuilder();
        
        // TO RUN
        // Session 1
        // 1. kafka-console-producer --broker-list localhost:9092 --topic joins-input-topic-left --property parse.key=true --property key.separator=:
        // 2. a:a
        // Session 2
        // 1. kafka-console-producer --broker-list localhost:9092 --topic joins-input-topic-right --property parse.key=true --property key.separator=:
        // 2. b:b
        // Session 3
        // 1. ./gradlew runJoins
        // Session 4
        // 1. kafka-console-consumer --bootstrap-server localhost:9092 --topic inner-join-output-topic --property print.key=true
        // Session 5
        // 1. kafka-console-consumer --bootstrap-server localhost:9092 --topic left-join-output-topic --property print.key=true
        // Session 6
        // 1. kafka-console-consumer --bootstrap-server localhost:9092 --topic outer-join-output-topic --property print.key=true

        // START STREAM IMPLEMENTATION
        KStream<String, String> left = builder.stream("joins-input-topic-left");
        KStream<String, String> right = builder.stream("joins-input-topic-right");

        // Perform an inner join
        KStream<String, String> innerJoined = left.join(
            right, 
            (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
            JoinWindows.of(Duration.ofMinutes(5))
        );
        innerJoined.to("inner-join-output-topic");

        // Perform a left join
        KStream<String, String> leftJoined = left.leftJoin(
            right, 
            (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
            JoinWindows.of(Duration.ofMinutes(5))
        );
        leftJoined.to("left-join-output-topic");

        // Perform an outer join
        KStream<String, String> outerJoined = left.outerJoin(
            right, 
            (leftValue, rightValue) -> "left=" + leftValue + ", right=" + rightValue,
            JoinWindows.of(Duration.ofMinutes(5))
        );
        outerJoined.to("outer-join-output-topic");

        // FINISH STREAM IMPLEMENTATION
        
        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        // Print the topology to the console.
        System.out.println(topology.describe());
        final CountDownLatch latch = new CountDownLatch(1);

        // Attach a shutdown handler to catch control-c and terminate the application gracefully.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (final Throwable e) {
            System.out.println(e.getMessage());
            System.exit(1);
        }
        System.exit(0);
    }

}
