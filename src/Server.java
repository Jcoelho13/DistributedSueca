import logic.Player;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final Menu menu;
    private final int port;
    private ServerSocketChannel serverSocket;
    private long lastPing;
    private final List<Player> waitingQueueNormal;
    private final List<Player> waitingQueueRanked;
    private final ReentrantLock waitingQueueNormalLock;
    private final ReentrantLock waitingQueueRankedLock;
    private final Thread[] authThreads;
    private final Thread[] normalGameThreads;
    private final Thread[] rankedGameThreads;

    public Server(int port) {
        this.port = port;
        this.waitingQueueNormalLock = new ReentrantLock();
        this.waitingQueueRankedLock = new ReentrantLock();
        this.waitingQueueNormal = new ArrayList<>();
        this.waitingQueueRanked = new ArrayList<>();
        this.menu = new Menu();
        this.lastPing = System.currentTimeMillis();
        this.authThreads = new Thread[8];
        this.normalGameThreads = new Thread[2];
        this.rankedGameThreads = new Thread[2];
    }

    // Starts the server and listens for connections on the specified port
    public void start() throws IOException {
        this.serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress(this.port));
        System.out.println("Server is listening on port " + this.port + ".");
    }

    public void run() {
        for (int i = 0; i < authThreads.length; i++) {
            authThreads[i] = new Thread(() -> {
                while (true) connectionAuthenticator();
            });
            authThreads[i].start();
        }

        for (int i = 0; i < normalGameThreads.length; i++) {
            normalGameThreads[i] = new Thread(() -> {
                while (true) {
                    pingPlayers();

                    //Simple mode
                    this.waitingQueueNormalLock.lock();

                    if (this.waitingQueueNormal.size() >= 4) {
                        List<Player> gamePlayers = new ArrayList<>();

                        for (int j = 0; j < 4; j++) {
                            gamePlayers.add(this.waitingQueueNormal.removeFirst());
                            System.out.println("Player " + gamePlayers.get(j).getUsername() + " removed from waiting queue.");
                        }
                        Runnable gameRunnable = new Game(gamePlayers, waitingQueueNormal, waitingQueueNormalLock, true);

                        new Thread(gameRunnable).start();
                    }
                    this.waitingQueueNormalLock.unlock();
                }
            });
            normalGameThreads[i].start();
        }

        for (int i = 0; i < rankedGameThreads.length; i++) {
            rankedGameThreads[i] = new Thread(() -> {
                while (true) {
                    pingPlayers();

                    //Ranked mode
                    this.waitingQueueRankedLock.lock();

                    if (this.waitingQueueRanked.size() >= 4) {
                        this.waitingQueueRanked.sort(Comparator.comparingInt(Player::getRank));
                        List<Player> gamePlayers = new ArrayList<>();

                        for (int j = 0; j < 4; j++) {
                            gamePlayers.add(this.waitingQueueRanked.removeFirst());
                            System.out.println("Player " + gamePlayers.get(j).getUsername() + " removed from waiting queue.");
                        }
                        Runnable gameRunnable = new Game(gamePlayers, waitingQueueRanked, waitingQueueRankedLock, false);

                        new Thread(gameRunnable).start();
                    }
                    this.waitingQueueRankedLock.unlock();
                }
            });
            rankedGameThreads[i].start();
        }

        Thread connectionAuthenticatorThread = new Thread(() -> {
            while (true) connectionAuthenticator();
        });

        connectionAuthenticatorThread.start();
    }

    private void connectionAuthenticator() {
        while (true) {
            try {
                SocketChannel playerSocket = this.serverSocket.accept();
                System.out.println("Player connected: " + playerSocket.getRemoteAddress());

                Runnable newPlayerRunnable = () -> {
                    try {
                        menu.initialMenu(playerSocket, this.waitingQueueNormal, this.waitingQueueRanked, "");
                    } catch (Exception exception) {
                        System.out.println("Error handling player: " + exception);
                    }
                };

                new Thread(newPlayerRunnable).start();

            } catch (Exception exception) {
                System.out.println("Error handling player: " + exception);
            }
        }
    }

    private synchronized void pingPlayers() {
        int PING_INTERVAL = 10000;
        if(System.currentTimeMillis() - this.lastPing > PING_INTERVAL) {
            this.lastPing = System.currentTimeMillis();

            //Simple mode
            this.waitingQueueNormalLock.lock();
            if (this.waitingQueueNormal.isEmpty()) {
                this.waitingQueueNormalLock.unlock();
                return;
            }

            System.out.println("Pinging players in Simple mode...");

            Iterator<Player> iterator = this.waitingQueueNormal.iterator();
            while (iterator.hasNext()) {
                Player player = iterator.next();
                try {
                    Connection.send(player.getSocket(), ". ");
                } catch (IOException exception) {
                    System.out.println("Error pinging player " + player.getUsername() + ": left the queue. / " + exception);
                    iterator.remove();
                } catch (Exception e) {
                    this.waitingQueueNormalLock.unlock();
                    throw new RuntimeException(e);
                }
            }
            this.waitingQueueNormalLock.unlock();

            //Ranked mode
            this.waitingQueueRankedLock.lock();
            if (this.waitingQueueRanked.isEmpty()) {
                this.waitingQueueRankedLock.unlock();
                return;
            }

            System.out.println("Pinging players in Ranked mode...");

            Iterator<Player> iterator2 = this.waitingQueueRanked.iterator();
            while (iterator2.hasNext()) {
                Player player = iterator2.next();
                try {
                    Connection.send(player.getSocket(), ". ");
                } catch (IOException exception) {
                    System.out.println("Error pinging player " + player.getUsername() + ": left the queue. / " + exception);
                    iterator2.remove();
                } catch (Exception e) {
                    this.waitingQueueRankedLock.unlock();
                    throw new RuntimeException(e);
                }
            }
            this.waitingQueueRankedLock.unlock();
        }
    }
}
