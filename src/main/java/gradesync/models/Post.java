package gradesync.models; // <--- CORRECTED PACKAGE NAME

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.google.api.client.util.DateTime; // Still need this import as the parameter type

public class Post {
    private String id;
    private String title;
    private LocalDateTime creationTime; // Time when the coursework was created
    private LocalDate dueDate;          // Due date of the coursework
    private Double studentMark;
    private String comment; // For future use, will be empty for now

    // Constructor now takes the raw API DateTime and Date objects
    public Post(String id, String title, String timeString, com.google.api.services.classroom.model.Date dueDateApi) {
        this.id = id;
        this.title = title;
        this.comment = ""; // Initialize comment as empty

        // --- Handle creation time from API's DateTime object by parsing its string representation ---
        if (timeString != null) {
            try {
                // Get the string representation of the DateTime object (e.g., "2023-10-27T10:00:00.000Z")
                if (!timeString.isEmpty()) {
                    // Parse the ISO 8601 string to an Instant, then convert to LocalDateTime in system's default time zone
                    this.creationTime = LocalDateTime.ofInstant(Instant.parse(timeString), ZoneId.systemDefault());
                } else {
                    this.creationTime = null;
                }
            } catch (DateTimeParseException | NullPointerException | IllegalArgumentException e) {
                System.err.println("Warning: Could not parse creation time string for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.creationTime = null;
            }
        } else {
            this.creationTime = null;
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
    public LocalDateTime getCreationTime() { return creationTime; }
    public LocalDate getDueDate() { return dueDate; }
    public Double getStudentMark() { return studentMark; }
    public void setStudentMark(Double studentMark) { this.studentMark = studentMark; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    @Override
    public String toString() {
        return "  Post{" +
               "id='" + id + '\'' +
               ", title='" + title + '\'' +
               ", created=" + (creationTime != null ? creationTime.toLocalDate() : "N/A") +
               ", dueDate=" + (dueDate != null ? dueDate : "N/A") +
               ", studentMark=" + (studentMark != null ? studentMark : "N/A") +
               '}';
    }
}