package gradesync.models;

import java.util.ArrayList;
import java.util.List;

public class Subject {
    private String id; // Google Classroom Course ID
    private String name;
    private String gradeBoundaries; // New field for grade boundaries
    private List<Post> posts;

    public Subject(String id, String name) {
        this(id, name, "{\"7\": 85, \"6\": 70, \"5\": 60, \"4\": 40, \"3\": 25, \"2\": 0, \"1\": 0, \"0\": 0}"); // Default boundaries
    }

    public Subject(String id, String name, String gradeBoundaries) {
        this.id = id;
        this.name = name;
        this.gradeBoundaries = gradeBoundaries;
        this.posts = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public String getGradeBoundaries() {
        return gradeBoundaries;
    }

    public void addPost(Post post) {
        this.posts.add(post);
    }

    @Override
    public String toString() {
        return "Subject{" +
               "id='" + id + '\'' +
               ", name='" + name + '\'' +
               ", postsCount=" + posts.size() +
               '}';
    }
}