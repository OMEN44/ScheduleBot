import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.entities.User;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Project {
    //object stuff
    private String name = null;
    private String description = "";
    private boolean hasCategory;
    private boolean isPrivate;
    //discord stuff
    private long owner;
    private long role;
    private List<Long> users = new ArrayList<>();
    private List<Task> tasks = new ArrayList<>();

    public Project(String name) {
        this.name = name;
    }

    public Project(String name, long ownerId) {
        this(name);
        this.owner = ownerId;
    }

    public Project(String name, long owner, String description) {
        this(name, owner);
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isHasCategory() {
        return hasCategory;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public long getOwner() {
        return owner;
    }

    public long getRole() {
        return role;
    }

    public List<Long> getUsers() {
        return users;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setHasCategory(boolean hasCategory) {
        this.hasCategory = hasCategory;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public void setOwner(long owner) {
        this.owner = owner;
    }

    public void setRole(long role) {
        this.role = role;
    }

    public void setUsers(List<Long> users) {
        this.users = users;
    }

    public void addUser(Long... users) {
        this.users.addAll(Arrays.asList(users));
    }

    public void setTasks(Task... tasks) {
        this.tasks = Arrays.stream(tasks).toList();
    }

    public static List<Project> getProjects(String path) {
        List<Project> projects = new ArrayList<>();
        Reader newReader;
        try {
            Gson gson = new Gson();
            newReader = Files.newBufferedReader(Paths.get(path));
            projects = gson.fromJson(
                    newReader, new TypeToken<List<Project>>() {
                    }.getType()
            );
            newReader.close();
            if (projects == null)
                projects = new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return projects;
    }

    public static boolean projectExist(String projectName, String path) {
        List<Project> projects = getProjects(path);
        boolean exists = false;
        for (Project proj : projects) {
            if (Objects.equals(proj.getName(), projectName)) {
                exists = true;
                break;
            }
        }
        return exists;
    }

    public static List<String> getProjectNames(String path) {
        List<Project> projects = getProjects(path);
        List<String> names = new ArrayList<>();
        for (Project proj : projects)
            names.add(proj.getName());
        return names;
    }

    public static Project projectByName(String name, String path) {
        List<Project> projects = getProjects(path);
        Project target = null;
        for (Project proj : projects) {
            if (Objects.equals(proj.getName(), name)) target = proj;
        }
        return target;
    }

    public Project addTask(Task t) {
        this.tasks.add(t);
        return this;
    }
}
