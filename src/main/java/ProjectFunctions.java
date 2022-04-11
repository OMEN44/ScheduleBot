import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.internal.entities.UserById;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ProjectFunctions {

    private final User sender;
    private final Utils utils;
    private final String fileName;
    private final Message message;
    private boolean description = false;
    private boolean owner = false;
    private HashMap<Long, Key> editor = new HashMap<>();
    private Project project;

    public Project getProject() {
        return project;
    }

    public User getSender() {
        return sender;
    }

    public ProjectFunctions(User sender, Project project, Utils utils, String fileName, Message message) {
        this.sender = sender;
        this.project = project;
        this.utils = utils;
        this.fileName = fileName;
        this.message = message;
    }

    public void showTasks(ButtonInteractionEvent event) {
        List<Task> tasks = this.project.getTasks();
        if (tasks.isEmpty())
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Oh no!",
                                    "No tasks were found!", false).build())
                    .setActionRow(
                            Button.primary("showProject", "View project")
                    ).queue();
    }

    public void confirmDelete(ButtonInteractionEvent event) {
        if (event.getUser().getIdLong() == project.getOwner()) {
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                    .addField(
                            "Delete " + project.getName(),
                            "Are you sure you want to delete this project",
                            false
                    ).build()).setActionRow(
                    Button.success("delYes", "Yes"),
                    Button.danger("showProject", "No")
            ).queue();
        } else event.reply("You do not have permission to do this!").setEphemeral(true).queue();
    }

    public void deleteProject(ButtonInteractionEvent event) {
        try {
            if (event.getUser().getIdLong() == project.getOwner()) {
                List<Project> projects = Project.getProjects(fileName);
                projects.removeIf(proj -> Objects.equals(proj.getName(), this.project.getName()));
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                Writer newWriter = Files.newBufferedWriter(Paths.get(fileName));
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

    public void sendProjectEmbed(ButtonInteractionEvent event, boolean ended) {
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
        if (this.project.getUsers().contains(event.getUser()) || this.project.isPrivate())
            join = join.asDisabled();
        event.editMessageEmbeds(createProjectEmbed().build())
                .setActionRow(tasks, edit, join, delete).queue();
    }

    public void sendProjectEmbed(MessageReceivedEvent event, boolean ended) {
        Button tasks = Button.primary("tasks", "View tasks");
        Button edit = Button.primary("edit", "Edit project");
        Button join = Button.primary("join", "Join project");
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
        if (this.project.getUsers().contains(event.getAuthor()) || this.project.isPrivate())
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
        eb.addField("Owner", new UserById(this.project.getOwner()).getAsMention(), true);
        if (this.project.isPrivate())
            eb.addField("Private project", "`true`", true);
        else
            eb.addField("Private project", "`false`", true);
        if (!this.project.getTasks().isEmpty())
            eb.addField("Number of tasks:", String.valueOf(this.project.getTasks().size()), true);
        if (!this.project.getUsers().isEmpty()) {
            StringBuilder users = new StringBuilder();
            for (User u : this.project.getUsers())
                users.append(u.getName());
            eb.addField("Users", users.substring(0, (users.length() - 1)), true);
        }
        return eb;
    }

    private void updateProjects() {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Project> projects = Project.getProjects(fileName);
            projects.removeIf(proj -> Objects.equals(proj.getName(), this.project.getName()));
            projects.add(this.project);

            Writer newWriter = Files.newBufferedWriter(Paths.get(fileName));
            gson.toJson(projects, newWriter);
            newWriter.close();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
    }

    public void joinProject(ButtonInteractionEvent event) {
        for (User u : this.project.getUsers()) {
            if (u.getIdLong() == event.getUser().getIdLong()) {
                event.reply("Your already a member of this project!").setEphemeral(true).queue();
                event.editButton(event.getButton().asDisabled()).queue();
                return;
            }
        }
        if (!this.project.isPrivate()) {
            this.project.addUser(event.getUser());
            updateProjects();
            sendProjectEmbed(event, false);
        } else {
            event.reply("This project is private! Ask the owner to send you an invite.")
                    .setEphemeral(true).queue();
            event.editButton(event.getButton().asDisabled()).queue();
        }
    }

    public void editMenu(ButtonInteractionEvent event) {
        if (event.getUser().getIdLong() == this.project.getOwner()) {
            event.editMessageEmbeds(
                    new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Project editor", "Select an aspect to edit below.", false)
                            .build()
            ).setActionRow(
                    Button.primary("editDescription", "Description"),
                    Button.primary("editPrivate", "Private"),
                    Button.primary("editOwner", "Ownership"),
                    Button.danger("showProject", "Cancel")
            ).queue();
        } else event.reply("Only the owner can edit a project!").setEphemeral(true).queue();
    }

    public void editDescription(ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                .addField(
                        "Waiting...",
                        "The next message you send will become this projects description",
                        false
                ).build()).queue();
        this.description = true;
    }

    public void editOwner(ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                .addField(
                        "Waiting...",
                        "The next person you ping will become this projects owner",
                        false
                ).build()).queue();
        this.owner = true;
    }

    public void editPrivate(ButtonInteractionEvent event) {
        event.editMessageEmbeds(
                new EmbedBuilder()
                        .setFooter("card id: " + this.message.getIdLong())
                        .addField(
                                "Make project private?",
                                "This means that people cant join your project but can still view it.",
                                false
                        ).build()
        ).setActionRow(
                Button.success("privYes", "Yes"),
                Button.danger("privNo", "No")
        ).queue();
    }

    public void privateYes(ButtonInteractionEvent event) {
        this.project.setPrivate(true);
        updateProjects();
        sendProjectEmbed(event, false);
    }

    public void privateNo(ButtonInteractionEvent event) {
        this.project.setPrivate(false);
        updateProjects();
        sendProjectEmbed(event, false);
    }
}
