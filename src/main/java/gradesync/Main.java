package gradesync; // <--- CORRECTED PACKAGE NAME

import gradesync.models.Subject;
import gradesync.models.Post;

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.StudentSubmission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to the Google Classroom Grade Tracker (API Version)");
        System.out.println("----------------------------------------------------------");

        try {
            // Instantiate Database and initialize it
            Database database = new Database();
            List<Subject> subjects = database.initializeDatabase(); // This handles loading/fetching/persisting data

            System.out.println("\n--- All Data Loaded/Fetched ---");
            System.out.println("Summary of Subjects and Posts:");
            for (Subject subject : subjects) {
                System.out.println(subject);
                for (Post post : subject.getPosts()) {
                    System.out.println(post);
                }
            }

        } catch (IOException e) {
            System.err.println("An error occurred during API interaction: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}