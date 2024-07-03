import java.io.IOException;

public class Main {
    private Server server;

    public static void main(String[] args) throws IOException {
        Main main = new Main();

        main.server = new Server(8000);
        main.server.start();
        main.server.run();
    }
}
