import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.internal.entities.UserById;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class ProjectCommand extends ListenerAdapter {

    private final Utils utils;
    private Project project = null;
    private User sender = null;
    private String fileName;
    //editor state:
    private boolean description = false;
    private boolean owner = false;

    public ProjectCommand(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            if (this.description && event.getAuthor().equals(this.sender)) {
                this.project.setDescription(event.getMessage().getContentRaw());
                this.description = false;
                updateProjects();
            }
            if (this.owner && event.getAuthor().equals(this.sender)) {
                if (event.getMessage().getMentionedUsers().size() != 1 ||
                        event.getMessage().getMentionedUsers().get(0).isBot()) {
                    Utils.errorEmbed(
                            event.getMessage(),
                            "Either no user was mentioned in your message or too many were."
                    );
                }
                this.project.setOwner(event.getMessage().getMentionedUsers().get(0).getIdLong());
                this.owner = false;
                updateProjects();
            }

            String message = event.getMessage().getContentRaw();
            String[] args = message.split(" ");
            fileName =
                    event.getGuild().getId() + event.getGuild().getName().replace(" ", "_") + ".json";
            List<Project> projects = Project.getProjects(fileName);

            if (message.startsWith("*")) {
                String name = null;
                for (Project p : projects) {
                    if (Objects.equals(args[0].substring(1), p.getName())) {
                        name = p.getName();
                        this.project = p;
                        break;
                    }
                }
                if (name == null) {
                    //Utils.errorEmbed(event, "The project `" + args[0].substring(1) + "` does not exist!");
                    return;
                }
                //the project has been found and exists!
                if (args.length == 1) {
                    event.getMessage().replyEmbeds(createProjectEmbed().build()).setActionRow(
                            Button.primary("tasks", "View tasks"),
                            Button.primary("edit", "Edit project"),
                            Button.primary("join", "Join project"),
                            Button.danger("delete", "Delete project")
                    ).queue();
                    this.sender = event.getAuthor();
                }
            }
        } catch (Exception e) {
            this.utils.exceptionEmbed(event.getMessage(), e);
        }
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

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        try {
            if (this.project == null || this.sender == null) {
                event.editButton(event.getButton().asDisabled()).queue();
                return;
            }
            if (event.getUser().getIdLong() != this.sender.getIdLong()) {
                event.reply("Sorry, you cant interact with this menu!").setEphemeral(true).queue();
                return;
            }
            switch (Objects.requireNonNull(event.getButton().getId())) {
                case "showProject" -> sendProjectEmbed(event, false);
                case "tasks" -> {
                    List<Task> tasks = this.project.getTasks();
                    if (tasks.isEmpty())
                        event.editMessageEmbeds(new EmbedBuilder().addField("Oh no!",
                                        "No tasks were found!", false).build())
                                .setActionRow(
                                        Button.primary("showProject", "View project")
                                ).queue();
                }
                case "delete" -> {
                    if (event.getUser().getIdLong() == project.getOwner()) {
                        event.editMessageEmbeds(new EmbedBuilder().addField(
                                "Delete " + project.getName(),
                                "Are you sure you want to delete this project",
                                false
                        ).build()).setActionRow(
                                Button.success("delYes", "Yes"),
                                Button.danger("showProject", "No")
                        ).queue();
                    } else event.reply("You do not have permission to do this!").setEphemeral(true).queue();
                }
                case "delYes" -> {
                    try {
                        if (event.getUser().getIdLong() == project.getOwner()) {
                            List<Project> projects = Project.getProjects(fileName);
                            projects.removeIf(proj -> Objects.equals(proj.getName(), this.project.getName()));
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            Writer newWriter = Files.newBufferedWriter(Paths.get(fileName));
                            gson.toJson(projects, newWriter);
                            newWriter.close();
                            event.editMessageEmbeds(
                                    new EmbedBuilder()
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
                case "edit" -> {
                    if (event.getUser().getIdLong() == this.project.getOwner()) {
                        event.editMessageEmbeds(
                                new EmbedBuilder()
                                        .addField("Project editor", "Select an aspect to edit below.", false)
                                        .build()
                        ).setActionRow(
                                Button.primary("editDescription", "Description"),
                                Button.primary("editPrivate", "Private"),
                                Button.primary("editOwner", "Ownership"),
                                Button.danger("showProject", "Cancel")
                        ).queue();
                    } else event.reply("Only the owner can edit this!").setEphemeral(true).queue();
                }
                case "editDescription" -> {
                    event.editMessageEmbeds(new EmbedBuilder()
                            .addField(
                                    "Waiting...",
                                    "The next message you send will become this projects description",
                                    false
                            ).build()).queue();
                    this.description = true;
                }
                case "editOwner" -> {
                    event.editMessageEmbeds(new EmbedBuilder()
                            .addField(
                                    "Waiting...",
                                    "The next person you ping will become this projects owner",
                                    false
                            ).build()).queue();
                    this.owner = true;
                }
                case "editPrivate" -> event.editMessageEmbeds(
                        new EmbedBuilder()
                                .addField(
                                        "Make project private?",
                                        "This means that people cant join your project but can still view it.",
                                        false
                                ).build()
                ).setActionRow(
                        Button.success("privYes", "Yes"),
                        Button.danger("privNo", "No")
                ).queue();
                case "privYes" -> {
                    this.project.setPrivate(true);
                    updateProjects();
                    sendProjectEmbed(event, false);
                }
                case "privNo" -> {
                    this.project.setPrivate(false);
                    updateProjects();
                    sendProjectEmbed(event, false);
                }
                case "join" -> {
                    boolean isUser = false;
                    for (User u : this.project.getUsers()) {
                        if (u.getIdLong() == event.getUser().getIdLong())
                            isUser = true;
                    }
                    if (isUser) {
                        event.reply("You are already a member!").setEphemeral(true).queue();
                        event.editButton(event.getButton().asDisabled()).queue();
                    } else {
                        if (!this.project.isPrivate()) {
                            this.project.addUser(event.getUser());
                            updateProjects();
                            sendProjectEmbed(event, false);
                        } else {
                            event.reply("This project is private!").setEphemeral(true).queue();
                            event.editButton(event.getButton().asDisabled()).queue();
                        }
                    }
                }
            }
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    private void sendProjectEmbed(ButtonInteractionEvent event, boolean ended) {

        if (ended)
            event.editMessageEmbeds(createProjectEmbed().build()).setActionRow(
                    Button.primary("tasks", "View tasks").asDisabled(),
                    Button.primary("edit", "Edit project").asDisabled(),
                    Button.primary("join", "Join project").asDisabled(),
                    Button.danger("delete", "Delete project").asDisabled()
            ).queue();
        else {
            if (event.getUser().getIdLong() != this.project.getOwner())
                event.editMessageEmbeds(createProjectEmbed().build()).setActionRow(
                        Button.primary("tasks", "View tasks"),
                        Button.primary("edit", "Edit project").asDisabled(),
                        Button.primary("join", "Join project"),
                        Button.danger("delete", "Delete project").asDisabled()
                ).queue();
            else
                event.editMessageEmbeds(createProjectEmbed().build()).setActionRow(
                        Button.primary("tasks", "View tasks"),
                        Button.primary("edit", "Edit project"),
                        Button.primary("join", "Join project"),
                        Button.danger("delete", "Delete project")
                ).queue();
        }
    }

    public EmbedBuilder createProjectEmbed() {
        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(this.project.getName().substring(0, 1).toUpperCase() +
                        this.project.getName().substring(1))
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
}
