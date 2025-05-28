package gradesync; // <--- MAKE SURE THIS MATCHES YOUR PACKAGE NAME

import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.ListCoursesResponse;

import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to the Google Classroom Grade Tracker (API Version)");
        System.out.println("----------------------------------------------------------");

        try {
            Classroom service = AuthManager.getClassroomService();

            System.out.println("\nListing all courses:");
            ListCoursesResponse response = service.courses().list()
                .setPageSize(10)
                .execute();
            List<Course> courses = response.getCourses();
            if (courses == null || courses.isEmpty()) {
                System.out.println("No courses found.");
            } else {
                for (Course course : courses) {
                    System.out.printf("  Course ID: %s, Name: %s, Status: %s\n",
                                      course.getId(), course.getName(), course.getCourseState());
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