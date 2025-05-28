package gradesync; // Changed package to gradesync

import com.google.api.client.auth.oauth2.Credential;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class Main {
    public static void main(String[] args) throws IOException, GeneralSecurityException {
        AuthManager authManager = new AuthManager();
        Credential credential = authManager.authorize();

        if (credential != null) {
            System.out.println("DEBUG: Authentication successful in Main. Token: " + credential.getAccessToken());
            System.out.println("Successfully signed in with Google!");
            // Proceed with main application logic here
        } else {
            System.out.println("ERROR: Authentication failed. Please check the console for more details.");
            // Handle authentication failure, perhaps exit or prompt user to retry
        }
    }
}
