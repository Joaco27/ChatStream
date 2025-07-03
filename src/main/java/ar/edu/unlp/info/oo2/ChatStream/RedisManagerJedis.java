package ar.edu.unlp.info.oo2.ChatStream;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;

public class RedisManagerJedis {
    private final Jedis publisher;
    private final Thread subscriberThread;

    public RedisManagerJedis(String host, int port, Consumer<String> onMessageReceived) {
        this.publisher = new Jedis(host, port);

        this.subscriberThread = new Thread(() -> {
            Jedis subscriber = new Jedis(host, port);
            subscriber.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    onMessageReceived.accept(message);
                }
            }, "broadcast-channel");
        });
        this.subscriberThread.start();
    }

    public void publish(String message) {
        publisher.publish("broadcast-channel", message);
    }

    public void shutdown() {
        publisher.close();
        subscriberThread.interrupt();
    }
}
