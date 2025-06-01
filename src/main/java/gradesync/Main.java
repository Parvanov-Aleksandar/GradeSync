package gradesync;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.sql.SQLException;

public class Main
{ 
    public static void main(String[] args) // Main method
    { 
        Database database = new Database(); // Instantiates the database
        try {
            database.initializeDatabase(); // Initializes the database
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() ->
            {
                new GUI().setVisible(true); // Makes the GUI visible.
            });
    }
}
