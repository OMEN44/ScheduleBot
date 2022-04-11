import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ProjectCommand extends ListenerAdapter {

    private final Utils utils;
    //                   <{embed title, sender ID}, project>
    private final HashMap<Key, ProjectFunctions> hashMap = new HashMap<>();
    private String fileName;

    public ProjectCommand(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] args = message.split(" ");
        fileName = event.getGuild().getId() + event.getGuild().getName().replace(" ", "_") + ".json";
        List<Project> projects = Project.getProjects(fileName);
        Project project = null;

        if (Objects.equals(args[0], "*new") || Objects.equals(args[0], "*list")) return;

        if (message.startsWith("*")) {
            for (Project p : projects) {
                if (Objects.equals(args[0].substring(1), p.getName())) {
                    project = p;
                    break;
                }
            }

            if (project == null)
                return;
            //the project has been found and exists!
            ProjectFunctions pf = new ProjectFunctions(event.getAuthor(), project, this.utils, this.fileName, event.getMessage());
            if (args.length == 1) {
                pf.sendProjectEmbed(event, false);
                Key key = new Key("card id: " + event.getMessage().getIdLong(), event.getAuthor().getIdLong());
                this.hashMap.put(
                        key,
                        pf
                );
            }
        }
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        try {
            Key key = new Key(
                    Objects.requireNonNull(event.getMessage().getEmbeds().get(0).getFooter()).getText(),
                    event.getUser().getIdLong()
            );
            ProjectFunctions pf = this.hashMap.get(key);

            switch (Objects.requireNonNull(event.getButton().getId())) {
                case "showProject" -> pf.sendProjectEmbed(event, false);
                case "join" -> pf.joinProject(event);
                case "tasks" -> pf.showTasks(event);
                case "delete" -> pf.confirmDelete(event);
                case "delYes" -> pf.deleteProject(event);
                case "edit" -> pf.editMenu(event);
                case "editDescription" -> pf.editDescription(event);
                case "editOwner" -> pf.editOwner(event);
                case "editPrivate" -> pf.editPrivate(event);
                case "privYes" -> pf.privateYes(event);
                case "privNo" -> pf.privateNo(event);
            }
        } catch (NullPointerException e) {
            event.reply("Sorry, that ones not yours!").setEphemeral(true).queue();
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    /*public void onMessage(@NotNull MessageReceivedEvent event) {
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
    }*/

}
