package gradesync.models;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime; // Add this import
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.util.DateTime; // Still need this import as the parameter type

public class Post {
    private String id;
    private String title;
    private LocalDate creationDate; // Time when the coursework was created
    private LocalDate dueDate;          // Due date of the coursework

    private Double studentMark;
    private Double maximumMark; // New field for maximum possible mark
    private double percentageMark;
    private int grade;
    private String comment; // For future use, will be empty for now
    private String description; // New field for post description
    private String subjectId;

    // Constructor now takes the raw API DateTime and Date objects
    public Post(String id, String title, String creationTimeString, com.google.api.services.classroom.model.Date dueDateApi, Double studentMark, Double maximumMark, double percentageMark, int grade, String comment, String description, String subjectId) {
        this.id = id;
        this.title = title;
        this.comment = comment;
        this.description = description; // Initialize description
        this.studentMark = studentMark;
        this.maximumMark = maximumMark;
        this.percentageMark = percentageMark;
        this.grade = grade;
        this.subjectId = subjectId;

        // Handle creation time from API's DateTime object by parsing its string representation
        if (creationTimeString != null && !creationTimeString.isEmpty()) {
            try {
                // Attempt to parse as yyyy-MM-dd
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                this.creationDate = LocalDate.parse(creationTimeString.substring(0, 10), formatter);
            } catch (DateTimeParseException e) {
                System.err.println("Warning: Could not parse creation time string for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.creationDate = null;
            }
        } else {
            this.creationDate = null;
        }

        // --- Handle due date from API's Date object (different type, has getYear, getMonth, getDay) ---
        if (dueDateApi != null && dueDateApi.getYear() != null && dueDateApi.getMonth() != null && dueDateApi.getDay() != null) {
            try {
                this.dueDate = LocalDate.of(dueDateApi.getYear(), dueDateApi.getMonth(), dueDateApi.getDay());
            } catch (Exception e) {
                System.err.println("Warning: Could not parse due date for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.dueDate = null;
            }
        } else {
            this.dueDate = null;
        }
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public LocalDate getCreationDate() { return creationDate; }
    public String getCreationDateString() {
        if (creationDate != null) {
            return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        return "N/A";
    }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Double getStudentMark() { return studentMark; }
    public void setStudentMark(Double studentMark) { this.studentMark = studentMark; }
    public Double getMaximumMark() { return maximumMark; }
    public void setMaximumMark(Double maximumMark) { this.maximumMark = maximumMark; }
    public double getPercentageMark() { return percentageMark; }
    public void setPercentageMark(double percentageMark) { this.percentageMark = percentageMark; }
    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSubjectId() { return subjectId; }
    public void setSubjectId(String subjectId) { this.subjectId = subjectId; }

    public void calculatePercentageMark() {
        if (maximumMark != null && maximumMark > 0) {
            this.percentageMark = (studentMark != null ? (studentMark / maximumMark) * 100 : 0.0);
        } else {
            this.percentageMark = 0.0; // Handle division by zero or null maximumMark
        }
    }

    public void calculateGrade(String gradeBoundariesJson) {
        // Default grade boundaries if JSON is null or empty
        String defaultBoundaries = "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}";
        String jsonToParse = (gradeBoundariesJson != null && !gradeBoundariesJson.isEmpty()) ? gradeBoundariesJson : defaultBoundaries;

        try {
            // Using a simple parsing approach for the JSON string
            // This assumes the JSON is always in the format {"grade": percentage, ...}
            // For a more robust solution, a JSON library like Gson or Jackson would be used.
            jsonToParse = jsonToParse.substring(1, jsonToParse.length() - 1); // Remove curly braces
            String[] pairs = jsonToParse.split(", ");
            
            // Sort boundaries in descending order of percentage for correct grade assignment
            List<GradeBoundary> boundaries = new ArrayList<>();
            for (String pair : pairs) {
                String[] keyValue = pair.split(": ");
                int grade = Integer.parseInt(keyValue[0].replace("\"", ""));
                double minPercentage = Double.parseDouble(keyValue[1]);
                boundaries.add(new GradeBoundary(grade, minPercentage));
            }
            
            boundaries.sort((b1, b2) -> Double.compare(b2.minPercentage, b1.minPercentage));

            this.grade = 0; // Default to 0 if no grade matches
            for (GradeBoundary boundary : boundaries) {
                if (this.percentageMark >= boundary.minPercentage) {
                    this.grade = boundary.grade;
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing grade boundaries or calculating grade: " + e.getMessage());
            this.grade = 0; // Fallback grade in case of error
        }
    }

    // Helper class for grade boundaries
    private static class GradeBoundary {
        int grade;
        double minPercentage;

        GradeBoundary(int grade, double minPercentage) {
            this.grade = grade;
            this.minPercentage = minPercentage;
        }
    }

    @Override
    public String toString() {
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