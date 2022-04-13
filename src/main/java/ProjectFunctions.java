import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class ProjectFunctions {

    private final User sender;
    private final Utils utils;
    private final String fileName;
    private final Message message;
    private final ProjectCommand pc;
    private Project project;

    public ProjectFunctions(User sender, Project project, Utils utils, String fileName,
                            Message message, ProjectCommand projectCommand) {
        this.sender = sender;
        this.project = project;
        this.utils = utils;
        this.fileName = fileName;
        this.message = message;
        this.pc = projectCommand;
    }

    public void showTasks(ButtonInteractionEvent event) {
        List<Task> tasks = this.project.getTasks();
        if (tasks.isEmpty())
            event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                            .addField("Oh no!",
                                    "No tasks were found!", false).build())
                    .setActionRow(
                            Button.primary("showProject", "View project"),
                            Button.primary("newTask", "Create task").withEmoji(Emoji.fromMarkdown("<:new:245267426227388416>"))
                    ).queue();
    }

    public void confirmDelete(@NotNull ButtonInteractionEvent event) {
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

    public void deleteProject(@NotNull ButtonInteractionEvent event) {
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
        event.editMessageEmbeds(createProjectEmbed(event.getGuild()).build())
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
        event.getMessage().replyEmbeds(createProjectEmbed(event.getGuild()).build())
                .setActionRow(tasks, edit, join, delete).queue();
    }

    public EmbedBuilder createProjectEmbed(Guild guild) {
        //guild.retrieveMembersByIds()
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
                //System.out.println(l + guild.getMemberById(l).getEffectiveName());
                try {
                    users.append("<@").append(l).append("> \n");
                } catch (NullPointerException e) {
                    users.append("`missing` \n");
                }
            }
            eb.addField("Users", users.toString(), true);
        }
        return eb;
    }

    public static void updateProjects(Project project, String fileName) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            List<Project> projects = Project.getProjects(fileName);
            projects.removeIf(proj -> Objects.equals(proj.getName(), project.getName()));
            projects.add(project);

            Writer newWriter = Files.newBufferedWriter(Paths.get(fileName));
            gson.toJson(projects, newWriter);
            newWriter.close();
        } catch (NullPointerException | IOException e) {
            e.printStackTrace();
        }
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
            updateProjects(this.project, this.fileName);
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

        updateProjects(this.project, this.fileName);
        sendProjectEmbed(event, false);
        event.editButton(Button.primary("join", "Join project")).queue();
    }

    public void editMenu(@NotNull ButtonInteractionEvent event) {
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

    public void editDescription(@NotNull ButtonInteractionEvent event) {
        event.editMessageEmbeds(new EmbedBuilder().setFooter("card id: " + this.message.getIdLong())
                .addField(
                        "Waiting...",
                        "The next message you send will become this projects description",
                        false
                ).build())
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
                ).build())
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
                        ).build()
        ).setActionRow(
                Button.success("privYes", "Yes"),
                Button.danger("privNo", "No")
        ).queue();
    }

    public void privateYes(ButtonInteractionEvent event) {
        this.project.setPrivate(true);
        updateProjects(this.project, this.fileName);
        sendProjectEmbed(event, false);
    }

    public void privateNo(ButtonInteractionEvent event) {
        this.project.setPrivate(false);
        updateProjects(this.project, this.fileName);
        sendProjectEmbed(event, false);
    }
}
