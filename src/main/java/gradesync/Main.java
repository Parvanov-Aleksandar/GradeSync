package gradesync;

import gradesync.models.Subject;
import gradesync.models.Post;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.StudentSubmission;

import javax.swing.SwingUtilities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to the Google Classroom Grade Tracker");
        System.out.println("---------------------------------------------");

        try {
            // Instantiate Database and initialize it
            Database database = new Database();
            database.initializeDatabase(); // This handles loading/fetching/persisting data

            // Launch the GradeSync GUI
            SwingUtilities.invokeLater(() -> {
                new GradeSyncGUI().setVisible(true);
            });

        } catch (IOException e) {
            System.err.println("An error occurred during API interaction: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}