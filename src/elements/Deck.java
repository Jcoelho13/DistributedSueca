package elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This program demonstrates a simple deck of cards for the game "sueca".
 * The deck is composed of 40 cards, 4 suits and 10 ranks.
 * The suits are: hearts, diamonds, clubs and spades.
 * The ranks are: A, 7, K, J, Q, 6, 5, 4, 3, 2.
 * The value of the cards is: A=11, 7=10, K=4, J=3, Q=2, 6=0, 5=0, 4=0, 3=0, 2=0.
 */
public class Deck {

    private final List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        String[] suits = {"♥", "♦", "♣", "♠"};
        String[] ranks = {"A", "7", "K", "J", "Q", "6", "5", "4", "3", "2"};
        int[] values = {11, 10, 4, 3, 2, 0, 0, 0, 0, 0};

        for (String suit : suits) {
            for (int j = 0; j < ranks.length; j++) {
                cards.add(new Card(values[j], suit, ranks[j]));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card deal() {
        if (cards.isEmpty()) {
            return null;
        }
        return cards.removeFirst();
    }

    public static String[] getRanks() {
        return new String[]{"A", "7", "K", "J", "Q", "6", "5", "4", "3", "2"};
    }

    public static void sortPlayerHand(List<Card> hand) {
        hand.sort((card1, card2) -> {
            // First, compare by suit
            int suitComparison = card1.getSuit().compareTo(card2.getSuit());
            if (suitComparison != 0) {
                return suitComparison;
            } else {
                // If suits are equal, compare by points
                return Integer.compare(card1.getValue(), card2.getValue());
            }
        });
    }
}