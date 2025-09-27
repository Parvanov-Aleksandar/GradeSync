package gradesync;

import gradesync.models.Post;
import gradesync.models.Subject;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BackgroundUpdate {

    private static final String JAR_NAME = "GradeSync-1.0-SNAPSHOT.jar";
    private static final String STARTUP_FOLDER = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\Startup";

    public static void main(String[] args) {
        // This main method will be executed when the shortcut runs the background updater
        // It does not create a GUI, but rather run the update logic in the background
        System.out.println("BackgroundUpdate: Main method started.");
        
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        System.out.println("BackgroundUpdate: Scheduler initialized. Task will run every 30 minutes.");
        scheduler.scheduleAtFixedRate(() -> {
            System.out.println("BackgroundUpdate: Executing scheduled update task.");
            try {
                performBackgroundUpdate();
                System.out.println("BackgroundUpdate: Scheduled update task completed successfully.");
            } catch (IOException | SQLException e) {
                System.err.println("BackgroundUpdate: Error during scheduled background update: " + e.getMessage());
                e.printStackTrace(); // Print stack trace for detailed error
            }
        }, 0, 1, TimeUnit.MINUTES); // Run every 30 minutes
    }

    public static void updateOnStartup() {
        File startupDir = new File(STARTUP_FOLDER);
        System.out.println("BackgroundUpdate: Checking startup directory: " + STARTUP_FOLDER);
        if (!startupDir.exists()) {
            System.out.println("BackgroundUpdate: Startup directory does not exist. Creating: " + startupDir.getAbsolutePath());
            startupDir.mkdirs();
            System.out.println("BackgroundUpdate: Startup directory created: " + startupDir.exists());
        } else {
            System.out.println("BackgroundUpdate: Startup directory already exists.");
        }

        File shortcutFile = new File(STARTUP_FOLDER + File.separator + "GradeSyncBackgroundUpdate.lnk");

        System.out.println("BackgroundUpdate: Checking if shortcut exists: " + shortcutFile.getAbsolutePath());
        if (!shortcutFile.exists()) {
            System.out.println("BackgroundUpdate: Shortcut does not exist. Attempting to create it.");
            try {
                // Get the path to the currently running JAR
                String jarPath = new File("target", "GradeSync-1.0-SNAPSHOT.jar").getAbsolutePath();
                System.out.println("BackgroundUpdate: Determined JAR path: " + jarPath);
                File jarFile = new File(jarPath);
                if (!jarFile.exists()) {
                    System.err.println("BackgroundUpdate: ERROR: JAR file not found at " + jarPath + ". Cannot create shortcut.");
                    throw new IOException("JAR file not found.");
                }
                System.out.println("BackgroundUpdate: JAR file exists at: " + jarPath);

                // Create a VBScript to create the shortcut
                String vbsScript =
                    "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n" +
                    "Set oShellLink = WshShell.CreateShortcut(\"" + shortcutFile.getAbsolutePath().replace("\\", "\\\\") + "\")\n" +
                    "oShellLink.TargetPath = \"javaw.exe\"\n" +
                    "oShellLink.Arguments = \"-jar \"\"" + jarPath.replace("\\", "\\\\") + "\"\" gradesync.BackgroundUpdate\"\n" +
                    "oShellLink.WorkingDirectory = \"" + new File(jarPath).getParent().replace("\\", "\\\\") + "\"\n" +
                    "oShellLink.WindowStyle = 7\n" + // Minimized window
                    "oShellLink.Save\n";
                System.out.println("BackgroundUpdate: VBScript content:\n" + vbsScript);

                File tempVbs = File.createTempFile("createShortcut", ".vbs");
                java.nio.file.Files.write(tempVbs.toPath(), vbsScript.getBytes());
                System.out.println("BackgroundUpdate: Temporary VBScript file created at: " + tempVbs.getAbsolutePath());

                ProcessBuilder pb = new ProcessBuilder("wscript", tempVbs.getAbsolutePath());
                System.out.println("BackgroundUpdate: Executing VBScript with command: wscript " + tempVbs.getAbsolutePath());
                Process p = pb.start();
                int exitCode = p.waitFor();
                System.out.println("BackgroundUpdate: VBScript execution finished with exit code: " + exitCode);
                tempVbs.delete(); // Clean up the temporary VBScript file
                System.out.println("BackgroundUpdate: Temporary VBScript file deleted.");

                if (shortcutFile.exists()) {
                    System.out.println("BackgroundUpdate: Startup shortcut created successfully at: " + shortcutFile.getAbsolutePath());
                } else {
                    System.err.println("BackgroundUpdate: ERROR: Shortcut file was not created despite VBScript execution.");
                }
            } catch (Exception e) {
                System.err.println("BackgroundUpdate: Error creating startup shortcut: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("BackgroundUpdate: Startup shortcut already exists at: " + shortcutFile.getAbsolutePath());
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
        checkForNewGradedPosts(oldPosts, newPosts);
        System.out.println("BackgroundUpdate: Finished checking for new graded posts.");

        // Check for target grade achievement
        System.out.println("BackgroundUpdate: Checking for target grade achievement.");
        checkForTargetGradeAchievement(oldSubjects, newSubjects); // This method will now use database-sourced averages
        System.out.println("BackgroundUpdate: Finished checking for target grade achievement.");
        System.out.println("BackgroundUpdate: performBackgroundUpdate() completed.");
    }

    private static void checkForNewGradedPosts(List<Post> oldPosts, List<Post> newPosts) {
        System.out.println("BackgroundUpdate: Entering checkForNewGradedPosts method.");
        if (newPosts == null || newPosts.isEmpty()) {
            System.out.println("BackgroundUpdate: No new posts to check.");
            return;
        }
        for (Post newPost : newPosts) {
            System.out.println("BackgroundUpdate: Processing new post: " + newPost.getTitle() + " (ID: " + newPost.getId() + ")");
            boolean foundInOld = false;
            if (oldPosts != null) {
                for (Post oldPost : oldPosts) {
                    if (newPost.getId().equals(oldPost.getId())) {
                        foundInOld = true;
                        System.out.println("BackgroundUpdate: Post found in old list. Checking for grade changes.");
                        // Check if a grade was assigned or changed
                        if (newPost.getStudentMark() != null && (oldPost.getStudentMark() == null || !newPost.getStudentMark().equals(oldPost.getStudentMark()))) {
                            System.out.println("BackgroundUpdate: Grade changed for post: " + newPost.getTitle() + ". Showing notification.");
                            showNotification("New Grade Received!", "You received a grade for: " + newPost.getTitle() + " in " + newPost.getSubjectId() + " - " + newPost.getPercentageMark() + "%");
                        } else {
                            System.out.println("BackgroundUpdate: No grade change or no grade assigned for post: " + newPost.getTitle());
                        }
                        break;
                    }
                }
            }
            // If it's a completely new post with a grade
            if (!foundInOld && newPost.getStudentMark() != null) {
                System.out.println("BackgroundUpdate: Found a completely new graded post: " + newPost.getTitle() + ". Showing notification.");
                showNotification("New Graded Post!", "A new graded post has been added: " + newPost.getTitle() + " in " + newPost.getSubjectId() + " - " + newPost.getPercentageMark() + "%");
            } else if (!foundInOld && newPost.getStudentMark() == null) {
                System.out.println("BackgroundUpdate: Found a new post without a grade: " + newPost.getTitle() + ". No notification.");
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