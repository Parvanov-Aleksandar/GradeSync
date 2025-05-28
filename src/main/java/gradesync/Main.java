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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Main {
    public static void main(String[] args) {
        System.out.println("Welcome to the Google Classroom Grade Tracker (API Version)");
        System.out.println("----------------------------------------------------------");

        try {
            // Build a new authorized Classroom API client service.
            Classroom service = AuthManager.getClassroomService();

            // List to store our custom Subject objects
            List<Subject> subjects = new ArrayList<>();

            // 1. Fetch Courses (Subjects)
            System.out.println("\n--- Fetching Courses (Subjects) ---");
            ListCoursesResponse coursesResponse = service.courses().list()
                .setStudentId("me") // List courses the authenticated user is a student of
                .setPageSize(10)
                .execute();
            List<Course> apiCourses = coursesResponse.getCourses();

            if (apiCourses == null || apiCourses.isEmpty()) {
                System.out.println("No courses found for the authenticated user.");
            } else {
                for (Course apiCourse : apiCourses) {
                    Subject subject = new Subject(apiCourse.getId(), apiCourse.getName());
                    subjects.add(subject);
                    System.out.printf("  Found Course: ID=%s, Name=%s, State=%s\n",
                                      subject.getId(), subject.getName(), apiCourse.getCourseState());
                }
            }

            // 2. For each Subject, fetch its CourseWork (Posts/Assignments)
            System.out.println("\n--- Fetching Coursework (Posts) for each Subject ---");
            for (Subject subject : subjects) {
                System.out.println("\n  Processing Subject: " + subject.getName());
                try {
                    ListCourseWorkResponse courseWorkResponse = service.courses().courseWork().list(subject.getId())
                        .setPageSize(20) // Fetch up to 20 coursework items per course
                        .execute();
                    List<CourseWork> apiCourseWorks = courseWorkResponse.getCourseWork();

                    if (apiCourseWorks == null || apiCourseWorks.isEmpty()) {
                        System.out.println("    No coursework found for this subject.");
                    } else {
                        for (CourseWork apiCourseWork : apiCourseWorks) {
                            // Filter for relevant coursework types (e.g., ASSIGNMENT, QUIZ_ASSIGNMENT)
                            // "SHORT_ANSWER_QUESTION", "MULTIPLE_CHOICE_QUESTION" typically don't have grades directly
                            if (apiCourseWork.getWorkType() != null &&
                                (apiCourseWork.getWorkType().equals("ASSIGNMENT") ||
                                 apiCourseWork.getWorkType().equals("QUIZ_ASSIGNMENT"))) {

                                Post post = new Post(apiCourseWork.getId(), apiCourseWork.getTitle(),
                                // Use creation time if due date isn't available
                                apiCourseWork.getCreationTime() != null ? apiCourseWork.getCreationTime().getMillis() : null);
                                subject.addPost(post);
                                System.out.printf("    Found Coursework: ID=%s, Title='%s', MaxPoints=%s\n",
                                                  post.getId(), post.getTitle(), apiCourseWork.getMaxPoints());

                                // 3. For each CourseWork, fetch Student Submissions (Grades)
                                try {
                                    ListStudentSubmissionsResponse submissionsResponse =
                                        service.courses().courseWork().studentSubmissions().list(
                                            subject.getId(), post.getId()
                                        ).setUserId("me") // Get submissions for the authenticated user only
                                         .execute();
                                    List<StudentSubmission> apiSubmissions = submissionsResponse.getStudentSubmissions();

                                    if (apiSubmissions != null && !apiSubmissions.isEmpty()) {
                                        // A student should typically have only one submission per coursework
                                        StudentSubmission mySubmission = apiSubmissions.get(0); // Take the first one

                                        if (mySubmission.getAssignedGrade() != null) {
                                            post.setStudentMark(mySubmission.getAssignedGrade().doubleValue());
                                            System.out.printf("      -> Student Grade: %s (Max Points: %s)\n",
                                                              post.getStudentMark(), apiCourseWork.getMaxPoints());
                                        } else {
                                            System.out.println("      -> No grade assigned yet, or not submitted.");
                                        }
                                    } else {
                                        System.out.println("      -> No student submission found for this coursework.");
                                    }
                                } catch (IOException e) {
                                    System.err.println("      Error fetching submission for " + post.getTitle() + ": " + e.getMessage());
                                }
                            } else {
                                System.out.printf("    Skipping Coursework '%s' (Type: %s) - not a grade-relevant assignment.\n",
                                                  apiCourseWork.getTitle(), apiCourseWork.getWorkType());
                            }
                        }
                    }
                } catch (IOException e) {
                    System.err.println("  Error fetching coursework for subject " + subject.getName() + ": " + e.getMessage());
                }
            }

            System.out.println("\n--- All Data Fetched ---");
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