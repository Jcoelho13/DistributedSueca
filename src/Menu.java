import java.nio.channels.SocketChannel;
import java.util.*;
import logic.Player;

public class Menu {

    private final List<String> activeTokens = new ArrayList<>();

    private final String logo = " ░██████╗██╗░░░██╗███████╗░█████╗░░█████╗░\n" +
                                " ██╔════╝██║░░░██║██╔════╝██╔══██╗██╔══██╗\n" +
                                " ╚█████╗░██║░░░██║█████╗░░██║░░╚═╝███████║\n" +
                                " ░╚═══██╗██║░░░██║██╔══╝░░██║░░██╗██╔══██║\n" +
                                " ██████╔╝╚██████╔╝███████╗╚█████╔╝██║░░██║\n" +
                                " ╚═════╝░░╚═════╝░╚══════╝░╚════╝░╚═╝░░╚═╝\n\n";

    public void initialMenu(SocketChannel playerSocket, List<Player> waitingQueueNormal, List<Player> waitingQueueRanked, String extra) throws Exception {

        String message = clearScreen() +
                "\n        Welcome to the Sueca Game!         \n\n" +  logo + extra +
                "1 - Login\n" +
                "2 - Register\n\n" +
                "0 - Exit\n\n";

        Connection.send(playerSocket, message);
        String option = Connection.receive(playerSocket).trim();
        Authentication auth = new Authentication();

        switch (option) {
            case "1" -> auth = loginMenu(playerSocket, "");

            case "2" -> auth = registerMenu(playerSocket, "");

            case "0" -> {
                playerSocket.close();
                return;
            }

            default -> initialMenu(playerSocket, waitingQueueNormal, waitingQueueRanked, "Invalid option, please try again.\n\n");
        }

        if (auth == null) {
            initialMenu(playerSocket, waitingQueueNormal, waitingQueueRanked, "This account is already logged in, please try again.\n\n");
            return;
        }

        Player player = new Player(auth.getUsername(), playerSocket);

        int gameMode = gameModeMenu(player);

        if(gameMode == 1) {
            waitingQueueNormal.add(player);
            Connection.send(playerSocket, "You entered in waiting queue.\nWaiting for other players...\n\n");
            System.out.println("Player " + player.getUsername() + " is now in the normal waiting queue. Queue size: " + waitingQueueNormal.size());
        }
        else if(gameMode == 2) {
            waitingQueueRanked.add(player);
            Connection.send(playerSocket, "You entered in waiting queue with ranking " + player.getRank() + " points.\nWaiting for other players...\n\n");
            System.out.println("Player " + player.getUsername() + " is now in the ranked waiting queue. Queue size: " + waitingQueueRanked.size());
        }
    }

    public int gameModeMenu(Player player) throws Exception {
        SocketChannel playerSocket = player.getSocket();

        String message = clearScreen() +
                "\n        Welcome " + player.getUsername() + "!" + "\n" +
                "\n        Choose a Game Mode!         \n\n" +  logo +
                "1 - Normal\n" +
                "2 - Ranked\n\n" +
                "0 - Exit\n\n";

        Connection.send(playerSocket, message);
        String option = Connection.receive(playerSocket).trim();

        switch (option) {
            case "1" -> {
                return 1;
            }
            case "2" -> {
                return 2;
            }
            case "0" -> {
                activeTokens.remove(player.getToken());
                playerSocket.close();
                return 0;
            }
            default -> {
                return 0;
            }
        }
    }

    public Authentication loginMenu(SocketChannel playerSocket, String extra) throws Exception {
        String username;
        String password;
        Authentication auth = new Authentication();

        Connection.send(playerSocket, clearScreen() + logo + "Login\n\n");
        Connection.send(playerSocket, extra);
        Connection.send(playerSocket, "Enter your username: ");
        username = Connection.receive(playerSocket).trim();

        Connection.send(playerSocket, "Enter your password: ");
        password = Connection.receive(playerSocket).trim();

        while (!auth.authenticate(username, password) || activeTokens.contains(auth.getToken())) {

            if (activeTokens.contains(auth.getToken())) {
                return null;
            }
            Connection.send(playerSocket, clearScreen() + logo + "Login\n\nInvalid credentials. Please try again.\n\nEnter your username: ");
            username = Connection.receive(playerSocket).trim();

            Connection.send(playerSocket, "Enter your password: ");
            password = Connection.receive(playerSocket).trim();
        }

        activeTokens.add(auth.getToken());

        return auth;
    }

    public Authentication registerMenu(SocketChannel playerSocket, String extra) throws Exception {
        String username;
        String password;
        Authentication auth = new Authentication();

        Connection.send(playerSocket, clearScreen() + logo + "Register\n\n");
        Connection.send(playerSocket, extra);
        Connection.send(playerSocket, "Enter your username: ");
        username = Connection.receive(playerSocket).trim();

        Connection.send(playerSocket, "Enter your password: ");
        password = Connection.receive(playerSocket).trim();

        while (!auth.register(username, password)) {
            Connection.send(playerSocket, clearScreen() + logo + "Register\n\nUsername already being used. Please try again.\n\nEnter your username: ");
            username = Connection.receive(playerSocket).trim();

            Connection.send(playerSocket, "Enter your password: ");
            password = Connection.receive(playerSocket).trim();
        }

        return auth;
    }

    private String clearScreen() {
        return "\n".repeat(50);
    }
}
