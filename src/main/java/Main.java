import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;

import javax.security.auth.login.LoginException;

public class Main {

    private static final Utils utils = new Utils();
    private static JDA jda;

    public static void main(String[] args) throws LoginException, InterruptedException {
        jda = JDABuilder.createDefault("OTYwMjg3ODY0NDMwNjgyMTEy.YkoPxA.5pE6e1CpDGuser_n2W0NDWmdj4M")
                .setActivity(Activity.watching("The infinite march of time pass us by."))
                .setStatus(OnlineStatus.IDLE)
                .addEventListeners(
                        new BasicCommands(),
                        new ButtonTest(),
                        new CreateCommand(utils),
                        new ProjectCommand(utils),
                        new Config(),
                        utils
                )
                .build().awaitReady();



        /*Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            System.out.println("1");
            Reader newReader = Files.newBufferedReader(Paths.get("test.json"));
            List<Project> projects1 = gson.fromJson(newReader, new TypeToken<List<Project>>() {}.getType());
            newReader.close();
            if (projects1 == null)
                projects1 = new ArrayList<>();
            System.out.println("try | " *//*+ projects1.size()*//*);

            Project project = new Project("toot");
            projects1.add(project);
            System.out.println(projects1.size());
            System.out.println("2");

            Writer newWriter = Files.newBufferedWriter(Paths.get("test.json"));
            gson.toJson(projects1, newWriter);
            newWriter.close();
            System.out.println("done");
        } catch (NullPointerException | IOException e) {
            System.out.println("catch");
            e.printStackTrace();
        }*/

        /*jda.retrieveUserById(577382380763873280L)
                .map(User::getName)
                .queue(name -> {
                    // use name here
                    System.out.println("The user has the name " + name);
                });*/
    }

    public static JDA getJda() {
        return jda;
    }
}
