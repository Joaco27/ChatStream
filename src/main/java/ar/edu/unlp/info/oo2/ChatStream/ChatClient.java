package ar.edu.unlp.info.oo2.ChatStream;

import chat.grpc.ChatMessage;
import chat.grpc.ChatServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient {

    public static void main(String[] args) {
    	int port = 8080 + new Random().nextInt(2);
    	
        Map<String, Long> enviados = new ConcurrentHashMap<>();
    	
        // Conexion al servidor
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build();

        ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);
        
        // Leer entrada del usuario
        Scanner scanner = new Scanner(System.in);
        System.out.print("Tu nombre: ");
        String username = scanner.nextLine();

        // Observer que recibe mensajes del servidor
        StreamObserver<ChatMessage> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ChatMessage msg) {
            	if (msg.getUsername().equals(username)) {
            		Long enviado = enviados.remove(msg.getId());
                    if (enviado != null) {
                        long rtt = System.currentTimeMillis() - enviado;
                        //System.out.println("Tiempo de respuesta: " + rtt + " ms");
                    }
            	}
            	else {
            		if (msg.getMessage().equals("/connect")) {
                        System.out.println(msg.getUsername() + " se unio al chat");
                	}
                	else if  (msg.getMessage().equals("/disconnect")){
                        System.out.println(msg.getUsername() + " salio del chat");
                	}
                	else {
                		System.out.println(msg.getUsername() + ": " + msg.getMessage());
                	}
            	}
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Error en el canal: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                System.out.println("Conexion cerrada por el servidor.");
            }
        };

        // Iniciar el canal de comunicacion bidireccional
        StreamObserver<ChatMessage> requestObserver = asyncStub.chat(responseObserver);
        
        ChatMessage connectMsg = ChatMessage.newBuilder()
                .setUsername(username)
                .setMessage("/connect")
                .setTimestamp(System.currentTimeMillis())
                .setSource("server-"+port)
                .build();
        requestObserver.onNext(connectMsg);

        System.out.println("Escribe mensajes. Ctrl+C para salir.");
        while (true) {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("/exit")) {
                requestObserver.onCompleted();
                break;
            }
            String msgId = UUID.randomUUID().toString();
            long timestamp = System.currentTimeMillis();

            ChatMessage msg = ChatMessage.newBuilder()
                    .setUsername(username)
                    .setMessage(input)
                    .setTimestamp(timestamp)
                    .setId(msgId)
                    .setSource("server-"+port)
                    .build();

            enviados.put(msgId, timestamp);

            requestObserver.onNext(msg);
        }

        channel.shutdown();
    }
}
