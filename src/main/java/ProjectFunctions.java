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

import java.awt.Color;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class ProjectFunctions {

    private final Utils utils;
    private final Message message;
    private final ProjectCommand pc;
    private Project project;
    private Task task;

    public ProjectFunctions(Project project, Utils utils, Message message, ProjectCommand projectCommand) {
        this.project = project;
        this.utils = utils;
        this.message = message;
        this.pc = projectCommand;
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
        List<Task> tasks = this.project.getTasks();
        if (tasks.isEmpty())
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Oh no!",
                                    "No tasks were found!", false).setColor(Color.RED).build())
                    .setActionRow(
                            Button.primary("showProject", "View project"),
                            Button.primary("newTask", "Create task")
                    ).queue();
        else {
            Button create = Button.primary("newTask", "Create task").asDisabled();
            if (this.project.getUsers().contains(event.getUser().getIdLong()) ||
                    this.project.getOwner() == event.getUser().getIdLong())
                create = Button.primary("newTask", "Create task");
            SelectMenu.Builder menu = SelectMenu.create("taskSelector").setPlaceholder("task").setRequiredRange(1, 1);
            for (Task t : this.project.getTasks())
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
                event.editMessageEmbeds(
                        new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                                .addField(
                                        "SUCCESS!",
                                        "`" + this.project.getName() + "` was deleted",
                                        false
                                )
                                .setColor(Color.GREEN).build()
                ).setActionRow(
                        event.getButton().asDisabled(),
                        Button.danger("delNo", "No").asDisabled()
                ).queue();
                this.project = null;
            } else event.reply("You do not have permission to do this!").setEphemeral(true).queue();
        } catch (IOException e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    public void sendProjectEmbed(@NotNull ButtonInteractionEvent event, boolean ended) {
        Button tasks = Button.primary("tasks", "View tasks");
        Button edit = Button.primary("edit", "Edit project");
        Button join = Button.primary("join", "Join project");
        Button delete = Button.danger("delete", "Delete project");
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
        if (this.project.getUsers().contains(event.getUser().getIdLong()) ||
                this.project.isPrivate() || this.project.getOwner() == event.getUser().getIdLong())
            join = join.asDisabled();
        event.editMessageEmbeds(createProjectEmbed().build())
                .setActionRow(tasks, edit, join, delete).queue();
    }

    public void sendProjectEmbed(@NotNull MessageReceivedEvent event, boolean ended) {
        Button tasks = Button.primary("tasks", "View tasks");
        Button edit = Button.primary("edit", "Edit project");
        Button join = Button.primary("join", "Join project");
        Button leave = Button.primary("leave", "Leave project");
        Button delete = Button.danger("delete", "Delete project");
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
                .setActionRow(tasks, edit, join, delete).queue();
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
                    Button.danger("showProject", "Cancel")
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
                        Button.danger("cancelEdit", "Cancel")
                ).queue();
        this.pc.getEditingOwner().put(event.getUser().getIdLong(), new Object[]{false, this.project});
    }

    public void editOwner(@NotNull ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Waiting...",
                                "The next person you ping will become this projects owner",
                                false
                        ).setColor(Color.BLUE).build())
                .setActionRow(
                        Button.danger("cancelEdit", "Cancel")
                ).queue();
        this.pc.getEditingOwner().put(event.getUser().getIdLong(), new Object[]{true, this.project});
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
                Button.danger("showProject", "Back")
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
        this.pc.getMakingTask().put(event.getUser().getIdLong(), new Object[]{true, this.task, this});
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
        this.pc.getMakingTask().put(event.getUser().getIdLong(), new Object[]{false, this.task, this});
    }

    public void save(ButtonInteractionEvent event) {
        try {
            String name = this.task.getName();
            ProjectFunctions.updateProjects(this.project.addTask(this.task));
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
        this.pc.getMakingTask().remove(event.getUser().getIdLong());
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
}