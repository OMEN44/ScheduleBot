import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class CreateCommand extends ListenerAdapter {

    private final Utils utils;

    public CreateCommand(Utils utils) {
        this.utils = utils;
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        try {
            String message = event.getMessage().getContentRaw();
            boolean hasPrefix = message.startsWith("*");
            String[] args = message.split(" ");
            String fileName = event.getGuild().getId() +
                    event.getGuild().getName().replace(" ", "_") +
                    ".json";
            List<Project> projects = Project.getProjects(fileName);

            //check if file is created
            if (!Files.exists(Path.of(fileName))) {
                File file = new File(fileName);
                if (!file.createNewFile()) {
                    Utils.errorEmbed(event.getMessage(),
                            "Unable to make file please contact server admin immediately!");
                }
            }


            if (hasPrefix) {
                switch (args[0].toLowerCase()) {
                    case "*new" -> {
                        if (args.length < 2) {
                            Utils.errorEmbed(event.getMessage(),
                                    "Not enough arguments provided\n`*new [project_name]`");
                            return;
                        }
                        if (args[1].equalsIgnoreCase("new") ||
                                args[1].equalsIgnoreCase("help") ||
                                args[1].equalsIgnoreCase("list")) {
                            Utils.errorEmbed(event.getMessage(), "Illegal project name used!");
                            return;
                        }
                        try {
                            if (Project.projectExist(args[1], fileName)) {
                                Utils.errorEmbed(event.getMessage(),
                                        "A project with this name already exists!\n" +
                                                "Think of a new name and try again.");
                                return;
                            }

                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            Project project = new Project(args[1], event.getAuthor().getIdLong());
                            projects.add(project);

                            Writer newWriter = Files.newBufferedWriter(Paths.get(fileName));
                            gson.toJson(projects, newWriter);
                            newWriter.close();
                        } catch (NullPointerException | IOException e) {
                            e.printStackTrace();
                        }
                        Utils.successEmbed(event.getMessage(), "Created project: `" + args[1] + "`");
                    }
                    case "*list" -> {
                        StringBuilder list = new StringBuilder();
                        if (projects.isEmpty()) {
                            Utils.errorEmbed(event.getMessage(), "No projects found!\n" +
                                    "Use `*new [project_name]` to create one.");
                            return;
                        } else {
                            for (Project proj : projects)
                                list.append(proj.getName()).append("\n");
                        }
                        event.getMessage().replyEmbeds(
                                new EmbedBuilder()
                                        .addField("SUCCESS!", "Projects: \n`" + list + "`", false)
                                        .setColor(Color.GREEN)
                                        .setFooter("Showing " + projects.size() + " projects.")
                                        .build()
                        ).queue();
                    }
                }
            }
        } catch (Exception e) {
            this.utils.exceptionEmbed(event.getMessage(), e);
        }
    }
}