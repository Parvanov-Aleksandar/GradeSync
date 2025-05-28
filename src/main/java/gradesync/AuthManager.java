/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gradesync;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.ClassroomScopes;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author Aleks
 */
public class AuthManager {

    private static final String APPLICATION_NAME = "GradeSync";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String CREDENTIALS_FILE_PATH = "/client_secret_240847813177-n1cvthe3bqs4s0prfsro9bga7451g4k2.apps.googleusercontent.com.json";
    private static final List<String> SCOPES = Collections.singletonList(ClassroomScopes.CLASSROOM_COURSES_READONLY);
    // If you need more scopes, like basic user info:
    // private static final List<String> SCOPES = Arrays.asList(ClassroomScopes.CLASSROOM_COURSES_READONLY, "https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile");


    public Credential authorize() throws IOException, GeneralSecurityException {
        InputStream in = AuthManager.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new IOException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        System.out.println("DEBUG: Client secrets loaded successfully.");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        NetHttpTransport httpTransport = new NetHttpTransport();
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        System.out.println("DEBUG: GoogleAuthorizationCodeFlow initialized.");

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        System.out.println("DEBUG: LocalServerReceiver created. Attempting to open browser for user authorization...");

        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        System.out.println("DEBUG: User authorization successful. Credentials obtained.");
        return credential;
    }
    
    // It's good practice to have a main method for testing,
    // or move this to a separate test class.
    public static void main(String... args) {
        try {
            AuthManager authManager = new AuthManager();
            Credential credential = authManager.authorize();
            if (credential != null) {
                System.out.println("Authorization successful. Access token: " + credential.getAccessToken());
                // You can now use this credential to make API calls
                // For example, to get the user's email (if userinfo.email scope was requested):
                // com.google.api.services.oauth2.Oauth2 oauth2 = new com.google.api.services.oauth2.Oauth2.Builder(new NetHttpTransport(), JSON_FACTORY, credential).setApplicationName(APPLICATION_NAME).build();
                // com.google.api.services.oauth2.model.Userinfoplus userInfo = oauth2.userinfo().get().execute();
                // System.out.println("User email: " + userInfo.getEmail());
            } else {
                System.out.println("Authorization failed.");
            }
        } catch (IOException | GeneralSecurityException e) {
            System.err.println("ERROR: Authentication failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
