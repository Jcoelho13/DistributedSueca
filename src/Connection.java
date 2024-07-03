import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class Connection {

    public static void send(SocketChannel socket, String message) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);      // Create a ByteBuffer with a capacity of 1024 bytes
        buffer.clear();                                     // Clear the buffer
        buffer.put(message.getBytes());                     // Put the message into the buffer
        buffer.flip();                                      // Flip the buffer to prepare it for writing
        while (buffer.hasRemaining()) {                     // Write the buffer to the socket
            socket.write(buffer);
        }
    }

    public static String receive(SocketChannel socket) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(1024);          // Create a ByteBuffer with a capacity of 1024 bytes
        int bytesRead = socket.read(buffer);                    // Read from the socket into the buffer
        return new String(buffer.array(), 0, bytesRead); // Convert the bytes in the buffer to a String and return it
    }
}