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
                System.err.println("BackgroundUpdate: Error during scheduled background update: " + e.getMessage());
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

    private static void performBackgroundUpdate() throws IOException, SQLException {
        System.out.println("BackgroundUpdate: Starting performBackgroundUpdate().");
        Database database = new Database();
        System.out.println("BackgroundUpdate: Database instance created.");
        
        // Store previous state for comparison
        System.out.println("BackgroundUpdate: Fetching old posts and subjects for comparison.");
        List<Post> oldPosts = database.getAllPosts(); // Assuming a method to get all posts
        List<Subject> oldSubjects = database.getAllSubjects();
        System.out.println("BackgroundUpdate: Old posts count: " + (oldPosts != null ? oldPosts.size() : "null"));
        System.out.println("BackgroundUpdate: Old subjects count: " + (oldSubjects != null ? oldSubjects.size() : "null"));

        // Old subjects are already populated with average_mark from the database via getAllSubjects()
        System.out.println("BackgroundUpdate: Old subjects loaded with database average marks.");

        System.out.println("BackgroundUpdate: Calling database.updateDatabaseFromAPI().");
        database.updateDatabaseFromAPI(); // Update the database from API, which also calculates and stores new subject averages
        System.out.println("BackgroundUpdate: database.updateDatabaseFromAPI() completed.");

        // Fetch new state after update
        System.out.println("BackgroundUpdate: Fetching new posts and subjects after API update.");
        List<Post> newPosts = database.getAllPosts(); // Assuming a method to get all posts
        List<Subject> newSubjects = database.getAllSubjects(); // New subjects will have updated average marks from DB
        System.out.println("BackgroundUpdate: New posts count: " + (newPosts != null ? newPosts.size() : "null"));
        System.out.println("BackgroundUpdate: New subjects count: " + (newSubjects != null ? newSubjects.size() : "null"));

        // No need to populate posts for new subjects here, as Subject.getAverageMark() now directly returns the stored value.
        // The average mark is updated in the database during database.updateDatabaseFromAPI().

        // Check for new graded posts
        System.out.println("BackgroundUpdate: Checking for new graded posts.");
        checkForNewGradedPosts(database, oldPosts, newPosts); // Pass database instance
        System.out.println("BackgroundUpdate: Finished checking for new graded posts.");

        // Check for target grade achievement
        System.out.println("BackgroundUpdate: Checking for target grade achievement.");
        checkForTargetGradeAchievement(oldSubjects, newSubjects); // This method will now use database-sourced averages
        System.out.println("BackgroundUpdate: Finished checking for target grade achievement.");
        System.out.println("BackgroundUpdate: performBackgroundUpdate() completed.");
    }

    private static void checkForNewGradedPosts(Database database, List<Post> oldPosts, List<Post> newPosts) {
        System.out.println("BackgroundUpdate: Entering checkForNewGradedPosts method.");
        if (newPosts == null || newPosts.isEmpty()) {
            System.out.println("BackgroundUpdate: No new posts to check.");
            return;
        }

        // Convert oldPosts to a map for efficient lookup
        java.util.Map<String, Post> oldPostsMap = new java.util.HashMap<>();
        if (oldPosts != null) {
            for (Post post : oldPosts) {
                oldPostsMap.put(post.getId(), post);
            }
        }

        for (Post newPost : newPosts) {
            // Since Database.getAllPosts() now filters for student_mark IS NOT NULL,
            // we only need to check for grade changes or new graded posts.
            
            Post oldPost = oldPostsMap.get(newPost.getId());
            String subjectName = "Unknown Subject"; // Default subject name

            Subject subject = database.getSubjectById(newPost.getSubjectId());
            if (subject != null) {
                subjectName = subject.getName();
            } else {
                System.err.println("Warning: Could not fetch subject name for post " + newPost.getTitle() + " (Subject ID: " + newPost.getSubjectId() + ")");
            }

            if (oldPost != null) {
                // Post existed before, check for grade change
                if (!newPost.getStudentMark().equals(oldPost.getStudentMark())) {
                    System.out.println("BackgroundUpdate: Grade changed for post: " + newPost.getTitle() + ". Showing notification.");
                    showNotification("Grade Updated!", "Your grade for: " + newPost.getTitle() + " in " + subjectName + " has changed to " + String.format("%.2f", newPost.getPercentageMark()) + "%");
                } else {
                    System.out.println("BackgroundUpdate: No grade change for post: " + newPost.getTitle());
                }
            } else {
                // Completely new post with a grade (already filtered by getAllPosts)
                System.out.println("BackgroundUpdate: Found a new graded post: " + newPost.getTitle() + ". Showing notification.");
                showNotification("New Grade Received!", "You received a grade for: " + newPost.getTitle() + " in " + subjectName + " - " + String.format("%.2f", newPost.getPercentageMark()) + "%");
            }
        }
        System.out.println("BackgroundUpdate: Exiting checkForNewGradedPosts method.");
    }

    private static void checkForTargetGradeAchievement(List<Subject> oldSubjects, List<Subject> newSubjects) throws SQLException {
        System.out.println("BackgroundUpdate: Entering checkForTargetGradeAchievement method.");
        if (newSubjects == null || newSubjects.isEmpty()) {
            System.out.println("BackgroundUpdate: No new subjects to check.");
            return;
        }
        Database database = new Database(); // Re-instantiate to ensure fresh data if needed
        System.out.println("BackgroundUpdate: Database instance created for target grade check.");
        for (Subject newSubject : newSubjects) {
            System.out.println("BackgroundUpdate: Processing new subject: " + newSubject.getName() + " (ID: " + newSubject.getId() + ")");
            if (oldSubjects != null) {
                for (Subject oldSubject : oldSubjects) {
                    if (newSubject.getId().equals(oldSubject.getId())) {
                        System.out.println("BackgroundUpdate: Subject found in old list. Checking for target grade achievement.");
                        // The newSubject's average mark is already up-to-date from the database after updateDatabaseFromAPI()
                        // No need to recalculate here.

                        System.out.println("BackgroundUpdate: New subject average: " + newSubject.getAverageMark() + "%, Target: " + newSubject.getTargetMark() + "%");
                        System.out.println("BackgroundUpdate: Old subject average: " + oldSubject.getAverageMark() + "%, Target: " + oldSubject.getTargetMark() + "%");

                        if (newSubject.getAverageMark() >= newSubject.getTargetMark() && oldSubject.getAverageMark() < oldSubject.getTargetMark()) {
                            System.out.println("BackgroundUpdate: Target grade achieved for " + newSubject.getName() + "! Showing notification.");
                            showNotification("Target Grade Achieved!", "Congratulations! You have achieved your target grade for " + newSubject.getName() + "!");
                        } else {
                            System.out.println("BackgroundUpdate: Target grade not yet achieved or already achieved for " + newSubject.getName() + ".");
                        }
                        break;
                    }
                }
            } else {
                System.out.println("BackgroundUpdate: Old subjects list is null. Cannot compare for target grade achievement.");
            }
        }
        System.out.println("BackgroundUpdate: Exiting checkForTargetGradeAchievement method.");
    }

    private static void showNotification(String title, String message) {
        // Use SwingUtilities.invokeLater to ensure the dialog is shown on the Event Dispatch Thread
        System.out.println("BackgroundUpdate: Showing notification: Title='" + title + "', Message='" + message + "'");
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE);
            System.out.println("BackgroundUpdate: Notification dialog displayed.");
        });
    }
}