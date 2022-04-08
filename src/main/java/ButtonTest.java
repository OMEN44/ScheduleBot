import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Objects;

public class ButtonTest extends ListenerAdapter {

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equalsIgnoreCase("$button")) {
            event.getChannel().sendMessage("I have a button '-'").setActionRow(
                    Button.primary("success", "HUZZAH!"),
                    Button.danger("danger", ":skull: press me im spooky")
            ).queue();
        }
    }

    @Override
    public void onButtonInteraction(@Nonnull ButtonInteractionEvent event) {
        if (Objects.equals(event.getButton().getId(), "success")) {
            event.reply("Let's f**king go!! you pressed the button!").queue();
            event.editButton(Button.secondary("success", "HUZZAH!").asDisabled()).queue();
        } else if (Objects.equals(event.getButton().getId(), "danger")) {
            event.reply(":imp: I'm inside your house :upside_down:").queue();
            event.editButton(Button.secondary("danger", "press me im spooky").asDisabled()).queue();
        }
    }
}
