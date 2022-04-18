import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class Logic {

    private final Utils utils;
    private final Message message;
    private final Listeners listeners;
    private Project project;
    private Task task;

    public Logic(Project project, Utils utils, Message message, Listeners projectCommand) {
        this.project = project;
        this.utils = utils;
        this.message = message;
        this.listeners = projectCommand;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Project getProject() {
        return project;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public Task getTask() {
        return task;
    }

    public static void updateProjects(Project project) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Project> projects = Project.getProjects("projects.json");
            projects.removeIf(proj -> Objects.equals(proj.getName(), project.getName()));
            projects.add(project);

            Writer newWriter = Files.newBufferedWriter(Paths.get("projects.json"));
            gson.toJson(projects, newWriter);
            newWriter.close();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateProjects(Project project, Task task) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Project> projects = Project.getProjects("projects.json");
            projects.removeIf(proj -> Objects.equals(proj.getName(), project.getName()));
            List<Task> tasks = project.getTasks();
            tasks.removeIf(t -> Objects.equals(t.getName(), task.getName()));
            project.addTask(task);
            projects.add(project);

            Writer newWriter = Files.newBufferedWriter(Paths.get("projects.json"));
            gson.toJson(projects, newWriter);
            newWriter.close();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public void showTasks(ButtonInteractionEvent event) {
        this.task = null;
        List<Task> tasks = this.project.getTasks();
        Button createTask = Button.primary("newTask", "Create task");
        if (!this.project.getUsers().contains(event.getUser().getIdLong()) ||
                event.getUser().getIdLong() != this.project.getOwner())
            createTask = createTask.asDisabled();
        if (tasks.isEmpty())
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Oh no!",
                                    "No tasks were found!", false).setColor(Color.RED).build())
                    .setActionRow(
                            Button.primary("showProject", "View project"),
                            createTask
                    ).queue();
        else {
            Button create = Button.primary("newTask", "Create task").asDisabled();
            if (this.project.getUsers().contains(event.getUser().getIdLong()) ||
                    this.project.getOwner() == event.getUser().getIdLong())
                create = Button.primary("newTask", "Create task");

            SelectMenu.Builder menu = SelectMenu.create("taskMenu").setPlaceholder("task").setRequiredRange(1, 1);
            for (Task t : tasks)
                menu.addOption(t.getName(), t.getName(), t.getDescription());
            event.editMessageEmbeds(
                    new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Tasks:", "select one task to view", false)
                            .setColor(Color.BLUE)
                            .build()
            ).setActionRows(ActionRow.of(menu.build()), ActionRow.of(
                    Button.primary("showProject", "View project"),
                    create
            )).queue();
        }
    }

    public void confirmDelete(@NotNull ButtonInteractionEvent event) {
        if (event.getUser().getIdLong() == project.getOwner()) {
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                    .addField(
                            "Delete " + project.getName(),
                            "Are you sure you want to delete this project",
                            false
                    ).setColor(Color.BLUE).build()).setActionRow(
                    Button.success("delYes", "Yes"),
                    Button.danger("showProject", "No")
            ).queue();
        } else event.reply("You do not have permission to do this!").setEphemeral(true).queue();
    }

    public void deleteProject(@NotNull ButtonInteractionEvent event) {
        try {
            if (event.getUser().getIdLong() == project.getOwner()) {
                List<Project> projects = Project.getProjects("projects.json");
                projects.removeIf(proj -> Objects.equals(proj.getName(), this.project.getName()));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer newWriter = Files.newBufferedWriter(Paths.get("projects.json"));
                gson.toJson(projects, newWriter);
                newWriter.close();
                home(event);
                this.project = null;
            } else event.reply("You do not have permission to do this!").setEphemeral(true).queue();
        } catch (IOException e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    public void sendProjectEmbed(@NotNull GenericComponentInteractionCreateEvent event, boolean ended) {
        Button tasks = Button.primary("tasks", "View tasks");
        Button edit = Button.primary("edit", "Edit project");
        Button join = Button.primary("join", "Join project");
        Button leave = Button.primary("leave", "Leave project");
        Button delete = Button.danger("delete", "Delete project");
        Button home = Button.danger("home", "Back");
        if (event.getUser().getIdLong() != this.project.getOwner()) {
            edit = edit.asDisabled();
            delete = delete.asDisabled();
        }
        if (ended) {
            tasks = tasks.asDisabled();
            edit = edit.asDisabled();
            join = join.asDisabled();
            delete = delete.asDisabled();
        }
        if (this.project.getUsers().contains(event.getUser().getIdLong()))
            join = leave;
        if (this.project.isPrivate() || this.project.getOwner() == event.getUser().getIdLong())
            join = join.asDisabled();
        event.editMessageEmbeds(createProjectEmbed().build())
                .setActionRow(tasks, edit, join, home, delete).queue();
    }

    public void sendProjectEmbed(@NotNull MessageReceivedEvent event, boolean ended) {
        Button tasks = Button.primary("tasks", "View tasks");
        Button edit = Button.primary("edit", "Edit project");
        Button join = Button.primary("join", "Join project");
        Button leave = Button.primary("leave", "Leave project");
        Button delete = Button.danger("delete", "Delete project");
        Button home = Button.danger("home", "Back");
        if (event.getAuthor().getIdLong() != this.project.getOwner()) {
            edit = edit.asDisabled();
            delete = delete.asDisabled();
        }
        if (ended) {
            tasks = tasks.asDisabled();
            edit = edit.asDisabled();
            join = join.asDisabled();
            delete = delete.asDisabled();
        }
        if (this.project.getUsers().contains(event.getAuthor().getIdLong()))
            join = leave;
        if (this.project.isPrivate() || this.project.getOwner() == event.getAuthor().getIdLong())
            join = join.asDisabled();
        event.getMessage().replyEmbeds(createProjectEmbed().build())
                .setActionRow(tasks, edit, join, home, delete).queue();
    }

    public EmbedBuilder createProjectEmbed() {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle("Project: " + this.project.getName())
                .setFooter("card id: " + this.message.getIdLong())
                .setColor(Color.BLUE);
        if (!Objects.equals(this.project.getDescription(), ""))
            eb.addField("Project description", this.project.getDescription(), false);
        try {
            eb.addField("Owner", "<@" + this.project.getOwner() + ">", true);
        } catch (NullPointerException e) {
            eb.addField("Owner", "`Not in this server`", true);
        }
        if (this.project.isPrivate())
            eb.addField("Private project", "`true`", true);
        else
            eb.addField("Private project", "`false`", true);
        if (!this.project.getTasks().isEmpty())
            eb.addField("Number of tasks:", String.valueOf(this.project.getTasks().size()), true);
        if (!this.project.getUsers().isEmpty()) {
            StringBuilder users = new StringBuilder();
            for (Long l : this.project.getUsers()) {
                users.append("<@").append(l).append("> \n");
            }
            eb.addField("Users", users.toString(), true);
        }
        return eb;
    }

    public void joinProject(ButtonInteractionEvent event) {
        for (Long u : this.project.getUsers()) {
            if (u == event.getUser().getIdLong()) {
                event.reply("Your already a member of this project!").setEphemeral(true).queue();
                event.editButton(event.getButton().asDisabled()).queue();
                return;
            }
        }
        if (!this.project.isPrivate()) {
            this.project.addUser(event.getUser().getIdLong());
            updateProjects(this.project);
            sendProjectEmbed(event, false);
            event.editButton(Button.primary("leave", "Leave project")).queue();
        } else {
            event.reply("This project is private! Ask the owner to send you an invite.")
                    .setEphemeral(true).queue();
            event.editButton(event.getButton().asDisabled()).queue();
        }
    }

    public void leaveProject(ButtonInteractionEvent event) {
        boolean isJoined = false;
        for (Long u : this.project.getUsers()) {
            if (u == event.getUser().getIdLong()) {
                isJoined = true;
                break;
            }
        }

        if (!isJoined) {
            event.reply("You must be a member of this project to leave").setEphemeral(true).queue();
            return;
        }

        List<Long> users = this.project.getUsers();
        users.remove(event.getUser().getIdLong());
        this.project.setUsers(users);

        updateProjects(this.project);
        sendProjectEmbed(event, false);
        event.editButton(Button.primary("join", "Join project")).queue();
    }

    public void editMenu(@NotNull ButtonInteractionEvent event) {
        if (event.getUser().getIdLong() == this.project.getOwner()) {
            event.editMessageEmbeds(
                    new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Project editor", "Select an aspect to edit below.", false)
                            .setColor(Color.BLUE)
                            .build()
            ).setActionRow(
                    Button.primary("editDescription", "Description"),
                    Button.primary("editPrivate", "Private"),
                    Button.primary("editOwner", "Ownership"),
                    Button.danger("showProject", "Back")
            ).queue();
        } else event.reply("Only the owner can edit a project!").setEphemeral(true).queue();
    }

    public void editDescription(@NotNull ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Waiting...",
                                "The next message you send will become this projects description",
                                false
                        ).setColor(Color.BLUE).build())
                .setActionRow(
                        Button.danger("cancelEdit", "Back")
                ).queue();
        this.listeners.getEditingOwner().put(event.getUser().getIdLong(), new Object[]{false, this.project});
    }

    public void editOwner(@NotNull ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Waiting...",
                                "The next person you ping will become this projects owner",
                                false
                        ).setColor(Color.BLUE).build())
                .setActionRow(
                        Button.danger("cancelEdit", "Back")
                ).queue();
        this.listeners.getEditingOwner().put(event.getUser().getIdLong(), new Object[]{true, this.project});
    }

    public void editPrivate(@NotNull ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder()
                        .setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Make project private?",
                                "This means that people cant join your project but can still view it.",
                                false
                        ).setColor(Color.BLUE).build()
        ).setActionRow(
                Button.success("privYes", "Yes"),
                Button.danger("privNo", "No")
        ).queue();
    }

    public void privateYes(ButtonInteractionEvent event) {
        this.project.setPrivate(true);
        updateProjects(this.project);
        sendProjectEmbed(event, false);
    }

    public void privateNo(ButtonInteractionEvent event) {
        this.project.setPrivate(false);
        updateProjects(this.project);
        sendProjectEmbed(event, false);
    }

    // =====================
    //       Tasks:
    // =====================

    public void newTask(ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "New task",
                                "Select an option below to set it.\nName is required.",
                                false
                        ).setColor(Color.BLUE).build()
        ).setActionRow(
                Button.primary("taskName", "Set name"),
                Button.primary("taskDescription", "Set description"),
                Button.success("save", "Save"),
                Button.danger("tasks", "Back")
        ).queue();
    }

    public void setTaskName(ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Waiting...",
                                "The next thing that you send will become the tasks name",
                                false
                        ).setColor(Color.BLUE).build())
                .setActionRow(
                        Button.danger("cancelTaskEdit", "Back")
                ).queue();
        if (this.task == null)
            this.task = new Task();
        this.listeners.getMakingTask().put(event.getUser().getIdLong(), new Object[]{true, this.task, this});
    }

    public void setTaskDescription(ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Waiting...",
                                "Send the description as a message",
                                false
                        ).setColor(Color.BLUE).build())
                .setActionRow(
                        Button.danger("cancelTaskEdit", "Back")
                ).queue();
        if (this.task == null)
            this.task = new Task();
        this.listeners.getMakingTask().put(event.getUser().getIdLong(), new Object[]{false, this.task, this});
    }

    public void save(ButtonInteractionEvent event, boolean editing) {
        try {
            String name = this.task.getName();
            if (editing) {
                for (Task t : this.project.getTasks()) {
                    if (Objects.equals(name, t.getName())) {
                        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                                .addField(
                                        "ERROR",
                                        "A task with this name already exists!",
                                        false).setColor(Color.RED)
                                .build()).setActionRow(Button.danger("newTask", "Back")).queue();
                        return;
                    }
                }
                Logic.updateProjects(this.project.addTask(this.task));
            } else
                Logic.updateProjects(this.project, this.task);
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                    .addField(
                            "Success",
                            "Project " + this.project.getName() + " now has the task: " + name,
                            false).setColor(Color.GREEN)
                    .build()).setActionRow(Button.danger("showProject", "Back to project")).queue();
        } catch (NullPointerException e) {
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                    .addField(
                            "ERROR",
                            "a name is required! press back and assign a name to this task",
                            false).setColor(Color.RED)
                    .build()).setActionRow(Button.danger("newTask", "Back")).queue();
        }
    }

    public void cancelTaskEdit(ButtonInteractionEvent event) {
        this.listeners.getMakingTask().remove(event.getUser().getIdLong());
        newTask(event);
    }

    public void progress(ButtonInteractionEvent event) {
        this.task.setProgress(this.task.getProgress() + 10);
        updateProjects(this.project, this.task);
        sendTaskEmbed(event, this.task);
    }

    public void regress(ButtonInteractionEvent event) {
        this.task.setProgress(this.task.getProgress() - 10);
        updateProjects(this.project, this.task);
        sendTaskEmbed(event, this.task);
    }

    public void sendTaskEmbed(GenericComponentInteractionCreateEvent event, Task task) {
        int progress = Math.round(task.getProgress() / 10f);
        EmbedBuilder eb = new EmbedBuilder()
                .setFooter("card id: " + this.message.getIdLong())
                .setTitle("Task: " + task.getName())
                .setColor(Color.BLUE)
                .addField(
                        "Progress: ",
                        ":green_square:".repeat(progress) + ":red_square:".repeat(10 - progress),
                        false
                );
        if (task.getDescription() != null)
            eb.addField("description", task.getDescription(), false);
        Button up = Button.success("progress", "+1");
        Button down = Button.danger("regress", "-1");
        if (task.getProgress() == 0)
            down = down.asDisabled();
        if (task.getProgress() == 100)
            up = up.asDisabled();
        ActionRow ar = ActionRow.of(up, down, Button.primary("editTask", "Edit"),
                Button.danger("deleteTask", "Delete"));
        if (this.project.getUsers().contains(event.getUser().getIdLong()) ||
                this.project.getOwner() == event.getUser().getIdLong())
            event.editMessageEmbeds(eb.build()).setActionRows(
                    ar,
                    ActionRow.of(Button.primary("tasks", "View tasks"))
            ).queue();
        else
            event.editMessageEmbeds(eb.build()).setActionRow(
                    Button.primary("tasks", "View tasks")
            ).queue();
    }

    public void editTask(ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Edit task",
                                "Select an option below to edit.",
                                false
                        ).setColor(Color.BLUE).build()
        ).setActionRow(
                Button.primary("taskName", "Set name"),
                Button.primary("taskDescription", "Set description"),
                Button.success("saveEdit", "Save"),
                Button.danger("tasks", "Back")
        ).queue();
    }

    public void deleteTask(ButtonInteractionEvent event) {
        List<Task> tasks = this.project.getTasks();
        tasks.removeIf(t -> Objects.equals(t.getName(), this.task.getName()));
        this.project.setTasks(tasks);
        updateProjects(this.project);
        event.editMessageEmbeds(
                new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .setColor(Color.GREEN)
                        .addField(
                                "Success",
                                "task " + this.task.getName() + "was successfully deleted",
                                false)
                        .build()
        ).setActionRow(Button.danger("showProject", "Back")).queue();
    }

    public void helpEmbed(ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .setTitle("Help Page")
                        .setColor(Color.BLUE)
                        .addField("title", "placeholder", false)
                        .build()
        ).setActionRow(Button.danger("home", "Back")).queue();
    }

    public void home(ButtonInteractionEvent event) {
        SelectMenu.Builder sm = SelectMenu.create("projectMenu").setRequiredRange(1, 1).setPlaceholder("project");
        for (Project p : Project.getProjects("projects.json"))
            sm.addOption(p.getName(), p.getName());
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                .setTitle("Larry the project bot")
                .setColor(Color.BLUE)
                .addField(
                        "What I am:",
                        "I am a bot created by ΩMЄN44! I am based of the website Trello and " +
                                "am here to help with organising projects! I am button based and " +
                                "not text based so enjoy!",
                        false)
                .build()
        ).setActionRows(
                ActionRow.of(sm.build()),
                ActionRow.of(
                        Button.primary("createProject", "New project"),
                        Button.primary("help", "Help")
                )
        ).queue();
    }

    public void createProject(ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .setColor(Color.BLUE)
                        .addField(
                                "New Project",
                                "Send the name of your new project. It can only be 1 word.",
                                false
                        ).build()
        ).setActionRow(Button.danger("exitNewProject", "Back")).queue();
        if (this.project == null)
            this.project = new Project();
        this.listeners.getMakingProject().put(event.getUser().getIdLong(), this.project);
    }
}