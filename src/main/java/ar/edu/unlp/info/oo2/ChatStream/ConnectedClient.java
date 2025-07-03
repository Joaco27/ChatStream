package ar.edu.unlp.info.oo2.ChatStream;

import chat.grpc.ChatMessage;
import io.grpc.stub.StreamObserver;

class ConnectedClient {
    private String username;
    private StreamObserver<ChatMessage> stream;

    ConnectedClient(String username, StreamObserver<ChatMessage> stream) {
        this.username = username;
        this.stream = stream;
    }

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public StreamObserver<ChatMessage> getStream() {
		return stream;
	}

	public void setStream(StreamObserver<ChatMessage> stream) {
		this.stream = stream;
	}
    
    
}
