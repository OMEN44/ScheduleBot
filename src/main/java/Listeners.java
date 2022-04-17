import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Listeners extends ListenerAdapter {

    //                   <{embed title, sender ID}, project>
    private final HashMap<Key, Logic> hashMap = new HashMap<>();
    //                   <userid, {bool, project}>
    private final HashMap<Long, Object[]> editingOwner = new HashMap<>();
    //                   <userid, {bool, task}>
    private final HashMap<Long, Object[]> makingTask = new HashMap<>();
    //                   <userid, project>
    private final HashMap<Long, Project> makingProject = new HashMap<>();
    private final Utils utils;

    public Listeners(Utils utils) {
        this.utils = utils;
    }

    public HashMap<Long, Object[]> getEditingOwner() {
        return editingOwner;
    }

    public HashMap<Long, Object[]> getMakingTask() {
        return makingTask;
    }

    public HashMap<Long, Project> getMakingProject() {
        return makingProject;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            String message = event.getMessage().getContentRaw();
            String[] args = message.split(" ");
            List<Project> projects = Project.getProjects("projects.json");
            Project project = null;

            //edit project button listener
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
                Logic.updateProjects(p);
                this.editingOwner.remove(event.getAuthor().getIdLong());
                Utils.successEmbed(event.getMessage(), "Project details have been updated");
            }

            //create task button listener
            if (this.makingTask.get(event.getAuthor().getIdLong()) != null) {
                Logic l = (Logic) this.makingTask.get(event.getAuthor().getIdLong())[2];
                Task t = (Task) this.makingTask.get(event.getAuthor().getIdLong())[1];
                if ((boolean) this.makingTask.get(event.getAuthor().getIdLong())[0]) {
                    t.setName(message);
                } else {
                    if (message.length() > 100) {
                        Utils.errorEmbed(
                                event.getMessage(),
                                "Description cannot be longer then 100 characters!"
                        );
                        this.makingTask.remove(event.getAuthor().getIdLong());
                        return;
                    }
                    t.setDescription(message);
                }

                l.setTask(t);
                this.makingTask.remove(event.getAuthor().getIdLong());
                Utils.successEmbed(event.getMessage(), "Task details have been updated. Press save to finish.");
            }

            //create project button listener
            if (this.makingProject.get(event.getAuthor().getIdLong()) != null) {
                Project proj = new Project(args[0], event.getAuthor().getIdLong());
                Logic.updateProjects(proj);
                this.makingProject.remove(event.getAuthor().getIdLong());
                Utils.successEmbed(event.getMessage(), "Project details have been updated");
            }

            if (event.getMessage().getMentionedUsers().size() == 1 && !event.getAuthor().isBot()) {
                if (event.getMessage().getMentionedUsers().get(0).getIdLong() == 960287864430682112L) {

                    Logic logic = new Logic(
                            null,
                            this.utils,
                            event.getMessage(),
                            this
                    );
                    Key key = new Key("card id: " + event.getMessage().getIdLong(), event.getAuthor().getIdLong());
                    this.hashMap.put(
                            key,
                            logic
                    );

                    SelectMenu.Builder sm = SelectMenu.create("projectMenu").setRequiredRange(1, 1).setPlaceholder("project");
                    for (Project p : projects)
                        sm.addOption(p.getName(), p.getName());
                    event.getMessage().replyEmbeds(
                            new EmbedBuilder().setFooter("card id: " + event.getMessage().getIdLong())
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
            }

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
                Logic logic = new Logic(
                        project,
                        this.utils,
                        event.getMessage(),
                        this
                );
                if (args.length == 1) {
                    logic.sendProjectEmbed(event, false);
                    Key key = new Key("card id: " + event.getMessage().getIdLong(), event.getAuthor().getIdLong());
                    this.hashMap.put(
                            key,
                            logic
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
            Logic logic = this.hashMap.get(key);

            switch (Objects.requireNonNull(event.getButton().getId())) {
                case "home" -> logic.home(event);
                case "help" -> logic.helpEmbed(event);
                case "showProject" -> logic.sendProjectEmbed(event, false);
                case "join" -> logic.joinProject(event);
                case "leave" -> logic.leaveProject(event);
                case "delete" -> logic.confirmDelete(event);
                case "delYes" -> logic.deleteProject(event);
                case "edit" -> logic.editMenu(event);
                case "editDescription" -> logic.editDescription(event);
                case "editOwner" -> logic.editOwner(event);
                case "cancelEdit" -> {
                    this.editingOwner.remove(event.getUser().getIdLong());
                    logic.sendProjectEmbed(event, false);
                }
                case "editPrivate" -> logic.editPrivate(event);
                case "privYes" -> logic.privateYes(event);
                case "privNo" -> logic.privateNo(event);
                case "createProject" -> logic.createProject(event);
                case "exitNewProject" -> {
                    this.makingProject.remove(event.getUser().getIdLong());
                    logic.home(event);
                }
                //tasks
                case "tasks" -> logic.showTasks(event);
                case "newTask" -> logic.newTask(event);
                case "editTask" -> logic.editTask(event);
                case "deleteTask" -> logic.deleteTask(event);
                case "save" -> logic.save(event, false);
                case "saveEdit" -> logic.save(event, true);
                case "taskName" -> logic.setTaskName(event);
                case "taskDescription" -> logic.setTaskDescription(event);
                case "cancelTaskEdit" -> logic.cancelTaskEdit(event);
                case "progress" -> logic.progress(event);
                case "regress" -> logic.regress(event);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            event.reply("Sorry, you dont have permission to use that!").setEphemeral(true).queue();
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    @Override
    public void onSelectMenuInteraction(@Nonnull SelectMenuInteractionEvent event) {
        try {
            if (event.getComponentId().equals("taskMenu")) {
                Key key = new Key(
                        Objects.requireNonNull(event.getMessage().getEmbeds().get(0).getFooter()).getText(),
                        event.getUser().getIdLong()
                );
                Logic logic = this.hashMap.get(key);
                boolean exists = false;
                for (Task t : logic.getProject().getTasks()) {
                    if (t.getName().equals(event.getInteraction().getSelectedOptions().get(0).getLabel())) {
                        exists = true;
                        logic.setTask(t);
                        break;
                    }
                }
                if (!exists) return;
                logic.sendTaskEmbed(event, logic.getTask());
            } else {
                Project project = null;

                for (Project p : Project.getProjects("projects.json")) {
                    if (Objects.equals(event.getInteraction().getSelectedOptions().get(0).getLabel(), p.getName())) {
                        project = p;
                        break;
                    }
                }

                Key key = new Key(
                        Objects.requireNonNull(event.getMessage().getEmbeds().get(0).getFooter()).getText(),
                        event.getUser().getIdLong()
                );
                Logic logic = this.hashMap.get(key);
                logic.setProject(project);
                logic.sendProjectEmbed(event, false);
                this.hashMap.remove(key);
                this.hashMap.put(
                        key,
                        logic
                );
            }

        } catch (NullPointerException e) {
            e.printStackTrace();
            event.reply("Sorry, that ones not yours!").setEphemeral(true).queue();
        } catch (Exception e) {
            utils.exceptionEmbed(event.getMessage(), e);
        }
    }

    public record Key(String embedTitle, Long userID) {

        @Override
        public String toString() {
            return "Key{" +
                    "embedTitle='" + embedTitle + '\'' +
                    ", userID=" + userID +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Key key)) return false;
            return Objects.equals(embedTitle, key.embedTitle) && Objects.equals(userID, key.userID);
        }

        @Override
        public int hashCode() {
            int result = embedTitle.hashCode();
            result = 31 * result + userID.hashCode();
            return result;
        }
    }
}
