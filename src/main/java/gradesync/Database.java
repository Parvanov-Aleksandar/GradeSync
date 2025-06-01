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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database
{ // This class demonstrates Encapsulation by managing database interactions and hiding the underlying SQL complexities from other parts of the application
    private static final String DB_PATH = "src/main/resources/gradesync.db";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    public Database()
    { // Constructor for Database class. Use of external libraries. This constructor ensures the necessary JDBC driver is loaded for database connectivity
        try
        { // Attempts to load the SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
        }
        catch (ClassNotFoundException e) // Catches if the JDBC driver is not found. This handles a critical dependency error
        { 
            System.err.println(e);
        }
    }

    private Connection connect() throws SQLException
    { // Establishes a connection to the SQLite database. Provides a low-level connection, abstracted by higher-level methods
        return DriverManager.getConnection(JDBC_URL);
    }

    public boolean databaseExists()
    { // Checks if the database file exists on the file system. Provides a simple abstraction for file system checks
        File dbFile = new File(DB_PATH);
        return dbFile.exists();
    }

    public List<Subject> initializeDatabase() throws SQLException, IOException
    { // Initializes the database, creating tables and fetching data if it doesn't exist. Initial setup of the database, abstraction
        if (!databaseExists())
        { // Checks if the database file exists. This conditional logic ensures idempotent initialization.
            createTables(); // Creates database tables.
            fetchAndPersistDataFromAPI(); // Fetches and persists data from API.
        }
        return loadAllSubjectsWithPosts(); // Returns all subjects with their posts.
    }

    private void createTables() throws SQLException
    { // Creates the subjects and posts tables in the database. Use of external libraries. This method encapsulates the DDL operations for database schema creation.
        try (Connection conn = connect(); Statement stmt = conn.createStatement())
        { // Establishes connection and creates a statement. This uses a try-with-resources for automatic resource management.
            String createSubjectsTable = "CREATE TABLE IF NOT EXISTS subjects (" +
                                         "id TEXT PRIMARY KEY," +
                                         "name TEXT NOT NULL," +
                                         "grade_boundaries TEXT," +
                                         "target_mark INTEGER DEFAULT 0," +
                                         "average_mark REAL DEFAULT 0.0," +
                                         "target_mark_achieved BOOLEAN DEFAULT FALSE" +
                                         ");";
            stmt.execute(createSubjectsTable); // Executes SQL to create subjects table.

            String createPostsTable = "CREATE TABLE IF NOT EXISTS posts (" +
                                      "id TEXT PRIMARY KEY," +
                                      "subject_id TEXT," +
                                      "title TEXT NOT NULL," +
                                      "creation_time TEXT," +
                                      "due_date TEXT," +
                                      "student_mark REAL," +
                                      "maximum_mark REAL," +
                                      "percentage_mark REAL," +
                                      "grade INTEGER," +
                                      "comment TEXT," +
                                      "description TEXT," +
                                      "FOREIGN KEY (subject_id) REFERENCES subjects(id)" +
                                      ");";
          stmt.execute(createPostsTable); // Executes SQL to create posts table.

        }
    }

    private void fetchAndPersistDataFromAPI() throws IOException, SQLException
    { // Fetches course and coursework data from the Google Classroom API and persists it to the database. Use of external libraries. This method demonstrates Abstraction by interacting with the Google Classroom API through a simplified interface provided by `AuthManager`.
        Classroom service = AuthManager.getClassroomService(); // Obtains the Classroom service instance.
        
        List<Course> courses = service.courses().list().setStudentId("me").execute().getCourses(); // Fetches courses from the API. This involves network I/O and data deserialization.

        if (courses != null && !courses.isEmpty())
        { // Checks if courses were retrieved. This conditional statement ensures that processing only proceeds if there is data to work with.
            Connection conn = null; // Declares a connection object, initialized to null for proper finally block handling.
            try{ // Manages database connection and transaction. This try-catch block ensures transactional integrity.
                conn = connect(); // Establishes a database connection.
 
                conn.setAutoCommit(false); // Starts a database transaction. This ensures atomicity for data persistence.
 
                String insertSubjectSQL = "INSERT INTO subjects(id, name, grade_boundaries, target_mark, average_mark, target_mark_achieved) VALUES(?, ?, ?, ?, ?, ?)";
                String insertPostSQL = "INSERT INTO posts(id, subject_id, title, creation_time, due_date, student_mark, maximum_mark, percentage_mark, grade, comment, description) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
 
                 for (Course course : courses)
                 { // Loops through each course retrieved from the API. Complexity: Loop. This loop processes each course individually.
                     try (PreparedStatement pstmt = conn.prepareStatement(insertSubjectSQL))
                     { // Prepares and executes statement to insert subject. This uses a try-with-resources for automatic statement closing.
                         pstmt.setString(1, course.getId());
                         pstmt.setString(2, course.getName());
                         String defaultGradeBoundaries = "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}";
                         pstmt.setString(3, defaultGradeBoundaries);
                         pstmt.setInt(4, 85);
                         pstmt.setDouble(5, 0.0);
                         pstmt.setBoolean(6, false);
                         int rowsAffected = pstmt.executeUpdate();
                     }
                     catch (SQLException e)
                     { // Catches SQL exceptions during subject insertion. This handles potential database errors, such as unique constraint violations.
                         if (e.getMessage().contains("UNIQUE constraint failed"))
                         { // Checks for unique constraint violation. This prevents duplicate subject entries.
                         }
                         else
                         { // Handles other SQL errors.
                             System.err.println("Error inserting subject " + course.getName() + ": " + e.getMessage());
                         }
                     }
 
                     List<CourseWork> courseWorks = service.courses().courseWork().list(course.getId()).execute().getCourseWork(); // Fetches coursework for the current course. This is another interaction with an external library.
 
                     if (courseWorks != null && !courseWorks.isEmpty())
                     { // Checks if coursework was retrieved. This ensures that processing only proceeds if there is coursework data.
                         for (CourseWork work : courseWorks)
                         { // Loops through each coursework item. Complexity: Loop. This nested loop processes each coursework item for the current course.
                             Double studentMark = null;
                             Double maximumMark = null;
                             String comment = "";
                             String description = "";
 
                             try
                             { // Attempts to fetch student submissions. This block handles potential network or API errors during submission retrieval.
                                 List<StudentSubmission> submissions = service.courses().courseWork().studentSubmissions().list(course.getId(), work.getId()).execute().getStudentSubmissions();
                                 if (submissions != null && !submissions.isEmpty())
                                 { // Checks if submissions exist. This ensures that submission data is available before processing.
                                     StudentSubmission submission = submissions.get(0); // Assumes one submission per student.
                                     if (submission.getAssignedGrade() != null)
                                     { // Checks if an assigned grade exists.
                                         studentMark = submission.getAssignedGrade().doubleValue();
                                     }
                                     else
                                     { // No assigned grade.
                                     }
                                 }
                                 else
                                 { // No student submissions.
                                 }
                             }
                             catch (IOException e)
                             { // Catches IO exceptions during submission fetch. This provides error feedback for failed API calls.
                                 System.err.println("Warning: Could not fetch submission for coursework '" + work.getTitle() + "'. " + e.getMessage());
                             }
 
                             if (work.getMaxPoints() != null)
                             { // Checks if maximum points are available. This ensures that mark calculations are based on valid data.
                                maximumMark = work.getMaxPoints().doubleValue();
                             }
                             else
                             { // No maximum mark.
                             }
 
                             String subjectGradeBoundaries = getSubjectGradeBoundaries(course.getId()); // Retrieves subject grade boundaries. This demonstrates Abstraction by calling another method to get specific data.
                             
                             if (work.getDescription() != null)
                             { // Checks if a description exists.
                                 description = work.getDescription();
                             }
                             else
                             { // No description.
                             }
 
                             Post tempPost = new Post(work.getId(), work.getTitle(),
                                                      work.getCreationTime() != null ? work.getCreationTime().substring(0, 10) : null,
                                                      work.getDueDate(), studentMark, maximumMark, 0.0, 0, comment, description, course.getId()); // Creates a new Post object. This demonstrates Encapsulation by creating an instance of the Post class.
                             tempPost.calculatePercentageMark(); // Calculates percentage mark for the post.
                             tempPost.calculateGrade(subjectGradeBoundaries); // Calculates grade for the post.
 
                             try (PreparedStatement pstmt = conn.prepareStatement(insertPostSQL))
                             { // Prepares and executes statement to insert post. This uses a try-with-resources for automatic statement closing.
                                 pstmt.setString(1, tempPost.getId());
                                 pstmt.setString(2, course.getId());
                                 pstmt.setString(3, tempPost.getTitle());
                                 pstmt.setString(4, tempPost.getCreationDate() != null ? tempPost.getCreationDate().toString() : null);
                                 pstmt.setString(5, tempPost.getDueDate() != null ? tempPost.getDueDate().toString() : null);
                                 
                                 if (tempPost.getStudentMark() != null)
                                 { // Sets student mark if not null.
                                     pstmt.setDouble(6, tempPost.getStudentMark());
                                 }
                                 else
                                 { // Sets student mark to null if null.
                                     pstmt.setNull(6, Types.REAL);
                                 }
                                 if (tempPost.getMaximumMark() != null)
                                 { // Sets maximum mark if not null.
                                     pstmt.setDouble(7, tempPost.getMaximumMark());
                                 }
                                 else
                                 { // Sets maximum mark to null if null.
                                     pstmt.setNull(7, Types.REAL);
                                 }
                                 pstmt.setDouble(8, tempPost.getPercentageMark());
                                 pstmt.setInt(9, tempPost.getGrade());
                                 pstmt.setString(10, tempPost.getComment());
                                 pstmt.setString(11, tempPost.getDescription());
                                 int rowsAffected = pstmt.executeUpdate();
                             }
                             catch (SQLException e)
                             { // Catches SQL exceptions during post insertion. This handles potential database errors for post entries.
                                 if (e.getMessage().contains("UNIQUE constraint failed"))
                                 { // Checks for unique constraint violation. This prevents duplicate post entries.
                                 }
                                 else
                                 { // Handles other SQL errors.
                                    System.err.println("Error inserting post " + work.getTitle() + ": " + e.getMessage());
                                 }
                             }
                         }
                     }
                     else
                     { // No coursework found.
                     }
                 }
                 conn.commit(); // Commits the database transaction. This finalizes all insertions within the transaction.
             } catch (SQLException e)
             { // Catches SQL exceptions during the transaction. This is a broad catch for any database-related errors during the entire persistence process.
                 System.err.println("Database transaction failed: " + e.getMessage());
                 if (conn != null
                 ){ // Checks if connection is not null.
                     try
                     { // Attempts to rollback the transaction. This ensures data consistency in case of an error.
                         conn.rollback();
                     }
                     catch (SQLException rollbackEx)
                     { // Catches errors during rollback. This handles potential issues during the rollback process itself.
                         System.err.println("Error during rollback: " + rollbackEx.getMessage());
                     }
                 }
                 throw e; // Re-throws the exception to propagate the error.
             } finally
             { // Ensures connection is closed. This block guarantees resource cleanup.
                 if (conn != null)
                 { // Checks if connection is not null.
                     try
                     { // Attempts to close the connection.
                         conn.close();
                     }
                     catch (SQLException closeEx)
                     { // Catches errors during connection close. This handles potential issues during connection termination.
                         System.err.println("Error closing connection: " + closeEx.getMessage());
                     }
                 }
             }
         }
         else
         { // No courses found.
         }
     }

    public void updateDatabaseFromAPI() throws IOException, SQLException
    { // Updates the database by fetching and persisting data from the API. Use of external libraries. This method provides a public interface for refreshing database content from the API.
        fetchAndPersistDataFromAPI(); // Calls method to fetch and persist data. This demonstrates Abstraction by delegating to a private helper method.
    }

    public List<Subject> getAllSubjects()
    { // Retrieves all subjects from the database. Use of external libraries. This method provides a read-only view of subjects, demonstrating Abstraction over the database query.
        List<Subject> subjects = new ArrayList<>(); // Initializes an empty list to hold Subject objects.
        String sql = "SELECT id, name, grade_boundaries, target_mark, average_mark, target_mark_achieved FROM subjects"; // SQL query to select all subjects.
        try (Connection conn = connect(); // Establishes connection. This ensures the connection is closed automatically.
             Statement stmt = conn.createStatement(); // Creates a statement.
             ResultSet rs = stmt.executeQuery(sql))
        { // Executes query and gets results. This retrieves data from the database.
            while (rs.next())
            { // Loops through each row in the result set. Complexity: Loop. This iterates over the fetched database records.
                subjects.add(new Subject(rs.getString("id"), rs.getString("name"), rs.getString("grade_boundaries"), rs.getInt("target_mark"), rs.getDouble("average_mark"), rs.getBoolean("target_mark_achieved"))); // Creates a new Subject object and adds it to the list. This demonstrates Encapsulation by creating objects from database rows.
            }
        }
        catch (SQLException e)
        { // Catches SQL exceptions during subject fetching. This handles potential database errors during data retrieval.
            System.err.println("Error fetching subjects: " + e.getMessage());
        }
        return subjects;
    }

    public List<Subject> loadAllSubjectsWithPosts()
    { // Loads all subjects and their associated posts from the database. Complexity: Loop. This method demonstrates a composite data structure (List of Subjects, each containing a List of Posts) and populates it.
        List<Subject> subjects = getAllSubjects(); // Retrieves all subjects. This reuses the `getAllSubjects` method, demonstrating code reusability.
        for (Subject subject : subjects)
        { // Loops through each subject. Complexity: Loop. This outer loop processes each subject.
            List<Post> posts = getPostsBySubjectId(subject.getId()); // Retrieves posts for the current subject. This demonstrates Abstraction by calling another method to get related data.
            for (Post post : posts)
            { // Loops through each post. Complexity: Loop. This inner loop processes each post for the current subject.
                subject.addPost(post); // Adds post to the subject. This demonstrates Encapsulation by modifying the state of the Subject object.
            }
        }
        return subjects;
    }


    public Subject getSubjectById(String id)
    { // Retrieves a subject by its ID from the database. Use of external libraries. This method provides a specific lookup functionality, demonstrating Abstraction over direct database access.
        String sql = "SELECT id, name, grade_boundaries, target_mark, average_mark, target_mark_achieved FROM subjects WHERE id = ?"; // SQL query to select a subject by ID.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement. This helps prevent SQL injection.
            pstmt.setString(1, id); // Sets the subject ID parameter.
            ResultSet rs = pstmt.executeQuery(); // Executes query and gets results.
            if (rs.next())
            { // Checks if a result was found.
                return new Subject(rs.getString("id"), rs.getString("name"), rs.getString("grade_boundaries"), rs.getInt("target_mark"), rs.getDouble("average_mark"), rs.getBoolean("target_mark_achieved")); // Creates and returns a new Subject object.
            }
        }
        catch (SQLException e)
        { // Catches SQL exceptions during subject fetching. This handles potential database errors during a single record retrieval.
            System.err.println("Error fetching subject by ID: " + e.getMessage());
        }
        return null; // Returns null if no subject is found or an error occurs.
    }

    public void updateSubjectTargetMark(String subjectId, int targetMark) throws SQLException
    { // Updates the target mark for a subject and recalculates achievement status. Use of external libraries, Loop. This method demonstrates Encapsulation by modifying the subject's state in the database.
        String sql = "UPDATE subjects SET target_mark = ?, target_mark_achieved = ? WHERE id = ?"; // SQL update statement.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setInt(1, targetMark); // Sets the new target mark.
            
            List<Post> posts = getPostsBySubjectId(subjectId); // Retrieves posts for the subject. This demonstrates Abstraction by calling another method.
            double currentAverageMark = 0.0; // Initializes average mark.
            if (posts != null && !posts.isEmpty())
            { // Checks if posts exist.
                double totalPercentage = 0; // Initializes total percentage.
                for (Post post : posts)
                { // Loops through each post to calculate total percentage. Complexity: Loop. This loop performs aggregation.
                    totalPercentage += post.getPercentageMark();
                }
                currentAverageMark = totalPercentage / posts.size(); // Calculates the current average mark.
            }

            boolean achieved = (currentAverageMark >= targetMark); // Determines if target mark is achieved.
            pstmt.setBoolean(2, achieved); // Sets the achieved status.
            pstmt.setString(3, subjectId); // Sets the subject ID.
            pstmt.executeUpdate(); // Executes the update.
            updateSubjectTargetMarkAchievedStatus(subjectId, achieved); // Updates the target mark achieved status. This demonstrates a chained update.
        }
    }

    public void updateSubjectAverageMark(String subjectId, double averageMark) throws SQLException
    { // Updates the average mark for a subject in the database. Use of external libraries. This method provides a direct way to update a specific attribute of a subject.
        String sql = "UPDATE subjects SET average_mark = ? WHERE id = ?"; // SQL update statement.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setDouble(1, averageMark); // Sets the average mark parameter.
            pstmt.setString(2, subjectId); // Sets the subject ID parameter.
            pstmt.executeUpdate(); // Executes the update.
        }
    }

    public void updateSubjectTargetMarkAchievedStatus(String subjectId, boolean targetMarkAchieved) throws SQLException
    { // Updates the target mark achieved status for a subject in the database. Use of external libraries. This method provides a direct way to update a boolean flag for a subject.
        String sql = "UPDATE subjects SET target_mark_achieved = ? WHERE id = ?"; // SQL update statement.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setBoolean(1, targetMarkAchieved); // Sets the target mark achieved status parameter.
            pstmt.setString(2, subjectId); // Sets the subject ID parameter.
            pstmt.executeUpdate(); // Executes the update.
        }
    }

    private String getSubjectGradeBoundaries(String subjectId)
    { // Retrieves grade boundaries for a specific subject from the database. Use of external libraries. This method encapsulates the logic for fetching a specific configuration for a subject.
        String sql = "SELECT grade_boundaries FROM subjects WHERE id = ?"; // SQL query to retrieve grade boundaries.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setString(1, subjectId); // Sets the subject ID parameter.
            ResultSet rs = pstmt.executeQuery(); // Executes query and gets results.
            if (rs.next())
            { // Checks if a result was found.
                return rs.getString("grade_boundaries"); // Returns the grade boundaries as a string.
            }
        }
        catch (SQLException e)
        { // Catches SQL exceptions during grade boundary fetching. This handles potential database errors during data retrieval.
            System.err.println("Error fetching grade boundaries for subject " + subjectId + ": " + e.getMessage());
        }
        return "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}"; // Returns default if not found or an error occurs.
    }

    public List<Post> getPostsBySubjectId(String subjectId)
    { // Retrieves all posts for a given subject ID from the database. Use of external libraries, Loop. This method demonstrates Abstraction by providing a filtered view of posts based on subject.
        List<Post> posts = new ArrayList<>(); // Initializes an empty list to hold Post objects.
        String sql = "SELECT id, title, creation_time, due_date, student_mark, maximum_mark, percentage_mark, grade, comment, description, subject_id FROM posts WHERE subject_id = ?"; // SQL query to select posts by subject ID.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setString(1, subjectId); // Sets the subject ID parameter.
            ResultSet rs = pstmt.executeQuery(); // Executes query and gets results.
            while (rs.next())
            { // Loops through each row in the result set. Complexity: Loop. This iterates over the fetched database records.
                String postId = rs.getString("id");
                String title = rs.getString("title");
                String creationTimeString = rs.getString("creation_time");
                String dueDateString = rs.getString("due_date");
                Double studentMark = rs.getObject("student_mark", Double.class);
                Double maximumMark = rs.getObject("maximum_mark", Double.class);
                double percentageMark = rs.getDouble("percentage_mark");
                int grade = rs.getInt("grade");
                String comment = rs.getString("comment");
                String description = rs.getString("description");
                String retrievedSubjectId = rs.getString("subject_id");

                Post post = new Post(postId, title, creationTimeString, null, studentMark, maximumMark, percentageMark, grade, comment, description, retrievedSubjectId); // Creates a new Post object. This demonstrates Encapsulation by creating objects from database rows.
                if (dueDateString != null)
                { // Checks if due date string is not null.
                    post.setDueDate(LocalDate.parse(dueDateString)); // Parses and sets the due date.
                }
                posts.add(post); // Adds the post to the list.
            }
        }
        catch (SQLException e)
        { // Catches SQL exceptions during post fetching. This handles potential database errors during data retrieval.
            System.err.println("Error fetching posts by subject ID: " + e.getMessage());
        }
        return posts;
    }

    public void updatePostMarkAndComment(String postId, Double mark, String comment) throws SQLException
    { // Updates the student mark and comment for a specific post in the database. Use of external libraries. This method demonstrates Encapsulation by providing a controlled way to modify post data.
        String sql = "UPDATE posts SET student_mark = ?, comment = ? WHERE id = ?"; // SQL update statement.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            if (mark != null)
            { // Checks if mark is not null.
                pstmt.setDouble(1, mark); // Sets the student mark parameter.
            }
            else
            { // If mark is null.
                pstmt.setNull(1, Types.REAL); // Sets the student mark to null.
            }
            pstmt.setString(2, comment); // Sets the comment parameter.
            pstmt.setString(3, postId); // Sets the post ID parameter.
            pstmt.executeUpdate(); // Executes the update.
        }
    }

    public void deletePost(String postId) throws SQLException
    { // Deletes a post from the database by its ID. Use of external libraries. This method provides a direct way to remove a post, demonstrating Abstraction over the SQL DELETE command.
        String sql = "DELETE FROM posts WHERE id = ?"; // SQL delete statement.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(sql))
        { // Prepares a statement.
            pstmt.setString(1, postId); // Sets the post ID parameter.
            pstmt.executeUpdate(); // Executes the delete.
        }
    }

    public void deleteSubject(String subjectId) throws SQLException
    { // Deletes a subject and all its associated posts from the database. Use of external libraries. This method demonstrates a cascading delete operation, ensuring data integrity.
        String deletePostsSql = "DELETE FROM posts WHERE subject_id = ?"; // SQL statement to delete posts related to the subject.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(deletePostsSql))
        { // Prepares a statement for deleting posts.
            pstmt.setString(1, subjectId); // Sets the subject ID parameter.
            pstmt.executeUpdate(); // Executes the delete for posts.
        }
 
        String deleteSubjectSql = "DELETE FROM subjects WHERE id = ?"; // SQL statement to delete the subject itself.
        try (Connection conn = connect(); // Establishes connection.
             PreparedStatement pstmt = conn.prepareStatement(deleteSubjectSql))
        { // Prepares a statement for deleting the subject.
            pstmt.setString(1, subjectId); // Sets the subject ID parameter.
            pstmt.executeUpdate(); // Executes the delete for the subject.
        }
    }

}