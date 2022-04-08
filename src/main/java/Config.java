import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public class Config extends ListenerAdapter {
    private String path;
    private boolean restrictUsers;
    private List<Long> users;
}
