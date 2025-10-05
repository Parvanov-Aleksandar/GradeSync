package gradesync;

import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public class Main
{ 
    public static void main(String[] args) // Main method
    {
        BackgroundUpdate.updateOnStartup(); // Call to create startup shortcut if it doesn't exist

        Database database = new Database(); // Instantiates the database
        try
        {
            database.initializeDatabase(); // Initializes the database
        }
        catch (SQLException | IOException e)
        {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() ->
            {
                new GUI().setVisible(true); // Makes the GUI visible
            });
    }

    public static void restartApplication()
    {
        try
        {
            // Get the current Java runtime
            String javaCommand = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            
            // Get the classpath
            String classpath = System.getProperty("java.class.path");
            
            // Get the main class
            String mainClass = Main.class.getName();

            // Build the command to restart the application
            ProcessBuilder pb = new ProcessBuilder(javaCommand, "-cp", classpath, mainClass);
            pb.start();
            System.exit(0); // Exit the current application
        }
        catch (IOException e)
        {
            JOptionPane.showMessageDialog(null, "Error restarting application: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            System.err.println("Error restarting application: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
