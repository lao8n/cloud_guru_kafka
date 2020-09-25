package com.linuxacademy.ccdak.streams;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

public class WindowingMain {

    public static void main(String[] args) {
        // Set up the configuration.
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "windowing-example");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        // Since the input topic uses Strings for both key and value, set the default Serdes to String.
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        // Get the source stream.
        final StreamsBuilder builder = new StreamsBuilder();
        
        // TO RUN
        // Session 1
        // 1. kafka-console-producer --broker-list localhost:9092 --topic windowing-input-topic --property parse.key=true --property key.separator=:
        // 2. a:a
        // Session 2
        // ./gradlew runWindowing
        // Session 3
        // kafka-console-consumer --bootstrap-server localhost:9092 --topic windowing-output-topic --property print.key=true

        // START STREAM IMPLEMENTATION
        KStream<String, String> source = builder.stream("windowing-input-topic");

        KGroupedStream<String, String> groupedStream = source.groupByKey();

        // Apply windowing to the stream with tumbling time windows of 10 seconds.
        TimeWindowedKStream<String, String> windowedStream = groupedStream.windowedBy(TimeWindows.of(Duration.ofSeconds(10))); 
        // add .advancedBy(Duration.ofSeconds(12)) if want hopping time windows

        // Combine the values of all records with the same key into a string separated by spaces, using 10-second windows.
        KTable<Windowed<String>, String> reducedTable = windowedStream.reduce((aggValue, newValue) -> aggValue + " " + newValue);
        reducedTable.toStream().to("windowing-output-topic", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String.class), Serdes.String()));

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
