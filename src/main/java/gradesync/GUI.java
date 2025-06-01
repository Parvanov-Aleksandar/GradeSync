package gradesync;

import gradesync.models.Subject;
import gradesync.models.Post;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class GUI extends JFrame { // Encapsulation
    private JPanel mainPanel;
    private JPanel subjectListPanel;
    private JPanel postListPanel;
    private Database database;
    private Subject currentSubject;

    public GUI() { // Initializes the GUI frame and components. Complexity: Use of additional libraries.
        setTitle("GradeSync");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        database = new Database(); // Initializes the database instance.
 
        mainPanel = new JPanel(new CardLayout()); // Sets up the main panel with CardLayout.
        add(mainPanel);
 
        createSubjectListView(); // Creates the subject list view.
        mainPanel.add(subjectListPanel, "SubjectListView"); // Adds the subject list view to the main panel.
 
        postListPanel = new JPanel(new BorderLayout()); // Initializes the post list panel.
        mainPanel.add(postListPanel, "PostListView"); // Adds the post list view to the main panel.
 
        showSubjectListView(); // Displays the subject list view.
    }

    private void createSubjectListView() { // Creates and populates the subject list view. Complexity: Use of additional libraries, Loop.
        subjectListPanel = new JPanel(new BorderLayout()); // Initializes the subject list panel.
 
        JPanel headerPanel = new JPanel(new BorderLayout()); // Creates a header panel.
        subjectListPanel.add(headerPanel, BorderLayout.NORTH); // Adds header to subject list panel.
 
        JPanel subjectsPanel = new JPanel(); // Creates a panel for subject buttons.
        subjectsPanel.setLayout(new BoxLayout(subjectsPanel, BoxLayout.Y_AXIS)); // Sets layout for vertical arrangement.
        JScrollPane scrollPane = new JScrollPane(subjectsPanel); // Adds scrollability to the subjects panel.
        subjectListPanel.add(scrollPane, BorderLayout.CENTER); // Adds scroll pane to subject list panel.
 
        List<Subject> subjects = database.getAllSubjects(); // Retrieves all subjects from the database.
        if (subjects != null && !subjects.isEmpty()) { // Checks if subjects exist.
            for (Subject subject : subjects) { // Iterates through each subject to create a button. Complexity: Loop.
                JButton subjectButton = new JButton(subject.getName()); // Creates a button for the subject.
                subjectButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Centers the button horizontally.
                subjectButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, subjectButton.getMinimumSize().height)); // Makes button fill width.
                subjectButton.addActionListener(new ActionListener() { // Adds an action listener to the button.
                    @Override
                    public void actionPerformed(ActionEvent e) { // Handles button click event.
                        currentSubject = subject; // Sets the current subject.
                        showPostListView(subject.getId()); // Displays the post list view for the selected subject.
                    }
                });
                subjectsPanel.add(subjectButton); // Adds the button to the subjects panel.
                subjectsPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Adds spacing between buttons.
            }
        } else { // If no subjects are found.
            subjectsPanel.add(new JLabel("No subjects found.")); // Displays a message.
        }
    }

    private void showSubjectListView() { // Displays the subject list view using CardLayout. Complexity: Use of additional libraries.
        CardLayout cl = (CardLayout) (mainPanel.getLayout()); // Gets the CardLayout from the main panel.
        cl.show(mainPanel, "SubjectListView"); // Shows the "SubjectListView" card.
    }


    private void showPostListView(String subjectId) { // Displays the post list view for a selected subject. Complexity: Use of additional libraries, Loop.
        postListPanel.removeAll(); // Clears previous content from the panel.
 
        currentSubject = database.getSubjectById(subjectId); // Retrieves the subject from the database.
        if (currentSubject == null) { // Checks if the subject was found.
            JOptionPane.showMessageDialog(this, "Subject not found.", "Error", JOptionPane.ERROR_MESSAGE); // Displays an error message.
            showSubjectListView(); // Returns to the subject list view.
            return;
        }
        List<Post> postsForSubject = database.getPostsBySubjectId(subjectId); // Retrieves posts for the subject.
        for (Post post : postsForSubject) { // Adds each post to the current subject. Complexity: Loop.
            currentSubject.addPost(post);
        }
 
        JPanel topPanel = new JPanel(new BorderLayout()); // Panel for back button and title.
        JPanel infoPanel = new JPanel(); // Panel for target and average marks.
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); // Sets vertical layout for info panel.
 
        JButton backButton = new JButton("Back"); // Creates a back button.
        backButton.addActionListener(e -> showSubjectListView()); // Adds action listener to go back to subject list.
        topPanel.add(backButton, BorderLayout.WEST); // Adds back button to the top panel.
 
        JLabel subjectTitleLabel = new JLabel("Posts for: " + currentSubject.getName()); // Creates subject title label.
        subjectTitleLabel.setFont(new Font("Arial", Font.BOLD, 16)); // Sets font for title.
        subjectTitleLabel.setHorizontalAlignment(SwingConstants.CENTER); // Centers the title.
        topPanel.add(subjectTitleLabel, BorderLayout.CENTER); // Adds title to the top panel.
 
        JPanel targetMarkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Panel for target mark.
        JLabel targetMarkLabel = new JLabel("Target Mark (%):"); // Label for target mark.
        targetMarkPanel.add(targetMarkLabel); // Adds label to target mark panel.
 
        JTextField targetMarkField = new JTextField(String.valueOf(currentSubject.getTargetMark()), 5); // Text field for target mark.
        targetMarkField.addActionListener(new ActionListener() { // Adds action listener for target mark field.
            @Override
            public void actionPerformed(ActionEvent e) { // Handles action when target mark is entered.
                try { // Attempts to parse and update target mark.
                    int newTargetMark = Integer.parseInt(targetMarkField.getText()); // Parses new target mark.
                    if (newTargetMark >= 0 && newTargetMark <= 100) { // Validates target mark range.
                        database.updateSubjectTargetMark(currentSubject.getId(), newTargetMark); // Updates target mark in database.
                        currentSubject.setTargetMark(newTargetMark); // Updates in-memory subject object.
                        JOptionPane.showMessageDialog(GUI.this, "Target mark updated successfully!"); // Shows success message.
 
                        if (currentSubject.getAverageMark() >= currentSubject.getTargetMark()) { // Checks if target mark is achieved.
                            JOptionPane.showMessageDialog(GUI.this, "Congratulations! You have achieved your target grade for this subject!", "Target Achieved", JOptionPane.INFORMATION_MESSAGE); // Shows congratulatory message.
                        }
                    } else { // Handles invalid target mark input.
                        JOptionPane.showMessageDialog(GUI.this, "Target mark must be between 0 and 100.", "Input Error", JOptionPane.ERROR_MESSAGE); // Shows error message.
                        targetMarkField.setText(String.valueOf(currentSubject.getTargetMark())); // Reverts to old value.
                    }
                } catch (NumberFormatException ex) { // Catches number format exceptions.
                    JOptionPane.showMessageDialog(GUI.this, "Please enter a valid number for target mark.", "Input Error", JOptionPane.ERROR_MESSAGE); // Shows error message.
                    targetMarkField.setText(String.valueOf(currentSubject.getTargetMark())); // Reverts to old value.
                } catch (java.sql.SQLException ex) { // Catches SQL exceptions.
                    JOptionPane.showMessageDialog(GUI.this, "Error saving target mark to database: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); // Shows database error.
                    targetMarkField.setText(String.valueOf(currentSubject.getTargetMark())); // Reverts to old value.
                }
            }
        });
        targetMarkPanel.add(targetMarkField); // Adds target mark field to its panel.
        infoPanel.add(targetMarkPanel); // Adds target mark panel to info panel.
 
        JPanel averageMarkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT)); // Panel for average mark.
        JLabel averageMarkLabel = new JLabel("Average Mark (%):"); // Label for average mark.
        averageMarkPanel.add(averageMarkLabel); // Adds label to average mark panel.
 
        JTextField averageMarkField = new JTextField(String.format("%.2f", currentSubject.getAverageMark()), 5); // Text field for average mark.
        averageMarkField.setEditable(false); // Makes average mark field non-editable.
        averageMarkPanel.add(averageMarkField); // Adds average mark field to its panel.
        infoPanel.add(averageMarkPanel); // Adds average mark panel to info panel.
 
        if (currentSubject.getAverageMark() >= currentSubject.getTargetMark()) { // Checks if target mark is achieved.
            JOptionPane.showMessageDialog(this, "Congratulations! You have achieved your target grade for this subject!", "Target Achieved", JOptionPane.INFORMATION_MESSAGE); // Shows congratulatory message.
        }
 
        JPanel headerContainerPanel = new JPanel(); // Container for header and info panels.
        headerContainerPanel.setLayout(new BoxLayout(headerContainerPanel, BoxLayout.Y_AXIS)); // Sets vertical layout.
        headerContainerPanel.add(topPanel); // Adds top panel.
        headerContainerPanel.add(infoPanel); // Adds info panel.
        postListPanel.add(headerContainerPanel, BorderLayout.NORTH); // Adds combined header to post list view.
 
        JPanel postsPanel = new JPanel(); // Panel for post buttons.
        postsPanel.setLayout(new BoxLayout(postsPanel, BoxLayout.Y_AXIS)); // Sets vertical layout.
        JScrollPane scrollPane = new JScrollPane(postsPanel); // Adds scrollability.
        postListPanel.add(scrollPane, BorderLayout.CENTER); // Adds scroll pane to post list panel.
 
        List<Post> posts = database.getPostsBySubjectId(subjectId); // Retrieves posts for the subject.
        if (posts != null && !posts.isEmpty()) { // Checks if posts exist.
            for (Post post : postsForSubject) { // Iterates through each post to create a button. Complexity: Loop.
                JButton postButton = new JButton(post.getTitle()); // Creates a button for the post.
                postButton.setAlignmentX(Component.CENTER_ALIGNMENT); // Centers the button horizontally.
                postButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, postButton.getMinimumSize().height)); // Makes button fill width.
                postButton.addActionListener(new ActionListener() { // Adds action listener to the button.
                    @Override
                    public void actionPerformed(ActionEvent e) { // Handles button click event.
                        showPostDetailDialog(post); // Displays post details dialog.
                    }
                });
                postsPanel.add(postButton); // Adds button to posts panel.
                postsPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Adds spacing.
            }
        } else { // If no posts are found.
            postsPanel.add(new JLabel("No posts found for this subject.")); // Displays a message.
        }
 
        postListPanel.revalidate(); // Revalidates the panel.
        postListPanel.repaint(); // Repaints the panel.
 
        CardLayout cl = (CardLayout) (mainPanel.getLayout()); // Gets CardLayout from main panel.
        cl.show(mainPanel, "PostListView"); // Shows "PostListView" card.
    }

    private void showPostDetailDialog(Post post) { // Displays a dialog with detailed information about a post. Complexity: Use of additional libraries.
        JDialog postDetailDialog = new JDialog(this, "Post Details", true); // Creates a new dialog.
        postDetailDialog.setSize(400, 300); // Sets dialog size.
        postDetailDialog.setLocationRelativeTo(this); // Centers dialog relative to parent.
 
        JPanel detailPanel = new JPanel(); // Panel to hold post details.
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS)); // Sets vertical layout.
        detailPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // Sets padding.
 
        JPanel titleDatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // Panel for title and date.
        JLabel titleLabel = new JLabel("Title: " + post.getTitle()); // Label for post title.
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14)); // Sets font for title.
        titleDatePanel.add(titleLabel); // Adds title label.
        
        JLabel dateLabel = new JLabel(" (Date: " + (post.getCreationDate() != null ? post.getCreationDate().toString() : "N/A") + ")"); // Label for creation date.
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Sets font for date.
        titleDatePanel.add(dateLabel); // Adds date label.
        detailPanel.add(titleDatePanel); // Adds title/date panel.
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Adds spacing.
 
        JLabel descriptionLabel = new JLabel("Description:"); // Label for description.
        descriptionLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Sets font for description label.
        detailPanel.add(descriptionLabel); // Adds description label.
        JTextArea descriptionArea = new JTextArea(post.getDescription()); // Text area for description.
        descriptionArea.setEditable(false); // Makes description area non-editable.
        descriptionArea.setLineWrap(true); // Enables line wrapping.
        descriptionArea.setWrapStyleWord(true); // Wraps at word boundaries.
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea); // Adds scrollability to description area.
        descriptionScrollPane.setPreferredSize(new Dimension(350, 80)); // Sets preferred size.
        detailPanel.add(descriptionScrollPane); // Adds description scroll pane.
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Adds spacing.
 
        JLabel studentMaxMarksLabel = new JLabel("Marks: " + (post.getStudentMark() != null ? String.format("%.2f", post.getStudentMark()) : "N/A") +
                                                 " / " + (post.getMaximumMark() != null ? String.format("%.2f", post.getMaximumMark()) : "N/A")); // Label for student and max marks.
        studentMaxMarksLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Sets font for marks label.
        detailPanel.add(studentMaxMarksLabel); // Adds marks label.
 
        JLabel percentageLabel = new JLabel("Percentage: " + String.format("%.2f", post.getPercentageMark()) + "%"); // Label for percentage.
        percentageLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Sets font for percentage label.
        detailPanel.add(percentageLabel); // Adds percentage label.
 
        JLabel gradeLabel = new JLabel("Grade: " + post.getGrade()); // Label for grade.
        gradeLabel.setFont(new Font("Arial", Font.PLAIN, 12)); // Sets font for grade label.
        detailPanel.add(gradeLabel); // Adds grade label.
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Adds spacing.
 
        if (post.getComment() != null && !post.getComment().isEmpty()) { // Checks if a comment exists.
            JLabel commentLabel = new JLabel("Comment:"); // Label for comment.
            commentLabel.setFont(new Font("Arial", Font.BOLD, 12)); // Sets font for comment label.
            detailPanel.add(commentLabel); // Adds comment label.
            JTextArea commentArea = new JTextArea(post.getComment()); // Text area for comment.
            commentArea.setEditable(false); // Makes comment area non-editable.
            commentArea.setLineWrap(true); // Enables line wrapping.
            commentArea.setWrapStyleWord(true); // Wraps at word boundaries.
            JScrollPane commentScrollPane = new JScrollPane(commentArea); // Adds scrollability to comment area.
            commentScrollPane.setPreferredSize(new Dimension(350, 50)); // Sets preferred size.
            detailPanel.add(commentScrollPane); // Adds comment scroll pane.
        }
 
        postDetailDialog.add(detailPanel); // Adds detail panel to the dialog.
        postDetailDialog.setVisible(true); // Makes the dialog visible.
    }
}