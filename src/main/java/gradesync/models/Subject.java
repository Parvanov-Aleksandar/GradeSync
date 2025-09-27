package gradesync.models;

import java.util.ArrayList;
import java.util.List;

public class Subject
{
    private String id;
    private String name;
    private String gradeBoundaries;
    private int targetMark;
    private double averageMark;
    private boolean isTargetMarkCurrentlyAchieved;
    private boolean alreadyAchieved;
    private List<Post> posts;

    public Subject(String id, String name)
    { // Constructor for Subject with default grade boundaries, target mark, average mark, and achieved status.
        this(id, name, "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}", 85, 0.0, false, false);
    }

    public Subject(String id, String name, String gradeBoundaries)
    { // Constructor for Subject with specified grade boundaries and default target mark, average mark, and achieved status.
        this(id, name, gradeBoundaries, 85, 0.0, false, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark)
    { // Constructor for Subject with specified grade boundaries and target mark, and default average mark and achieved status.
        this(id, name, gradeBoundaries, targetMark, 0.0, false, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark, double averageMark)
    { // Constructor for Subject with specified grade boundaries, target mark, and average mark, and default achieved status.
        this(id, name, gradeBoundaries, targetMark, averageMark, false, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark, double averageMark, boolean isTargetMarkCurrentlyAchieved, boolean alreadyAchieved)
    { // Full constructor for Subject. Complexity: Implementation of abstract data types.
        this.id = id;
        this.name = name;
        this.gradeBoundaries = gradeBoundaries;
        this.targetMark = targetMark;
        this.averageMark = averageMark;
        this.isTargetMarkCurrentlyAchieved = isTargetMarkCurrentlyAchieved;
        this.alreadyAchieved = alreadyAchieved;
        this.posts = new ArrayList<>();
    }

    public String getId()
    { // Returns the ID of the subject
        return id;
    }

    public String getName()
    { // Returns the name of the subject
        return name;
    }

    public List<Post> getPosts()
    { // Returns the list of posts associated with the subject
        return posts;
    }

    public String getGradeBoundaries()
    { // Returns the grade boundaries as a JSON string
        return gradeBoundaries;
    }

    public int getTargetMark()
    { // Returns the target mark for the subject
        return targetMark;
    }

    public void setTargetMark(int targetMark)
    { // Sets the target mark for the subject
        this.targetMark = targetMark;
    }

    public double getAverageMark()
    { // Returns the stored average mark for the subject
        return averageMark;
    }

    public void setAverageMark(double averageMark)
    { // Sets the average mark for the subject.
        this.averageMark = averageMark;
    }

    public boolean isTargetMarkCurrentlyAchieved()
    { // Checks if the target mark has been achieved.
        return averageMark >= targetMark;
    }

    public void setTargetMarkCurrentlyAchieved(boolean isTargetMarkCurrentlyAchieved)
    { // Sets the target mark achieved status.
        this.isTargetMarkCurrentlyAchieved = isTargetMarkCurrentlyAchieved;
    }

    public boolean isAlreadyAchieved() {
        return alreadyAchieved;
    }

    public void setAlreadyAchieved(boolean alreadyAchieved) {
        this.alreadyAchieved = alreadyAchieved;
    }

    public void addPost(Post post)
    { // Adds a post to the subject
        this.posts.add(post); // Adds the post to the list
    }

    @Override
    public String toString() // Returns a string description of the Subject object
    {
        return "Subject{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", postsCount=" + posts.size() +
               '}';
    }
}