package gradesync.models;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class Post
{
    // Encapsulation
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

    public Post(String id, String title, String creationTimeString, com.google.api.services.classroom.model.Date dueDateApi,
                Double studentMark, Double maximumMark, double percentageMark, int grade, String comment, String description, String subjectId)
    { // Constructor for the Post class. Complexity: Parsing a text file or other data stream.
        this.id = id;
        this.title = title;
        this.comment = comment;
        this.description = description;
        this.studentMark = studentMark;
        this.maximumMark = maximumMark;
        this.percentageMark = percentageMark;
        this.grade = grade;
        this.subjectId = subjectId;

        if (creationTimeString != null && !creationTimeString.isEmpty()) // Checks if creation time string is valid
        {
            try
            {
                this.creationDate = LocalDate.parse(creationTimeString.substring(0, 10)); // Parses the creation date string
            }
            catch (DateTimeParseException e) // Catches parsing errors
            {
                System.err.println("Warning: Could not parse creation time string for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.creationDate = null;
            }
        }
        else // If creation time string is null or empty
        {
            this.creationDate = null;
        }

        if (dueDateApi != null && dueDateApi.getYear() != null && dueDateApi.getMonth() != null && dueDateApi.getDay() != null) // Checks if due date API object is valid
        {
            try // Attempts to create a LocalDate object from the due date API object
            {
                this.dueDate = LocalDate.of(dueDateApi.getYear(), dueDateApi.getMonth(), dueDateApi.getDay());
            }
            catch (Exception e) // Catches any exceptions during due date parsing
            {
                System.err.println("Warning: Could not parse due date for '" + title + "'. Setting to null. Error: " + e.getMessage());
                this.dueDate = null;
            }
        }
        else // If due date API object is invalid
        {
            this.dueDate = null;
        }
    }

    // Accessors and Mutators
    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public LocalDate getCreationDate()
    {
        return creationDate;
    }

    public String getCreationDateString()
    {
        if (creationDate != null)
        {
            return creationDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")); // Formats the date
        }
        return "N/A";
    }

    public LocalDate getDueDate()
    {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate)
    {
        this.dueDate = dueDate;
    }

    public Double getStudentMark()
    {
        return studentMark;
    }

    public void setStudentMark(Double studentMark)
    {
        this.studentMark = studentMark;
    }

    public Double getMaximumMark()
    {
        return maximumMark;
    }

    public void setMaximumMark(Double maximumMark)
    {
        this.maximumMark = maximumMark;
    }
    
    public double getPercentageMark()
    {
        return percentageMark;
    }

    public void setPercentageMark(double percentageMark)
    {
        this.percentageMark = percentageMark;
    }

    public int getGrade()
    {
        return grade;
    }

    public void setGrade(int grade)
    {
        this.grade = grade;
    }

    public String getComment()
    {
        return comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getSubjectId()
    {
        return subjectId;
    }

    public void setSubjectId(String subjectId)
    {
        this.subjectId = subjectId;
    }

    public void calculatePercentageMark() // Calculates the percentage mark based on student and maximum marks
    {
        if (maximumMark != null && maximumMark > 0) // Checks if maximum mark is valid
        {
            this.percentageMark = (studentMark != null ? (studentMark / maximumMark) * 100 : 0.0); // Calculates percentage
        }
        else // Handles cases where maximum mark is invalid
        {
            this.percentageMark = 0.0;
        }
    }

    public void calculateGrade(String gradeBoundariesJson) // Calculates the grade based on percentage mark and grade boundaries
    {
        String defaultBoundaries = "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}"; // Default grade boundaries
        String jsonToParse = (gradeBoundariesJson != null && !gradeBoundariesJson.isEmpty()) ? gradeBoundariesJson : defaultBoundaries; // Uses provided or default boundaries

        try
        {
            jsonToParse = jsonToParse.substring(1, jsonToParse.length() - 1); // Removes curly brackets from JSON string
            String[] pairs = jsonToParse.split(", "); // Splits JSON into key-value pairs
            
            ArrayList<GradeBoundary> boundaries = new ArrayList<>(); // ArrayList to store grade boundaries
            for (String pair : pairs)
            { // Iterates through each key-value pair
                String[] keyValue = pair.split(": "); // Splits into key and value
                int grade = Integer.parseInt(keyValue[0].replace("\"", "")); // Parses grade
                double minPercentage = Double.parseDouble(keyValue[1]); // Parses minimum percentage
                boundaries.add(new GradeBoundary(grade, minPercentage)); // Adds to boundaries list
            }
            
            boundaries.sort((b1, b2) -> Double.compare(b2.minPercentage, b1.minPercentage)); // Sorts boundaries in descending order

            this.grade = 0; // Default grade
            for (GradeBoundary boundary : boundaries) // Iterates through sorted boundaries
            {
                if (this.percentageMark >= boundary.minPercentage) // Checks if percentage mark meets boundary
                {
                    this.grade = boundary.grade; // Assigns grade
                    break; // Exits loop once grade is found
                }
            }
        }
        catch (Exception e) // Catches any exceptions during grade calculation
        {
            System.err.println("Error parsing grade boundaries or calculating grade: " + e.getMessage());
            this.grade = 0; // Fallback grade in case of error
        }
    }

    private static class GradeBoundary // Helper class to represent a grade boundary. Complexity: Implementation of abstract data types
    {
        int grade;
        double minPercentage;

        GradeBoundary(int grade, double minPercentage)
        {
            this.grade = grade;
            this.minPercentage = minPercentage;
        }
    }

    @Override
    public String toString() // Returns a string description of the Post object
    {
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