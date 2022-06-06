import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class EchoServer {

    public static void main(String[] args) throws IOException {

       try (ServerSocket server = new ServerSocket(8189)) {
           System.out.println("Server started");
           while(true) {
               Socket socket = server.accept();
               Handler handler = new Handler(socket);
               new Thread(handler).start();
           }
       }
    }
}
