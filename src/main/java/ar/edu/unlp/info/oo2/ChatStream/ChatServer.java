package ar.edu.unlp.info.oo2.ChatStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ChatServer {

	public static void main(String[] args) throws IOException, InterruptedException {
		int port = 8080;

		if (args.length > 0) {
			try {
				port = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Puerto invalido, usando el puerto por defecto: 8080");
			}
		}

		// Crear historial vacio para el puerto (se sobrescribe siempre)
		/*
		 * try (FileWriter fileWriter = new FileWriter("historiales/Historial-" + port +
		 * ".txt", false)) { fileWriter.write(""); } catch (IOException e) {
		 * e.printStackTrace(); }
		 */

		// Crear archivo compartido si no existe (no se sobreescribe)
		/*
		 * File broadcastFile = new File("broadcasts/mensajes-compartidos.txt"); if
		 * (!broadcastFile.exists()) { broadcastFile.getParentFile().mkdirs(); //
		 * Asegura que exista el directorio broadcastFile.createNewFile(); }
		 */

		Server server = ServerBuilder.forPort(port)
				.addService(new ChatServiceImpl(port))
				.build();

		System.out.println("Starting server...");
		server.start();
		System.out.println("Server started on port " + port);

		server.awaitTermination();
	}
}
