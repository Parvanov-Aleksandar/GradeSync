package gradesync.models; // <--- CORRECTED PACKAGE NAME

import java.util.ArrayList;
import java.util.List;

public class Subject {
    private String id; // Google Classroom Course ID
    private String name;
    private List<Post> posts;

    public Subject(String id, String name) {
        this.id = id;
        this.name = name;
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