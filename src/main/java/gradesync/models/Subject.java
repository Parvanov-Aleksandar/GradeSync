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
    private boolean targetMarkAchieved;
    private List<Post> posts;

    public Subject(String id, String name)
    { // Constructor for Subject with default grade boundaries, target mark, average mark, and achieved status.
        this(id, name, "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}", 85, 0.0, false);
    }

    public Subject(String id, String name, String gradeBoundaries)
    { // Constructor for Subject with specified grade boundaries and default target mark, average mark, and achieved status.
        this(id, name, gradeBoundaries, 85, 0.0, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark)
    { // Constructor for Subject with specified grade boundaries and target mark, and default average mark and achieved status.
        this(id, name, gradeBoundaries, targetMark, 0.0, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark, double averageMark)
    { // Constructor for Subject with specified grade boundaries, target mark, and average mark, and default achieved status.
        this(id, name, gradeBoundaries, targetMark, averageMark, false);
    }

    public Subject(String id, String name, String gradeBoundaries, int targetMark, double averageMark, boolean targetMarkAchieved)
    { // Full constructor for Subject. Complexity: Implementation of abstract data types.
        this.id = id;
        this.name = name;
        this.gradeBoundaries = gradeBoundaries;
        this.targetMark = targetMark;
        this.averageMark = averageMark;
        this.targetMarkAchieved = targetMarkAchieved;
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
    { // Calculates and returns the average mark for the subject based on its posts
        if (posts.isEmpty()){ // Checks if there are no posts
            return 0.0;
        }
        double totalPercentage = 0;
        for (Post post : posts)
        { // Iterates through each post to sum up percentage marks
            totalPercentage += post.getPercentageMark();
        }
        return totalPercentage / posts.size(); // Returns the average percentage
    }

    public void setAverageMark(double averageMark)
    { // Sets the average mark for the subject.
        this.averageMark = averageMark;
    }

    public boolean isTargetMarkAchieved()
    { // Checks if the target mark has been achieved.
        return averageMark >= targetMark;
    }

    public void setTargetMarkAchieved(boolean targetMarkAchieved)
    { // Sets the target mark achieved status.
        this.targetMarkAchieved = targetMarkAchieved;
    }

    public void addPost(Post post)
    { // Adds a post to the subject and recalculates average and target mark achievement
        this.posts.add(post); // Adds the post to the list
        this.averageMark = getAverageMark(); // Recalculates average mark
        this.targetMarkAchieved = isTargetMarkAchieved(); // Recalculates target mark achieved status
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