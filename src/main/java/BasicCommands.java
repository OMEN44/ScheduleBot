import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;

public class BasicCommands extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMessage().getContentRaw().equalsIgnoreCase("*ping"))
            event.getMessage().reply("Ping: very fast :smirk:").queue(message -> {
                long ping = event.getMessage().getTimeCreated().until(message.getTimeCreated(), ChronoUnit.MILLIS);
                message.editMessage("ping: " + ping).queue();
            });
    }
}
