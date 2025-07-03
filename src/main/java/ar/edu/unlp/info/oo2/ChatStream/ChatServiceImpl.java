package ar.edu.unlp.info.oo2.ChatStream;

import chat.grpc.ChatMessage;
import chat.grpc.ChatServiceGrpc.ChatServiceImplBase;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatServiceImpl extends ChatServiceImplBase {
    private final List<ConnectedClient> connectedClients = new CopyOnWriteArrayList<>();
    private final RedisManagerJedis redisManager;
    private final String historialFile;
    private final String serverId;

    public ChatServiceImpl(int port) {
        this.serverId = "server-" + port;
        this.historialFile = "historiales/Historial-" + port + ".txt";

        if (port == 8080) {
            try (FileWriter fileWriter = new FileWriter("resultados/resultados.txt", false)) {
                fileWriter.write("");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.redisManager = new RedisManagerJedis("redis", 6379, this::handleBroadcastMessage);
    }

    @Override
    public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessage> responseObserver) {
        final ConnectedClient[] currentClient = new ConnectedClient[1];

        return new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage incomingMessage) {
                if (currentClient[0] == null) {
                    currentClient[0] = new ConnectedClient(incomingMessage.getUsername(), responseObserver);
                    connectedClients.add(currentClient[0]);
                    System.out.println("Usuario conectado: " + incomingMessage.getUsername());
                }

                

                guardarEnHistorial(armarLog(incomingMessage));

                for (ConnectedClient client : connectedClients) {
                    if (client.getStream() != responseObserver && !isCancelled(client.getStream())) {
                        client.getStream().onNext(incomingMessage);
                    }
                }

                if (!incomingMessage.getMessage().equals("/connect")) {
                    responseObserver.onNext(incomingMessage);
                }

                String encoded = Base64.getEncoder().encodeToString(incomingMessage.toByteArray());
                redisManager.publish(encoded);
            }

            @Override
            public void onError(Throwable t) {
                connectedClients.removeIf(c -> c.getStream() == responseObserver);
                System.err.println(currentClient[0].getUsername() + " se ha desconectado de forma inesperada");

                ChatMessage disconnectMsg = ChatMessage.newBuilder()
                        .setUsername(currentClient[0].getUsername())
                        .setMessage("/disconnect")
                        .setTimestamp(System.currentTimeMillis())
                        .setSource(serverId)
                        .build();

                guardarEnHistorial(armarLog(disconnectMsg));

                for (ConnectedClient client : connectedClients) {
                    if (!isCancelled(client.getStream())) {
                        client.getStream().onNext(disconnectMsg);
                    }
                }

                redisManager.publish(Base64.getEncoder().encodeToString(disconnectMsg.toByteArray()));
            }

            @Override
            public void onCompleted() {
                connectedClients.removeIf(c -> c.getStream() == responseObserver);
                System.out.println("Usuario desconectado: " + currentClient[0].getUsername());

                ChatMessage disconnectMsg = ChatMessage.newBuilder()
                        .setUsername(currentClient[0].getUsername())
                        .setMessage("/disconnect")
                        .setTimestamp(System.currentTimeMillis())
                        .setSource(serverId)
                        .build();

                guardarEnHistorial(armarLog(disconnectMsg));

                for (ConnectedClient client : connectedClients) {
                    if (!isCancelled(client.getStream())) {
                        client.getStream().onNext(disconnectMsg);
                    }
                }

                redisManager.publish(Base64.getEncoder().encodeToString(disconnectMsg.toByteArray()));
                responseObserver.onCompleted();
            }
        };
    }

    private void handleBroadcastMessage(String base64Encoded) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Encoded);
            ChatMessage message = ChatMessage.parseFrom(decoded);

            if (message.getSource().equals(serverId)) {
                return; // Evitar re-procesar mensajes que origino este servidor
            }

            for (ConnectedClient client : connectedClients) {
                if (!isCancelled(client.getStream())) {
                    client.getStream().onNext(message);
                }
            }

            guardarEnHistorial(armarLog(message));

        } catch (InvalidProtocolBufferException e) {
            System.err.println("Error al decodificar mensaje desde Redis: " + e.getMessage());
        }
    }

    private boolean isCancelled(StreamObserver<?> observer) {
        return observer instanceof io.grpc.stub.ServerCallStreamObserver<?> &&
                ((io.grpc.stub.ServerCallStreamObserver<?>) observer).isCancelled();
    }

    public String formatearTimeStamp(long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return "[" + dateTime.format(formatter) + "] ";
    }

    public void guardarEnHistorial(String contenido) {
        File archivo = new File(this.historialFile);
        try (FileWriter writer = new FileWriter(archivo, true)) {
            writer.write(contenido + "\n");
        } catch (IOException e) {
            System.out.println("Ocurrio un error al escribir en el archivo: " + e.getMessage());
        }
    }

    public String armarLog(ChatMessage m) {
        if (m.getMessage().equals("/connect")) {
            return formatearTimeStamp(m.getTimestamp()) + m.getUsername() + " se unio al chat";
        } else if (m.getMessage().equals("/disconnect")) {
            return formatearTimeStamp(m.getTimestamp()) + m.getUsername() + " salio del chat";
        } else {
            return formatearTimeStamp(m.getTimestamp()) + m.getUsername() + ": " + m.getMessage();
        }
    }
}
