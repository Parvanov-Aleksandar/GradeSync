package gradesync;

import gradesync.models.Post;
import gradesync.models.Subject;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.StudentSubmission;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static final String DB_PATH = "src/main/resources/gradesync.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found: " + e.getMessage());
            System.exit(1);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(JDBC_URL);
    }

    public boolean databaseExists() {
        File dbFile = new File(DB_PATH);
        return dbFile.exists();
    }

    public List<Subject> initializeDatabase() throws SQLException, IOException {
        if (!databaseExists()) {
            System.out.println("Database does not exist. Creating and initializing schema...");
            createTables();
            fetchAndPersistDataFromAPI();
        } else {
            System.out.println("Database already exists. Loading data from database...");
        }
        return loadAllSubjectsWithPosts(); // Return all subjects with their posts
    }

    private void createTables() throws SQLException {
        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            String createSubjectsTable = "CREATE TABLE IF NOT EXISTS subjects (" +
                                         "id TEXT PRIMARY KEY," +
                                         "name TEXT NOT NULL," +
                                         "grade_boundaries TEXT" + // New column for grade boundaries
                                         ");";
            stmt.execute(createSubjectsTable);

            String createPostsTable = "CREATE TABLE IF NOT EXISTS posts (" +
                                      "id TEXT PRIMARY KEY," +
                                      "subject_id TEXT," + // Changed to TEXT
                                      "title TEXT NOT NULL," +
                                      "creation_time TEXT," +
                                      "due_date TEXT," +
                                      "student_mark REAL," +
                                      "maximum_mark REAL," +
                                      "percentage_mark REAL," + // New column for percentage mark
                                      "grade INTEGER," +       // New column for grade
                                      "comment TEXT," +
                                      "description TEXT," +    // New column for post description
                                      "FOREIGN KEY (subject_id) REFERENCES subjects(id)" +
                                      ");";
           stmt.execute(createPostsTable);
           System.out.println("Tables created successfully.");
        }
    }

    private void fetchAndPersistDataFromAPI() throws IOException, SQLException {
        System.out.println("Fetching data from Google Classroom API...");
        Classroom service = AuthManager.getClassroomService();
        
        List<Course> courses = service.courses().list().setStudentId("me").execute().getCourses();
        System.out.println("Retrieved " + (courses != null ? courses.size() : 0) + " courses from API.");

        if (courses != null && !courses.isEmpty()) {
            Connection conn = null; // Declare connection outside try-with-resources to handle rollback
            try {
                conn = connect();
                conn.setAutoCommit(false); // Start transaction

                String insertSubjectSQL = "INSERT INTO subjects(id, name, grade_boundaries) VALUES(?, ?, ?)";
                String insertPostSQL = "INSERT INTO posts(id, subject_id, title, creation_time, due_date, student_mark, maximum_mark, percentage_mark, grade, comment, description) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

                for (Course course : courses) {
                    System.out.println("Processing course: " + course.getName() + " (ID: " + course.getId() + ")");
                    // Insert Subject
                    try (PreparedStatement pstmt = conn.prepareStatement(insertSubjectSQL)) {
                        pstmt.setString(1, course.getId());
                        pstmt.setString(2, course.getName());
                        // Default grade boundaries
                        String defaultGradeBoundaries = "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}";
                        pstmt.setString(3, defaultGradeBoundaries);
                        int rowsAffected = pstmt.executeUpdate();
                        if (rowsAffected > 0) {
                            System.out.println("Successfully inserted subject: " + course.getName());
                        } else {
                            System.err.println("Failed to insert subject: " + course.getName() + ". No rows affected.");
                        }
                    } catch (SQLException e) {
                        if (e.getMessage().contains("UNIQUE constraint failed")) {
                            System.out.println("Subject " + course.getName() + " (ID: " + course.getId() + ") already exists. Skipping insertion.");
                        } else {
                            System.err.println("Error inserting subject " + course.getName() + ": " + e.getMessage());
                            // Do not re-throw here, allow other subjects/posts to be processed
                        }
                    }

                    // Fetch and insert Posts (CourseWork) for each subject
                    List<CourseWork> courseWorks = service.courses().courseWork().list(course.getId()).execute().getCourseWork();
                    System.out.println("  Retrieved " + (courseWorks != null ? courseWorks.size() : 0) + " coursework items for " + course.getName());

                    if (courseWorks != null && !courseWorks.isEmpty()) {
                        for (CourseWork work : courseWorks) {
                            System.out.println("  Processing coursework: " + work.getTitle() + " (ID: " + work.getId() + ")");
                            Double studentMark = null;
                            Double maximumMark = null; // Initialize maximumMark
                            String comment = "";
                            String description = ""; // Initialize description

                            // Attempt to get student submission and grade
                            try {
                                List<StudentSubmission> submissions = service.courses().courseWork().studentSubmissions().list(course.getId(), work.getId()).execute().getStudentSubmissions();
                                if (submissions != null && !submissions.isEmpty()) {
                                    StudentSubmission submission = submissions.get(0); // Assuming one submission per student
                                    if (submission.getAssignedGrade() != null) {
                                        studentMark = submission.getAssignedGrade().doubleValue();
                                        System.out.println("    Student mark found: " + studentMark);
                                    } else {
                                        System.out.println("    No assigned grade for this submission.");
                                    }
                                } else {
                                    System.out.println("    No student submissions found for this coursework.");
                                }
                            } catch (IOException e) {
                                System.err.println("Warning: Could not fetch submission for coursework '" + work.getTitle() + "'. " + e.getMessage());
                            }

                            // Retrieve maximum points from coursework if available
                            if (work.getMaxPoints() != null) {
                                maximumMark = work.getMaxPoints().doubleValue();
                                System.out.println("    Maximum mark found: " + maximumMark);
                            } else {
                                System.out.println("    No maximum mark found for this coursework.");
                            }

                            // Create a temporary Post object to calculate percentage and grade
                            // We need the subject's grade boundaries for this.
                            // This is a simplified approach; in a real app, you'd fetch the subject first.
                            // For now, we'll use the default boundaries for calculation if subject not found.
                            String subjectGradeBoundaries = getSubjectGradeBoundaries(course.getId());
                            
                            // Retrieve description from coursework if available
                            if (work.getDescription() != null) {
                                description = work.getDescription();
                                System.out.println("    Description found: " + description);
                            } else {
                                System.out.println("    No description found for this coursework.");
                            }

                            Post tempPost = new Post(work.getId(), work.getTitle(),
                                                     work.getCreationTime() != null ? work.getCreationTime().substring(0, 10) : null,
                                                     work.getDueDate(), studentMark, maximumMark, 0.0, 0, comment, description, course.getId());
                            tempPost.calculatePercentageMark();
                            tempPost.calculateGrade(subjectGradeBoundaries);

                            try (PreparedStatement pstmt = conn.prepareStatement(insertPostSQL)) {
                                pstmt.setString(1, tempPost.getId());
                                pstmt.setString(2, course.getId());
                                pstmt.setString(3, tempPost.getTitle());
                                pstmt.setString(4, tempPost.getCreationDate() != null ? tempPost.getCreationDate().toString() : null); // This is already a string from the Post object
                                pstmt.setString(5, tempPost.getDueDate() != null ? tempPost.getDueDate().toString() : null);
                                if (tempPost.getStudentMark() != null) {
                                    pstmt.setDouble(6, tempPost.getStudentMark());
                                } else {
                                    pstmt.setNull(6, Types.REAL);
                                }
                                if (tempPost.getMaximumMark() != null) {
                                    pstmt.setDouble(7, tempPost.getMaximumMark());
                                } else {
                                    pstmt.setNull(7, Types.REAL);
                                }
                                pstmt.setDouble(8, tempPost.getPercentageMark());
                                pstmt.setInt(9, tempPost.getGrade());
                                pstmt.setString(10, tempPost.getComment());
                                pstmt.setString(11, tempPost.getDescription()); // Set description
                                int rowsAffected = pstmt.executeUpdate();
                                if (rowsAffected > 0) {
                                    System.out.println("    Successfully inserted post: " + work.getTitle());
                                } else {
                                    System.err.println("    Failed to insert post: " + work.getTitle() + ". No rows affected.");
                                }
                            } catch (SQLException e) {
                                if (e.getMessage().contains("UNIQUE constraint failed")) {
                                    System.out.println("    Post " + work.getTitle() + " (ID: " + work.getId() + ") already exists. Skipping insertion.");
                                } else {
                                    System.err.println("Error inserting post " + work.getTitle() + ": " + e.getMessage());
                                    // Do not re-throw here, allow other posts to be processed
                                }
                            }
                        }
                    } else {
                        System.out.println("  No coursework found for " + course.getName() + ".");
                    }
                }
                conn.commit(); // Commit transaction
                System.out.println("Data fetched and persisted successfully.");
            } catch (SQLException e) {
                System.err.println("Database transaction failed: " + e.getMessage());
                if (conn != null) {
                    try {
                        conn.rollback(); // Rollback on any major SQL exception
                        System.err.println("Transaction rolled back.");
                    } catch (SQLException rollbackEx) {
                        System.err.println("Error during rollback: " + rollbackEx.getMessage());
                    }
                }
                throw e; // Re-throw to propagate the error to initializeDatabase
            } finally {
                if (conn != null) {
                    try {
                        conn.close(); // Ensure connection is closed
                    } catch (SQLException closeEx) {
                        System.err.println("Error closing connection: " + closeEx.getMessage());
                    }
                }
            }
        } else {
            System.out.println("No courses found from API or courses list is empty.");
        }
    }

    public void updateDatabaseFromAPI() throws IOException, SQLException {
        fetchAndPersistDataFromAPI();
    }

    // CRUD operations for Subject
    public List<Subject> getAllSubjects() {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id, name, grade_boundaries FROM subjects";
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                subjects.add(new Subject(rs.getString("id"), rs.getString("name"), rs.getString("grade_boundaries")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching subjects: " + e.getMessage());
        }
        return subjects;
    }

    public List<Subject> loadAllSubjectsWithPosts() {
        List<Subject> subjects = getAllSubjects();
        for (Subject subject : subjects) {
            List<Post> posts = getPostsBySubjectId(subject.getId());
            for (Post post : posts) {
                subject.addPost(post);
            }
        }
        return subjects;
    }

    public Subject getSubjectById(String id) {
        String sql = "SELECT id, name, grade_boundaries FROM subjects WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new Subject(rs.getString("id"), rs.getString("name"), rs.getString("grade_boundaries"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching subject by ID: " + e.getMessage());
        }
        return null;
    }

    // Helper method to get grade boundaries for a subject
    private String getSubjectGradeBoundaries(String subjectId) {
        String sql = "SELECT grade_boundaries FROM subjects WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subjectId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("grade_boundaries");
            }
        } catch (SQLException e) {
            System.err.println("Error fetching grade boundaries for subject " + subjectId + ": " + e.getMessage());
        }
        // Return default if not found or error
        return "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}";
    }

    // CRUD operations for Post
    public List<Post> getPostsBySubjectId(String subjectId) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT id, title, creation_time, due_date, student_mark, maximum_mark, percentage_mark, grade, comment, description, subject_id FROM posts WHERE subject_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, subjectId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String postId = rs.getString("id");
                String title = rs.getString("title");
                String creationTimeString = rs.getString("creation_time");
                String dueDateString = rs.getString("due_date");
                Double studentMark = rs.getObject("student_mark", Double.class);
                Double maximumMark = rs.getObject("maximum_mark", Double.class);
                double percentageMark = rs.getDouble("percentage_mark");
                int grade = rs.getInt("grade");
                String comment = rs.getString("comment");
                String description = rs.getString("description"); // Retrieve description
                String retrievedSubjectId = rs.getString("subject_id");

                Post post = new Post(postId, title, creationTimeString, null, studentMark, maximumMark, percentageMark, grade, comment, description, retrievedSubjectId);
                if (dueDateString != null) {
                    post.setDueDate(LocalDate.parse(dueDateString));
                }
                posts.add(post);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching posts by subject ID: " + e.getMessage());
        }
        return posts;
    }

    public void updatePostMarkAndComment(String postId, Double mark, String comment) throws SQLException {
        String sql = "UPDATE posts SET student_mark = ?, comment = ? WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (mark != null) {
                pstmt.setDouble(1, mark);
            } else {
                pstmt.setNull(1, Types.REAL);
            }
            pstmt.setString(2, comment);
            pstmt.setString(3, postId);
            pstmt.executeUpdate();
            System.out.println("Post " + postId + " updated successfully.");
        }
    }

    public void deletePost(String postId) throws SQLException {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, postId);
            pstmt.executeUpdate();
            System.out.println("Post " + postId + " deleted successfully.");
        }
    }

    public void deleteSubject(String subjectId) throws SQLException {
        // First delete all posts associated with the subject
        String deletePostsSql = "DELETE FROM posts WHERE subject_id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(deletePostsSql)) {
            pstmt.setString(1, subjectId);
            pstmt.executeUpdate();
        }

        // Then delete the subject itself
        String deleteSubjectSql = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = connect();
             PreparedStatement pstmt = conn.prepareStatement(deleteSubjectSql)) {
            pstmt.setString(1, subjectId);
            pstmt.executeUpdate();
            System.out.println("Subject " + subjectId + " and its posts deleted successfully.");
        }
    }
}