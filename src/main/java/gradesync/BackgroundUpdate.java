package gradesync;

import gradesync.models.Post;
import gradesync.models.Subject;

import javax.swing.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.nio.file.Files;

public class BackgroundUpdate {

    public static void main(String[] args)
    {
        // This main method will be executed when the vbs script is ran from the windows startup folder (automatically on startup)
        // It does not create the GUI, but rather runs the update logic in the background
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() ->
        {
            try
            {
                performBackgroundUpdate();
            }
            catch (IOException | SQLException e)
            {
                e.printStackTrace(); // Print stack trace for detailed error
            }
        }, 0, 30, TimeUnit.MINUTES); // Run every 30 minutes
    }

    public static void updateOnStartup()
    {
        String startupFolderPath = null;
        try
        {
            // 1. Determine the path to the Windows Startup folder (user-specific)
            String appData = System.getenv("APPDATA");
            if (appData == null)
            {
                appData = System.getProperty("user.home") + File.separator + "AppData" + File.separator + "Roaming";
            }
            startupFolderPath = Paths.get(appData, "Microsoft", "Windows", "Start Menu", "Programs", "Startup").toString();
        }
        catch (Exception e)
        {
            System.err.println("BackgroundUpdate: Could not determine APPDATA or Startup folder path: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        // Define the name of your VBScript file that will be placed in Startup
        String vbsFileName = "GradeSyncBackgroundUpdate.vbs";
        File vbsStartupFile = new File(startupFolderPath + File.separator + vbsFileName);

        if (!vbsStartupFile.exists())
        {
            try
            {
                // --- Determine Application Paths ---
                Path currentCodeSourcePath = Paths.get(gradesync.BackgroundUpdate.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                Path projectRootPath; // This will be C:\Coding\Java\GradeSync

                if (Files.isDirectory(currentCodeSourcePath))
                {
                    // Running from IDE, e.g., C:\Coding\Java\GradeSync\target\classes
                    // Project root is two levels up from 'classes' (target/classes -> target -> projectRoot)
                    projectRootPath = currentCodeSourcePath.getParent().getParent();
                }
                else
                {
                    // Running from a JAR, e.g., C:\Coding\Java\GradeSync\target\GradeSync-1.0-SNAPSHOT.jar
                    // JAR is in 'target', so project root is its parent's parent (target -> projectRoot)
                    projectRootPath = currentCodeSourcePath.getParent().getParent();
                }

                String projectRoot = projectRootPath.normalize().toAbsolutePath().toString();
                String appClassesOrJarPath = currentCodeSourcePath.normalize().toAbsolutePath().toString(); // This is target/classes or the JAR path

                File appCodeLocation = new File(appClassesOrJarPath);
                if (!appCodeLocation.exists())
                {
                    System.err.println("BackgroundUpdate: ERROR: Application code location not found at " + appClassesOrJarPath + ". Cannot create VBScript.");
                }

                // Path to where Maven copies dependencies (e.g., C:\Coding\Java\GradeSync\target\lib)
                Path dependenciesDirPath = Paths.get(projectRoot, "target", "lib");

                // Determine the path to 'java.exe' (as requested to keep functionality, even if it opens a window)
                // If you later want truly silent, change this to 'javaw.exe'
                String javaHome = System.getProperty("java.home");
                Path javaExePath = Paths.get(javaHome, "bin", "java.exe");
                if (!Files.exists(javaExePath))
                {
                    System.err.println("BackgroundUpdate: ERROR: java.exe not found at " + javaExePath + ". Cannot create VBScript.");
                }

                // Full class name for BackgroundUpdate
                String backgroundUpdateClassName = "gradesync.BackgroundUpdate";

                // Construct the full classpath string
                String fullClasspath = appClassesOrJarPath;
                if (Files.exists(dependenciesDirPath) && Files.isDirectory(dependenciesDirPath))
                {
                    fullClasspath += ";" + dependenciesDirPath.toString() + File.separator + "*";
                }

                // --- Construct the VBScript content using Chr(34) for quotes ---
                // Escape all backslashes in paths for VBScript string literals (e.g., C:\ -> C:\\)
                String vbsProjectRoot = projectRoot.replace("\\", "\\\\");
                String vbsJavaExePath = javaExePath.toString().replace("\\", "\\\\");
                String vbsClasspath = fullClasspath.replace("\\", "\\\\");

                // Build the VBScript content line by line using string concatenation in Java
                // Chr(34) will be dynamically inserted into the VBScript string by Java.
                String vbsContent =
                    "Set objShell = CreateObject(\"WScript.Shell\")\n" +
                    "\n" +
                    "strProjectRoot = \"" + vbsProjectRoot + "\"\n" +
                    "strJavaExe = \"" + vbsJavaExePath + "\"\n" +
                    "strClasspath = strProjectRoot & \"\\\\target\\\\classes;\" & strProjectRoot & \"\\\\target\\\\lib\\\\*\"\n" + // Reconstructing classpath as VBScript variable
                    "strMainClass = \"" + backgroundUpdateClassName + "\"\n" +
                    "\n" +
                    "strJavaArgs = \"-cp \" & Chr(34) & strClasspath & Chr(34) & \" \" & strMainClass\n" +
                    "\n" +
                    "strFullJavaCommand = Chr(34) & strJavaExe & Chr(34) & \" \" & strJavaArgs\n" +
                    "\n" +
                    "strCommandToRun = \"cmd /c \" & Chr(34) & \"cd /d \" & Chr(34) & strProjectRoot & Chr(34) & \" && \" & strFullJavaCommand & Chr(34)\n" +
                    "\n" +
                    "objShell.Run strCommandToRun, 0, False\n"; // 0 for hidden window

                // Write the VBScript content to the file in the Startup folder
                Files.write(vbsStartupFile.toPath(), vbsContent.getBytes());

                if (!vbsStartupFile.exists())
                {
                    System.err.println("BackgroundUpdate: ERROR: VBScript startup file was not created.");
                }
            }
            catch (Exception e)
            {
                System.err.println("BackgroundUpdate: An unexpected error occurred while creating the VBScript startup file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static void performBackgroundUpdate() throws IOException, SQLException
    {
        Database database = new Database(); // Create a new instance of Database
        
        // Store previous state for comparison
        List<Post> oldPosts = database.getAllPosts(); // Assuming a method to get all posts
        List<Subject> oldSubjects = database.getAllSubjects(); // Old subjects with average_mark from DB

        database.updateDatabaseFromAPI(); // Update the database from API, which also calculates and stores new subject averages

        // Fetch new state after update
        List<Post> newPosts = database.getAllPosts(); // Assuming a method to get all posts
        List<Subject> newSubjects = database.getAllSubjects(); // New subjects will have updated average marks from DB

        // Check for new graded posts
        checkForNewGradedPosts(database, oldPosts, newPosts); // Pass database instance

        // Check for target grade achievement
        checkForTargetGradeAchievement(oldSubjects, newSubjects); // This method will now use database-sourced averages
    }

    private static void checkForNewGradedPosts(Database database, List<Post> oldPosts, List<Post> newPosts)
    {
        if (newPosts == null || newPosts.isEmpty())
        {
            return;
        }

        // Convert oldPosts to a map for efficient lookup
        java.util.Map<String, Post> oldPostsMap = new java.util.HashMap<>();
        if (oldPosts != null)
        {
            for (Post post : oldPosts)
            {
                oldPostsMap.put(post.getId(), post);
            }
        }

        for (Post newPost : newPosts)
        {
            // Since Database.getAllPosts() now filters for student_mark IS NOT NULL,
            // we only need to check for grade changes or new graded posts.
            
            Post oldPost = oldPostsMap.get(newPost.getId());
            String subjectName = "Unknown Subject"; // Default subject name

            Subject subject = database.getSubjectById(newPost.getSubjectId());
            if (subject != null)
            {
                subjectName = subject.getName();
            }

            if (oldPost != null)
            {
                // Post existed before, check for grade change
                if (!newPost.getStudentMark().equals(oldPost.getStudentMark()))
                {
                    showNotification("Grade Updated!", "Your grade for: " + newPost.getTitle() + " in " + subjectName + " has changed to " + String.format("%.2f", newPost.getPercentageMark()) + "%");
                }
            }
            else
            {
                // Completely new post with a grade (already filtered by getAllPosts)
                showNotification("New Grade Received!", "You received a grade for: " + newPost.getTitle() + " in " + subjectName + " - " + String.format("%.2f", newPost.getPercentageMark()) + "%");
            }
        }
    }

    private static void checkForTargetGradeAchievement(List<Subject> oldSubjects, List<Subject> newSubjects) throws SQLException
    {
        if (newSubjects == null || newSubjects.isEmpty())
        {
            return;
        }
        Database database = new Database(); // Re-instantiate to ensure fresh data if needed
        for (Subject newSubject : newSubjects)
        {
            if (oldSubjects != null)
            {
                for (Subject oldSubject : oldSubjects)
                {
                    if (newSubject.getId().equals(oldSubject.getId()))
                    {
                        // The newSubject's average mark is already up-to-date from the database after updateDatabaseFromAPI()
                        // No need to recalculate here, just compare the values
                        if (newSubject.getAverageMark() >= newSubject.getTargetMark() && oldSubject.getAverageMark() < oldSubject.getTargetMark())
                        {
                            showNotification("Target Grade Achieved!", "Congratulations! You have achieved your target grade for " + newSubject.getName() + "!");
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void showNotification(String title, String message)
    {
        // Use SwingUtilities.invokeLater to ensure the dialog is shown on the Event Dispatch Thread
        SwingUtilities.invokeLater(() ->
        {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
        });
    }
}