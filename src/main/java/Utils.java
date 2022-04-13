import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Utils extends ListenerAdapter {

    private Exception exception;

    public static void successEmbed(Message message, String successMessage) {
        message.replyEmbeds(
                new EmbedBuilder().addField("SUCCESS!", successMessage, false).setColor(Color.GREEN).build()
        ).queue();
    }

    public static void errorEmbed(Message message, String errorMessage) {
        message.replyEmbeds(
                new EmbedBuilder().addField("ERROR!", errorMessage, false).setColor(Color.RED).build()
        ).queue();
    }

    public void exceptionEmbed(Message message, Exception e) {
        this.exception = e;
        String content;
        if (e.getMessage() == null)
            content = "`No cause was specified`";
        else
            content = "`" + e.getMessage() + "`";
        message.replyEmbeds(
                new EmbedBuilder().addField(
                        "Uncaught exception: \n",
                        content,
                        false).setColor(Color.RED).setTitle("ERROR").build()
        ).setActionRow(
                Button.secondary("showStack", "Show stack message"),
                Button.secondary("prntStack", "Print in terminal")
        ).queue();
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        switch (Objects.requireNonNull(event.getButton().getId())) {
            case "showStack" -> {
                if (this.exception == null) {
                    System.out.println("1");
                    event.editButton(Button.secondary("showStack", "Show stack message").asDisabled()).queue();
                    return;
                }
                try {
                    event.editMessage(Arrays.toString(this.exception.getStackTrace())).queue();
                } catch (IllegalArgumentException e) {
                    event.editMessage("`Stack is to large to be displayed`").queue();
                    event.editButton(Button.secondary("hideStack", "Hide stack message").asDisabled()).queue();
                    return;
                }
                event.editButton(Button.secondary("hideStack", "Hide stack message")).queue();
            }
            case "hideStack" -> {
                event.editMessage("").queue();
                event.editButton(Button.secondary("showStack", "Show stack message")).queue();
            }
            case "prntStack" -> {
                if (this.exception == null) {
                    event.editButton(Button.secondary("prntStack", "Print in terminal").asDisabled()).queue();
                    return;
                }
                this.exception.printStackTrace();
                event.reply("Success").setEphemeral(true).queue();
                event.editButton(Button.secondary("prntStack", "Print in terminal").asDisabled()).queue();
            }
        }
    }

    public static List<String> getNames(Guild guild, List<Long> idList) {
        guild.loadMembers();
        List<String> names = new ArrayList<>();
        for (Member member : guild.getMembers()) {
            System.out.println(member.getEffectiveName());
            if (idList.contains(member.getIdLong()))
                names.add(member.getEffectiveName());
        }
        return names;
    }
}
