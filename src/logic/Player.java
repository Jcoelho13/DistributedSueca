package logic;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import elements.Card;

public class Player{

    private String username;
    private List<Card> hand;
    private int rank;
    private SocketChannel socket;

    public Player(String username, SocketChannel socket){
        this.username = username;
        this.hand = new ArrayList<>(10);
        this.rank = this.getRank();
        this.socket = socket;
    }

    public List<Card> getHand(){
        return this.hand;
    }

    public void setRank(int rank){
        // update the rank in the csv file
        try {
            BufferedReader br = new BufferedReader(new FileReader("assign2/src/files/userinfo.csv"));
            String line;
            String input = "";
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equals(this.username)) {
                    values[3] = Integer.toString(rank);
                    line = String.join(",", values);
                }
                input += line + "\n";
            }
            br.close();
            FileWriter fw = new FileWriter("assign2/src/files/userinfo.csv");
            fw.write(input);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getRank(){
        try (BufferedReader br = new BufferedReader(new FileReader("assign2/src/files/userinfo.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equals(this.username)) {
                    return Integer.parseInt(values[3].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String getUsername(){
        return this.username;
    }

    public SocketChannel getSocket() {
        return this.socket;
    }

    public String toString(){
        return " Username: " + this.username + " - Rank: " + this.rank;
    }

    public String getToken() {
        try (BufferedReader br = new BufferedReader(new FileReader("assign2/src/files/userinfo.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equals(this.username)) {
                    return values[2].trim();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
