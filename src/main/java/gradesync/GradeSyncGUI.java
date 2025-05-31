package gradesync;

import gradesync.AuthManager;
import gradesync.Database;
import gradesync.models.Subject;
import gradesync.models.Post;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class GradeSyncGUI extends JFrame {

    private JPanel mainPanel;
    private JPanel subjectListPanel;
    private JPanel postListPanel;

    private JLabel accountNameLabel;
    private JButton changeAccountButton;

    private Database database;
    private AuthManager authManager;

    private Subject currentSubject;

    public GradeSyncGUI() {
        setTitle("GradeSync");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the window

        database = new Database();
        authManager = new AuthManager();

        // Initialize main panel with CardLayout for switching views
        mainPanel = new JPanel(new CardLayout());
        add(mainPanel);

        // Initialize and add Subject List View
        createSubjectListView();
        mainPanel.add(subjectListPanel, "SubjectListView");

        // Initialize Post List View (will be populated when a subject is selected)
        postListPanel = new JPanel(new BorderLayout());
        mainPanel.add(postListPanel, "PostListView");

        // Display the initial view
        showSubjectListView();
    }

    private void createSubjectListView() {
        subjectListPanel = new JPanel(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        accountNameLabel = new JLabel("Account: " + authManager.getCurrentUserName());
        accountNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(accountNameLabel, BorderLayout.WEST);

        changeAccountButton = new JButton("Change Account"); // Placeholder for dropdown icon
        changeAccountButton.addActionListener(e -> JOptionPane.showMessageDialog(this, "Change Account functionality not implemented."));
        headerPanel.add(changeAccountButton, BorderLayout.EAST);
        subjectListPanel.add(headerPanel, BorderLayout.NORTH);

        // Subject List
        JPanel subjectsPanel = new JPanel();
        subjectsPanel.setLayout(new BoxLayout(subjectsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(subjectsPanel);
        subjectListPanel.add(scrollPane, BorderLayout.CENTER);

        List<Subject> subjects = database.getAllSubjects(); // Assuming this method exists or will be added
        if (subjects != null && !subjects.isEmpty()) {
            for (Subject subject : subjects) {
                JButton subjectButton = new JButton(subject.getName());
                subjectButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                subjectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, subjectButton.getMinimumSize().height)); // Make button fill width
                subjectButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        currentSubject = subject;
                        showPostListView(subject.getId());
                    }
                });
                subjectsPanel.add(subjectButton);
                subjectsPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacing between buttons
            }
        } else {
            subjectsPanel.add(new JLabel("No subjects found."));
        }
    }

    private void showSubjectListView() {
        CardLayout cl = (CardLayout) (mainPanel.getLayout());
        cl.show(mainPanel, "SubjectListView");
    }

    private void showPostListView(String subjectId) {
        postListPanel.removeAll(); // Clear previous content

        // Header for Post List View
        JPanel postHeaderPanel = new JPanel(new BorderLayout());
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> showSubjectListView());
        postHeaderPanel.add(backButton, BorderLayout.WEST);

        JLabel subjectTitleLabel = new JLabel("Posts for: " + currentSubject.getName());
        subjectTitleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        postHeaderPanel.add(subjectTitleLabel, BorderLayout.CENTER);
        postListPanel.add(postHeaderPanel, BorderLayout.NORTH);

        // Post List
        JPanel postsPanel = new JPanel();
        postsPanel.setLayout(new BoxLayout(postsPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(postsPanel);
        postListPanel.add(scrollPane, BorderLayout.CENTER);

        List<Post> posts = database.getPostsBySubjectId(subjectId); // Assuming this method exists or will be added
        if (posts != null && !posts.isEmpty()) {
            for (Post post : posts) {
                JButton postButton = new JButton(post.getTitle());
                postButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                postButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, postButton.getMinimumSize().height));
                postButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showPostDetailDialog(post);
                    }
                });
                postsPanel.add(postButton);
                postsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            }
        } else {
            postsPanel.add(new JLabel("No posts found for this subject."));
        }

        postListPanel.revalidate();
        postListPanel.repaint();

        CardLayout cl = (CardLayout) (mainPanel.getLayout());
        cl.show(mainPanel, "PostListView");
    }

    private void showPostDetailDialog(Post post) {
        JDialog postDetailDialog = new JDialog(this, "Post Details", true);
        postDetailDialog.setSize(400, 300);
        postDetailDialog.setLocationRelativeTo(this);

        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title and Date on the same line
        JPanel titleDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel titleLabel = new JLabel("Title: " + post.getTitle());
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleDatePanel.add(titleLabel);
        
        JLabel dateLabel = new JLabel(" (Date: " + (post.getCreationDate() != null ? post.getCreationDate().toString() : "N/A") + ")");
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        titleDatePanel.add(dateLabel);
        detailPanel.add(titleDatePanel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // Description
        JLabel descriptionLabel = new JLabel("Description:");
        descriptionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        detailPanel.add(descriptionLabel);
        JTextArea descriptionArea = new JTextArea(post.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setPreferredSize(new Dimension(350, 80)); // Set preferred size for description area
        detailPanel.add(descriptionScrollPane);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // Marks, Percentage, Grade
        JLabel studentMaxMarksLabel = new JLabel("Marks: " + (post.getStudentMark() != null ? String.format("%.2f", post.getStudentMark()) : "N/A") +
                                                 " / " + (post.getMaximumMark() != null ? String.format("%.2f", post.getMaximumMark()) : "N/A"));
        studentMaxMarksLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailPanel.add(studentMaxMarksLabel);

        JLabel percentageLabel = new JLabel("Percentage: " + String.format("%.2f", post.getPercentageMark()) + "%");
        percentageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailPanel.add(percentageLabel);

        JLabel gradeLabel = new JLabel("Grade: " + post.getGrade());
        gradeLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        detailPanel.add(gradeLabel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spacing

        // Comment (if any)
        if (post.getComment() != null && !post.getComment().isEmpty()) {
            JLabel commentLabel = new JLabel("Comment:");
            commentLabel.setFont(new Font("Arial", Font.BOLD, 12));
            detailPanel.add(commentLabel);
            JTextArea commentArea = new JTextArea(post.getComment());
            commentArea.setEditable(false);
            commentArea.setLineWrap(true);
            commentArea.setWrapStyleWord(true);
            JScrollPane commentScrollPane = new JScrollPane(commentArea);
            commentScrollPane.setPreferredSize(new Dimension(350, 50)); // Set preferred size for comment area
            detailPanel.add(commentScrollPane);
        }

        postDetailDialog.add(detailPanel);
        postDetailDialog.setVisible(true);
    }
}