package ar.edu.unlp.info.oo2.ChatStream;

import chat.grpc.ChatMessage;
import io.grpc.stub.StreamObserver;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServiceImpl extends chat.grpc.ChatServiceGrpc.ChatServiceImplBase {
    private final List<ConnectedClient> connectedClients = new CopyOnWriteArrayList<>();
    private final String serverId;
    private final File broadcastFile;
    private final ExecutorService fileWatcherExecutor = Executors.newSingleThreadExecutor();

    public ChatServiceImpl(int port) {
        this.serverId = "server-" + port;
        this.broadcastFile = new File("mensajes/mensajes-compartidos.txt");


        if (!broadcastFile.exists()) {
            try {
                broadcastFile.getParentFile().mkdirs();
                broadcastFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        startFileWatcher();
    }

    private void startFileWatcher() {
        fileWatcherExecutor.submit(() -> {
            long lastPointer = 0;
            while (true) {
                try (RandomAccessFile raf = new RandomAccessFile(broadcastFile, "r")) {
                    raf.seek(lastPointer);
                    String line;
                    while ((line = raf.readLine()) != null) {
                        ChatMessage message = parseLogLine(line.trim());
                        if (message != null && !message.getSource().equals(serverId)) {
                            handleBroadcastMessage(message);
                        }
                    }
                    lastPointer = raf.getFilePointer();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {}
            }
        });
    }

    private ChatMessage parseLogLine(String line) {
        try {
            // Formato: [dd/MM/yyyy HH:mm:ss] username (server-x): mensaje
            int firstBracket = line.indexOf("]");
            int secondColon = line.indexOf(":", firstBracket);

            String timestampStr = line.substring(1, firstBracket).trim();
            String header = line.substring(firstBracket + 1, secondColon).trim();
            String message = line.substring(secondColon + 1).trim();

            String[] parts = header.split(" ");
            String username = parts[0];
            String source = parts.length > 1 ? parts[1].replace("(", "").replace(")", "") : "";

            long timestamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").parse(timestampStr).getTime();

            return ChatMessage.newBuilder()
                    .setUsername(username)
                    .setMessage(message)
                    .setTimestamp(timestamp)
                    .setSource(source)
                    .build();

        } catch (Exception e) {
            System.err.println("No se pudo parsear linea: " + line);
            return null;
        }
    }

    private void broadcast(ChatMessage msg) {
        synchronized (broadcastFile) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(broadcastFile, true))) {
                writer.write(armarLog(msg) + System.lineSeparator());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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


                for (ConnectedClient client : connectedClients) {
                    if (client.getStream() != responseObserver && !isCancelled(client.getStream())) {
                        client.getStream().onNext(incomingMessage);
                    }
                }

                if (!incomingMessage.getMessage().equals("/connect")) {
                    responseObserver.onNext(incomingMessage);
                }

                broadcast(incomingMessage);
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


                for (ConnectedClient client : connectedClients) {
                    if (!isCancelled(client.getStream())) {
                        client.getStream().onNext(disconnectMsg);
                    }
                }

                broadcast(disconnectMsg);
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


                for (ConnectedClient client : connectedClients) {
                    if (!isCancelled(client.getStream())) {
                        client.getStream().onNext(disconnectMsg);
                    }
                }

                broadcast(disconnectMsg);
                responseObserver.onCompleted();
            }
        };
    }

    private void handleBroadcastMessage(ChatMessage message) {
        for (ConnectedClient client : connectedClients) {
            if (!isCancelled(client.getStream())) {
                client.getStream().onNext(message);
            }
        }

    }

    private boolean isCancelled(StreamObserver<?> observer) {
        return observer instanceof io.grpc.stub.ServerCallStreamObserver<?> &&
                ((io.grpc.stub.ServerCallStreamObserver<?>) observer).isCancelled();
    }

    public String formatearTimeStamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }


    public String armarLog(ChatMessage message) {
        return String.format("[%s] %s (%s): %s",
                formatearTimeStamp(message.getTimestamp()),
                message.getUsername(),
                message.getSource(),
                message.getMessage());
    }
}
