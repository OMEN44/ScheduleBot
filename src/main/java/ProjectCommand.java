import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ProjectCommand extends ListenerAdapter {

    //                   <{embed title, sender ID}, project>
    private final HashMap<Key, ProjectFunctions> hashMap = new HashMap<>();
    private final HashMap<Long, Object[]> editingOwner = new HashMap<>();
    private final Utils utils;

    public ProjectCommand(Utils utils) {
        this.utils = utils;
    }

    public HashMap<Long, Object[]> getEditingOwner() {
        return editingOwner;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            String message = event.getMessage().getContentRaw();
            String[] args = message.split(" ");
            String fileName = event.getGuild().getId() + event.getGuild().getName().replace(" ", "_") + ".json";
            List<Project> projects = Project.getProjects(fileName);
            Project project = null;

            if (this.editingOwner.get(event.getAuthor().getIdLong()) != null) {
                Project p = (Project) this.editingOwner.get(event.getAuthor().getIdLong())[1];
                if ((boolean) this.editingOwner.get(event.getAuthor().getIdLong())[0]) {
                    if (event.getMessage().getMentionedUsers().size() != 1 ||
                            event.getMessage().getMentionedUsers().get(0).isBot()) {
                        Utils.errorEmbed(
                                event.getMessage(),
                                "Either no user was mentioned in your message or too many were."
                        );
                        this.editingOwner.remove(event.getAuthor().getIdLong());
                        return;
                    }
                    p.setOwner(event.getMessage().getMentionedUsers().get(0).getIdLong());
                } else {
                    p.setDescription(event.getMessage().getContentRaw());
                }
                ProjectFunctions.updateProjects(p, fileName);
                this.editingOwner.remove(event.getAuthor().getIdLong());
                Utils.successEmbed(event.getMessage(), "Project details have been updated");
            }

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
                ProjectFunctions pf = new ProjectFunctions(
                        project,
                        this.utils,
                        fileName,
                        event.getMessage(),
                        this
                );
                if (args.length == 1) {
                    pf.sendProjectEmbed(event, false);
                    Key key = new Key("card id: " + event.getMessage().getIdLong(), event.getAuthor().getIdLong());
                    this.hashMap.put(
                            key,
                            pf
                    );
                }
            }
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        try {
            if (event.getMessage().getEmbeds().size() > 0) {
                if (Objects.equals(event.getMessage().getEmbeds().get(0).getTitle(), "ERROR")) {
                    return;
                }
            }
            Key key = new Key(
                    Objects.requireNonNull(event.getMessage().getEmbeds().get(0).getFooter()).getText(),
                    event.getUser().getIdLong()
            );
            ProjectFunctions pf = this.hashMap.get(key);

            switch (Objects.requireNonNull(event.getButton().getId())) {
                case "showProject" -> pf.sendProjectEmbed(event, false);
                case "join" -> pf.joinProject(event);
                case "leave" -> pf.leaveProject(event);
                case "tasks" -> pf.showTasks(event);
                case "delete" -> pf.confirmDelete(event);
                case "delYes" -> pf.deleteProject(event);
                case "edit" -> pf.editMenu(event);
                case "editDescription" -> pf.editDescription(event);
                case "editOwner" -> pf.editOwner(event);
                case "cancelEdit" -> {
                    this.editingOwner.remove(event.getUser().getIdLong());
                    pf.sendProjectEmbed(event, false);
                }
                case "editPrivate" -> pf.editPrivate(event);
                case "privYes" -> pf.privateYes(event);
                case "privNo" -> pf.privateNo(event);
                //case "newTask" -> pf.newTask(event);
            }
        } catch (NullPointerException e) {
            event.reply("Sorry, that ones not yours!").setEphemeral(true).queue();
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }
}
