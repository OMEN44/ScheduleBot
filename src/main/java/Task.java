import java.util.Arrays;
import java.util.List;

public class Task {
    private final String name;
    private String description;
    //progress is updated by users and is an int out of 100
    private int progress;
    private List<Long> assignedUsers;

    public Task(String name) {
        this.name = name;
    }

    public Task(String name, String description, Long... users) {
        this(name);
        this.description = description;
        this.assignedUsers = Arrays.stream(users).toList();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getProgress() {
        return progress;
    }

    public List<Long> getAssignedUsers() {
        return assignedUsers;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setAssignedUsers(List<Long> assignedUsers) {
        this.assignedUsers = assignedUsers;
    }
}
