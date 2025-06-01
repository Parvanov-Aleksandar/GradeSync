package gradesync.models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class Post { // Encapsulation
    private String id;
    private String title;
    private LocalDate creationDate;
    private LocalDate dueDate;
    private Double studentMark;
    private Double maximumMark;
    private double percentageMark;
    private int grade;
    private String comment;
    private String description;
    private String subjectId;

    public Post(String id, String title, String creationTimeString, com.google.api.services.classroom.model.Date dueDateApi, Double studentMark, Double maximumMark, double percentageMark, int grade, String comment, String description, String subjectId) { // Constructor for the Post class. Complexity: Parsing a text file or other data stream.
        this.id = id;
        this.title = title;
        this.comment = comment;
        this.description = description;
        this.studentMark = studentMark;
        this.maximumMark = maximumMark;
        this.percentageMark = percentageMark;
        this.grade = grade;
        this.subjectId = subjectId;

        if (creationTimeString != null && !creationTimeString.isEmpty()) { // Checks if creation time string is valid.
            try { // Attempts to parse the creation date string.
                this.creationDate = LocalDate.parse(creationTimeString.substring(0, 10));
            } catch (DateTimeParseException e) { // Catches parsing errors.
                System.err.println("Warning: Could not parse creation time string for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.creationDate = null;
            }
        } else { // If creation time string is null or empty.
            this.creationDate = null;
        }

        if (dueDateApi != null && dueDateApi.getYear() != null && dueDateApi.getMonth() != null && dueDateApi.getDay() != null) { // Checks if due date API object is valid.
            try { // Attempts to create a LocalDate object from the due date API object.
                this.dueDate = LocalDate.of(dueDateApi.getYear(), dueDateApi.getMonth(), dueDateApi.getDay());
            } catch (Exception e) { // Catches any exceptions during due date parsing.
                System.err.println("Warning: Could not parse due date for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.dueDate = null;
            }
        } else { // If due date API object is invalid.
            this.dueDate = null;
        }
    }

    public String getId() { return id; } // Returns the ID of the post.
    public String getTitle() { return title; } // Returns the title of the post.
    public LocalDate getCreationDate() { return creationDate; } // Returns the creation date of the post.
    public String getCreationDateString() { // Returns the creation date as a formatted string.
        if (creationDate != null) { // Checks if creation date is not null.
            return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Formats the date.
        }
        return "N/A";
    }
    public LocalDate getDueDate() { return dueDate; } // Returns the due date of the post.
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; } // Sets the due date of the post.
    public Double getStudentMark() { return studentMark; } // Returns the student's mark for the post.
    public void setStudentMark(Double studentMark) { this.studentMark = studentMark; } // Sets the student's mark for the post.
    public Double getMaximumMark() { return maximumMark; } // Returns the maximum possible mark for the post.
    public void setMaximumMark(Double maximumMark) { this.maximumMark = maximumMark; } // Sets the maximum possible mark for the post.
    public double getPercentageMark() { return percentageMark; } // Returns the percentage mark for the post.
    public void setPercentageMark(double percentageMark) { this.percentageMark = percentageMark; } // Sets the percentage mark for the post.
    public int getGrade() { return grade; } // Returns the grade for the post.
    public void setGrade(int grade) { this.grade = grade; } // Sets the grade for the post.
    public String getComment() { return comment; } // Returns the comment for the post.
    public void setComment(String comment) { this.comment = comment; } // Sets the comment for the post.
    public String getDescription() { return description; } // Returns the description for the post.
    public void setDescription(String description) { this.description = description; } // Sets the description for the post.
    public String getSubjectId() { return subjectId; } // Returns the subject ID of the post.
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; } // Sets the subject ID of the post.

    public void calculatePercentageMark() { // Calculates the percentage mark based on student and maximum marks.
        if (maximumMark != null && maximumMark > 0) { // Checks if maximum mark is valid.
            this.percentageMark = (studentMark != null ? (studentMark / maximumMark) * 100 : 0.0); // Calculates percentage.
        } else { // Handles cases where maximum mark is invalid.
            this.percentageMark = 0.0;
        }
    }

    public void calculateGrade(String gradeBoundariesJson) { // Calculates the grade based on percentage mark and grade boundaries. Complexity: Parsing a text file or other data stream, Loop.
        String defaultBoundaries = "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}"; // Default grade boundaries.
        String jsonToParse = (gradeBoundariesJson != null && !gradeBoundariesJson.isEmpty()) ? gradeBoundariesJson : defaultBoundaries; // Uses provided or default boundaries.

        try { // Attempts to parse grade boundaries and calculate grade.
            jsonToParse = jsonToParse.substring(1, jsonToParse.length() - 1); // Removes curly braces from JSON string.
            String[] pairs = jsonToParse.split(", "); // Splits JSON into key-value pairs.
            
            List<GradeBoundary> boundaries = new ArrayList<>(); // List to store grade boundaries.
            for (String pair : pairs) { // Iterates through each key-value pair. Complexity: Loop.
                String[] keyValue = pair.split(": "); // Splits into key and value.
                int grade = Integer.parseInt(keyValue[0].replace("\"", "")); // Parses grade.
                double minPercentage = Double.parseDouble(keyValue[1]); // Parses minimum percentage.
                boundaries.add(new GradeBoundary(grade, minPercentage)); // Adds to boundaries list.
            }
            
            boundaries.sort((b1, b2) -> Double.compare(b2.minPercentage, b1.minPercentage)); // Sorts boundaries in descending order.

            this.grade = 0; // Default grade.
            for (GradeBoundary boundary : boundaries) { // Iterates through sorted boundaries. Complexity: Loop.
                if (this.percentageMark >= boundary.minPercentage) { // Checks if percentage mark meets boundary.
                    this.grade = boundary.grade; // Assigns grade.
                    break; // Exits loop once grade is found.
                }
            }
        } catch (Exception e) { // Catches any exceptions during grade calculation.
            System.err.println("Error parsing grade boundaries or calculating grade: " + e.getMessage());
            this.grade = 0; // Fallback grade in case of error.
        }
    }

    private static class GradeBoundary { // Helper class to represent a grade boundary. Complexity: Implementation of abstract data types.
        int grade;
        double minPercentage;

        GradeBoundary(int grade, double minPercentage) {
            this.grade = grade;
            this.minPercentage = minPercentage;
        }
    }

    @Override
    public String toString() { // Returns a string representation of the Post object.
        return "  Post{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", created=" + (creationDate != null ? creationDate : "N/A") +
               ", dueDate=" + (dueDate != null ? dueDate : "N/A") +
               ", studentMark=" + (studentMark != null ? studentMark : "N/A") +
               ", maximumMark=" + (maximumMark != null ? maximumMark : "N/A") +
               ", percentageMark=" + String.format("%.2f", percentageMark) + "%" +
               ", grade=" + grade +
               ", description='" + description + '\'' +
               '}';
   }
}