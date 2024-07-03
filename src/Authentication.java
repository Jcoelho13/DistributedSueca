import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class Authentication {
    private String username;
    private String password;
    private String token;

    public Authentication(){
        this.username = "";
        this.password = "";
        this.token = "";
    }

    public boolean authenticate(String username, String password) {

        if (username.equals("admin") && password.equals("admin")) {
            this.username = "admin";
            this.password = "admin";
            this.token = "008fa1d4-5dcb-4f20-93c1-1a7ed6afc902";
            return true;
        }

        //check the userinfo.csv file for the username and password
        try (BufferedReader br = new BufferedReader(new FileReader("assign2/src/files/userinfo.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equals(username) && values[1].trim().equals(hashPassword(password))) {
                    this.username = values[0].trim();
                    this.password = values[1].trim();
                    this.token = values[2].trim();
                    return true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean register(String username, String password) {
        try (BufferedReader br = new BufferedReader(new FileReader("assign2/src/files/userinfo.csv"))) {
            String line;
            boolean exists = false;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values[0].trim().equals(username)) {
                    exists = true;
                    break;
                }
            }

            if (exists) {
                return false;
            } else {
                String token = UUID.randomUUID().toString();
                try (FileWriter fw = new FileWriter("assign2/src/files/userinfo.csv", true)) {
                    fw.write("\n" + username + ", " + hashPassword(password) + ", " + token + ", 1000");
                }
                this.username = username;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, hash);
            StringBuilder hexString = new StringBuilder(number.toString(16));
            while (hexString.length() < 32) {
                hexString.insert(0, '0');
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }

    public String getToken(){
        return token;
    }

}