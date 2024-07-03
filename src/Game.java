import elements.Card;
import elements.Deck;
import logic.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * This program demonstrates a simple card game called "sueca".
 * The game is played with 4 players and a deck of 40 cards.
 * The game is composed of 10 rounds, each round has 4 turns.
 * The player with the highest score wins the game.
 *
 * Every round, the player with the highest card wins the round.
 * The team score is updated with the points of the cards played in the round.
 * The player that wins the round starts the next round.
 *
 */

public class Game implements Runnable {
    private Deck deck;
    private List<Player> players;
    private int team1Score = 0;
    private int team2Score = 0;
    private Card trunfo;
    private List<Player> waitingQueue;
    private ReentrantLock waitingQueueLock;
    private boolean normalGame;

    public void run() {
        System.out.println("Starting a game...");
        notifyPlayers(clearScreen(), null);
        notifyPlayers("\nGame started!\n\n", null);

        try {
            startGame(this.players);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Game(List<Player> players, List<Player> waitingQueue, ReentrantLock waitingQueueLock, boolean normalGame) {
        deck = new Deck();
        deck.shuffle();
        this.players = players;
        this.waitingQueue = waitingQueue;
        this.waitingQueueLock = waitingQueueLock;
        this.normalGame = normalGame;
    }

    public void startGame(List<Player> players) throws Exception {
        if(!normalGame) {
            //the teams should be balanced by rank (no team should have a combined much higher rank than the other - difference of 300)
            players.sort(Comparator.comparing(Player::getRank));
            int team1Rank = players.get(0).getRank() + players.get(2).getRank();
            int team2Rank = players.get(1).getRank() + players.get(3).getRank();
            if (Math.abs(team1Rank - team2Rank) > 300) {
                Player temp = players.get(0);
                players.set(0, players.get(1));
                players.set(1, temp);
            }
        }

        for (int i = 0; i < players.size(); i++) {
            if (i == 0) {
                Connection.send(players.get(i).getSocket(), "You are in team 1 with player " + players.get(2).getUsername() + ".");
            } else if (i == 1) {
                Connection.send(players.get(i).getSocket(), "You are in team 2 with player " + players.get(3).getUsername() + ".");
            } else if (i == 2) {
                Connection.send(players.get(i).getSocket(), "You are in team 1 with player " + players.get(0).getUsername() + ".");
            } else {
                Connection.send(players.get(i).getSocket(), "You are in team 2 with player " + players.get(1).getUsername() + ".");
            }
        }

        dealCards(players.get(0).getHand(), players.get(1).getHand(), players.get(2).getHand(), players.get(3).getHand());

        trunfo = players.get(0).getHand().getFirst(); // Assume first card is trunfo for simplicity
        this.notifyPlayers("\n\nTrunfo: " + trunfo + "\n", null);

        Deck.sortPlayerHand(players.get(0).getHand());
        Deck.sortPlayerHand(players.get(1).getHand());
        Deck.sortPlayerHand(players.get(2).getHand());
        Deck.sortPlayerHand(players.get(3).getHand());

        gameLoop();

        notifyPlayers("\nType 1 if you want to play again or 0 to exit\n", null);
        for (Player player : players) {
            String option = Connection.receive(player.getSocket()).trim();
            if (option.equals("1")) {
                Connection.send(player.getSocket(), "\nYou are now in the waiting queue...\n");
                waitingQueueLock.lock();
                waitingQueue.add(player);
                waitingQueueLock.unlock();
            } else {
                Connection.send(player.getSocket(), "You left the game.");
                try {
                    player.getSocket().close();
                } catch (IOException e) {
                    System.out.println("Error when closing the socket: " + e.getMessage());
                }
            }
        }
    }

    public void gameLoop() throws Exception {
        List<Player> initialPlayersOrder = players;

        while (!players.get(0).getHand().isEmpty()
                && !players.get(1).getHand().isEmpty()
                && !players.get(2).getHand().isEmpty()
                && !players.get(3).getHand().isEmpty()) {
            List<Card> cardsPlayedThisRound = new ArrayList<>();
            String[] firstSuit = {null}; // Using an array to store firstSuit as a mutable final variable
            int highestPlayerIndex = 0;
            int highestCardValue = -1;

            for (int i = 0; i < 4; i++) {
                List<Card> currentPlayerHand = players.get(i).getHand();

                this.notifyPlayers("\nCards on the table: " + cardsPlayedThisRound + "\n", null);

                Connection.send(players.get(i).getSocket(), "\nYour turn to play!");
                this.notifyPlayers("\nPlayer " + players.get(i).getUsername() + " is playing...\n", players.get(i));

                while (true) {
                    Connection.send(players.get(i).getSocket(), "\nYour hand: ");
                    Connection.send(players.get(i).getSocket(), currentPlayerHand.toString());
                    Connection.send(players.get(i).getSocket(), "\n\nChoose a card to play: 1 corresponds to " + currentPlayerHand.get(0) + "  and " + (currentPlayerHand.size()) + " corresponds to " + currentPlayerHand.get(currentPlayerHand.size() - 1) + "\n");

                    int cardIndex;
                    try {
                        cardIndex = Integer.parseInt(Connection.receive(players.get(i).getSocket()).trim()) - 1;

                        // Check if cardIndex is within the expected range
                        if (cardIndex < 0 || cardIndex >= currentPlayerHand.size()) {
                            throw new NumberFormatException("Input is not within the expected range.");
                        }
                    } catch (NumberFormatException e) {
                        Connection.send(players.get(i).getSocket(), "Invalid input. Please enter a number between 1 and " + currentPlayerHand.size() + "!");
                        continue;
                    }

                    Card cardToPlay = currentPlayerHand.remove(cardIndex);

                    if (cardToPlay.getValue() > highestCardValue) {
                        highestCardValue = cardToPlay.getValue();
                        highestPlayerIndex = i;
                    }

                    if (firstSuit[0] == null || cardToPlay.getSuit().equals(firstSuit[0]) || currentPlayerHand.stream().noneMatch(card -> card.getSuit().equals(firstSuit[0]))) {
                        cardsPlayedThisRound.add(cardToPlay);
                        break;
                    } else {
                        currentPlayerHand.add(cardToPlay);
                        Connection.send(players.get(i).getSocket(),"You must play a card of the same suit as the first player, unless you don't have any.");
                    }
                }

                if (firstSuit[0] == null) {
                    firstSuit[0] = cardsPlayedThisRound.getFirst().getSuit();
                }
            }

            int points = calculatePoints(cardsPlayedThisRound, trunfo.getSuit());
            if(highestPlayerIndex == 0 || highestPlayerIndex == 2){
                team1Score += points;
            } else {
                team2Score += points;
            }

            Player roundWinner = players.get(highestPlayerIndex);
            if(roundWinner.equals(initialPlayersOrder.get(0)) || roundWinner.equals(initialPlayersOrder.get(2))) {
                this.notifyPlayers(clearScreen() + "\nTeam 1 won this round with " + points + " points!\n", null);
            } else {
                this.notifyPlayers(clearScreen() + "\nTeam 2 won this round with " + points + " points!\n", null);
            }

            reorderPlayersByRoundWinner(roundWinner);
        }

        determineGameWinner(initialPlayersOrder, normalGame);
    }

    private void dealCards(List<Card> player1Hand, List<Card> player2Hand, List<Card> player3Hand, List<Card> player4Hand) {
        deck.shuffle();
        this.notifyPlayers("\nDeck shuffled", null);
        this.notifyPlayers("\nDealing cards...", null);

        for (int i = 0; i < 10; i++) {
            player1Hand.add(deck.deal());
            player2Hand.add(deck.deal());
            player3Hand.add(deck.deal());
            player4Hand.add(deck.deal());
        }
    }

    private void reorderPlayersByRoundWinner(Player roundWinner) {
        int winnerIndex = players.indexOf(roundWinner);

        List<Player> newOrder = new ArrayList<>(players.subList(winnerIndex, players.size()));
        newOrder.addAll(players.subList(0, winnerIndex));

        players = newOrder;
    }

    private void determineGameWinner(List<Player> players, boolean normalGame) {
        String winningTeam = team1Score > team2Score ? "Team 1" : "Team 2";
        this.notifyPlayers("\nThe winner of the game is " + winningTeam + " with " + Math.max(team1Score, team2Score) + " points!", null);
        this.notifyPlayers("\n" + (team1Score < team2Score ? "Team 1" : "Team 2") + " lost with " + Math.min(team1Score, team2Score) + " points!", null);

        if(!normalGame) {
            this.notifyPlayers("\nAll the ranks were updated (+100 for a Win, -100 for a Loss)\n", null);
            if (team1Score > team2Score) {
                players.get(0).setRank(players.get(0).getRank() + 100);
                players.get(2).setRank(players.get(2).getRank() + 100);
                players.get(1).setRank(players.get(1).getRank() - 100);
                players.get(3).setRank(players.get(3).getRank() - 100);
            } else {
                players.get(0).setRank(players.get(0).getRank() - 100);
                players.get(2).setRank(players.get(2).getRank() - 100);
                players.get(1).setRank(players.get(1).getRank() + 100);
                players.get(3).setRank(players.get(3).getRank() + 100);
            }
        }
    }

    public static int calculatePoints(List<Card> cardsPlayedThisRound, String trunfoSuit) {
        Card highestCard = cardsPlayedThisRound.getFirst();
        int points = highestCard.getValue();
        List<Card> remainingCards = cardsPlayedThisRound.subList(1, cardsPlayedThisRound.size());
        for (Card card : remainingCards) {
            if (card.getSuit().equals(highestCard.getSuit()) && card.getValue() > highestCard.getValue()) {
                highestCard = card;
                points += card.getValue();
            } else if (card.getSuit().equals(trunfoSuit) && !highestCard.getSuit().equals(trunfoSuit)) {
                highestCard = card;
                points += card.getValue();
            } else if (card.getSuit().equals(highestCard.getSuit()) && card.getValue() == highestCard.getValue()) {
                if (Arrays.asList(Deck.getRanks()).indexOf(card.getRank()) < Arrays.asList(Deck.getRanks()).indexOf(highestCard.getRank())) {
                    highestCard = card;
                }
            } else {
                points += card.getValue();
            }
        }
        return points;
    }

    public void notifyPlayers(String message, Player excluded) {
        try {
            for (Player player : this.players) {
                if (player.equals(excluded)) continue;
                Connection.send(player.getSocket(), message);
            }
        } catch (Exception exception) {
            System.out.println("Exception: " + exception.getMessage());
        }
    }

    private String clearScreen() {
        return "\n".repeat(50);
    }
}
