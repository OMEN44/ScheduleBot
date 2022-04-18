import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;

public class Main {

    private static final Utils utils = new Utils();

    public static void main(String[] args) throws LoginException, InterruptedException {
        JDABuilder.createDefault("OTYwMjg3ODY0NDMwNjgyMTEy.YkoPxA.stiAQjEPlUX7lDfem87d-SReeVM")
                .setActivity(Activity.watching("The infinite march of time pass us by."))
                .setStatus(OnlineStatus.IDLE)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(
                        new Listeners(utils),
                        utils
                )
                .build().awaitReady();
    }
}
