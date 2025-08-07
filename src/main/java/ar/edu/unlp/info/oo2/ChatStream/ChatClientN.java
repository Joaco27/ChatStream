package ar.edu.unlp.info.oo2.ChatStream;

import chat.grpc.ChatMessage;
import chat.grpc.ChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;

public class ChatClientN {

    public static void main(String[] args) {
        int[] ports = {8080, 8081};
        String[] hosts = {"chatserver1", "chatserver2"};

        int idx = new Random().nextInt(ports.length);
        String host = hosts[idx];
        int port = ports[idx];

        int N = 5;
        String un = "user";

        if (args.length > 0) {
            try {
                N = Integer.parseInt(args[0]);
                if (args.length > 1) un = args[1];
            } catch (NumberFormatException e) {
                System.err.println("Cantidad de mensajes invalida, usando 5 por defecto.");
            }
        }

        final String username = un;
        final String serverId = "server-" + port;

        String message = "Hola, soy " + username;
        Map<String, Long> enviados = new ConcurrentHashMap<>();
        List<Long> rtts = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(N);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

        // Observer que recibe mensajes del servidor
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage msg) {
                long timestamp = System.currentTimeMillis();
                if (msg.getUsername().equals(username)) {
                    Long enviado = enviados.remove(msg.getId());
                    if (enviado != null) {
                        long rtt = timestamp - enviado;
                        rtts.add(rtt);
                        latch.countDown();
                    }
                } else {
                    switch (msg.getMessage()) {
                        case "/connect" -> System.out.println(msg.getUsername() + " se unio al chat");
                        case "/disconnect" -> System.out.println(msg.getUsername() + " salio del chat");
                        default -> System.out.println(msg.getUsername() + ": " + msg.getMessage());
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error en el canal: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Conexion cerrada");
            }
        };

        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(responseObserver);

        ChatMessage connectMsg = ChatMessage.newBuilder()
                .setUsername(username)
                .setMessage("/connect")
                .setSource(serverId)
                .setTimestamp(System.currentTimeMillis())
                .build();
        requestObserver.onNext(connectMsg);

        // Enviar mensajes
        for (int i = 0; i < N; i++) {
            String msgId = UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();

            ChatMessage msg = ChatMessage.newBuilder()
                    .setUsername(username)
                    .setMessage(message)
                    .setTimestamp(timestamp)
                    .setId(msgId)
                    .setSource(serverId)
                    .build();

            enviados.put(msgId, timestamp);
            requestObserver.onNext(msg);

            try {
                Thread.sleep(100); // Pausa entre mensajes
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Esperar respuestas
        try {
            boolean ok = latch.await(10, TimeUnit.SECONDS);
            if (!ok) System.out.println("Timeout: no llegaron todas las respuestas");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Avisar cierre al servidor
        requestObserver.onCompleted();

        // Cerrar canal
        channel.shutdown();
        try {
            if (!channel.awaitTermination(10, TimeUnit.SECONDS)) {
                System.err.println("Forzando cierre...");
                channel.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            channel.shutdownNow();
        }

        double promedio = rtts.stream().mapToLong(Long::longValue).average().orElse(0);
        System.out.println("Promedio de RTT " + N + " mensajes: " + promedio + " ms");

        try (PrintWriter writer = new PrintWriter(new FileWriter("resultados/resultados.txt", true))) {
            writer.println(username + " PROMEDIO: " + promedio + " ms");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
