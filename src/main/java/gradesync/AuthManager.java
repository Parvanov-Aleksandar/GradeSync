package gradesync;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.ClassroomScopes;
 
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

public class AuthManager // Encapsulation
{ 

    private static final String APPLICATION_NAME = "Google Classroom Grade Tracker IA";
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/gradesync");
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static HttpTransport HTTP_TRANSPORT;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES =
        Arrays.asList(ClassroomScopes.CLASSROOM_COURSES_READONLY,
                      ClassroomScopes.CLASSROOM_COURSEWORK_ME_READONLY);

    static
    { // Initializes Google API client components. Use of external libraries
      // Ensures that the necessary components for Google API interaction are set up once when the class is loaded
        try
        { // Attempts to initialize HTTP transport and data store factory
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        }
        catch (GeneralSecurityException | IOException e)
        { // Catches security or IO exceptions during initialization. Error handling
            System.err.println("Error initializing Google API client components: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static Credential authorize() throws IOException
    { // Creates an authorized Credential object, opening a browser for user authentication if needed. Use of external libraries
      // This method abstracts the complex OAuth2 authorization flow into a simple call

        InputStream in = AuthManager.class.getResourceAsStream("/credentials.json");

        if (in == null)
        { // Checks if credentials.json is missing
            System.err.println("ERROR: credentials.json not found in resources folder.");
            System.err.println("Please place credentials.json in src/main/resources folder.");
            throw new IOException("credentials.json not found.");
        }
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in)); // Parses the client secrets JSON file

        GoogleAuthorizationCodeFlow flow = 
            new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        //handles OAuth 2.0 authorisation, opens google login, retrieves access token

        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        // Represents the user's authorization credentials

        return credential; // Method returns the credential
    }

    public static Classroom getClassroomService() throws IOException
    { // Builds and returns an authorized Google Classroom client service
      // Abstraction of Google Classroom API
        Credential credential = authorize(); // Obtains the authorized credential

        return new Classroom.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    public static void deleteCredentials()
    {
        try
        {
            if (DATA_STORE_DIR.exists())
            {
                deleteDirectory(DATA_STORE_DIR);
                System.out.println("Credentials directory deleted: " + DATA_STORE_DIR.getAbsolutePath());
            }
            else
            {
                System.out.println("Credentials directory does not exist: " + DATA_STORE_DIR.getAbsolutePath());
            }
        }
        catch (IOException e)
        {
            System.err.println("Error deleting credentials directory: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(java.io.File directory) throws IOException
    {
        if (directory.isDirectory())
        {
            java.io.File[] files = directory.listFiles();
            if (files != null)
            {
                for (java.io.File file : files)
                {
                    deleteDirectory(file);
                }
            }
        }
        if (!directory.delete())
        {
            throw new IOException("Failed to delete file or directory: " + directory.getAbsolutePath());
        }
    }
}