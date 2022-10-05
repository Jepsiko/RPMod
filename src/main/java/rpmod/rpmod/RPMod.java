package rpmod.rpmod;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.*;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class RPMod implements ModInitializer {

    private static File file;
    private static Map<String, String> name_table;

    private static final String help_text = """
        /rp help : show this help
        /rp [MESSAGE] : broadcast a message in roleplay mode
        /rp action [ACTION] : broadcast an action in roleplay mode
        /rp name set [NAME] : set your roleplaying name
        /rp name get : show your current roleplaying name
        /rp name clear : remove your roleplaying name
        /rp show_name_table : show usernames and corresponding roleplaying names
        """;

    @Override
    public void onInitialize() {
        file = new File("rpmod/name_table.txt");
        file.getParentFile().mkdirs();
        try {
            if (!file.createNewFile()) { // File already exists
                importNameTable();
            }
            else name_table = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
        }

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
        dispatcher.register(
        literal("rp").executes(context ->
            noArgs(context.getSource()))
            .then(literal("help").executes(context ->
                showHelp(context.getSource())))
            .then(literal("name")
                .then(literal("set")
                    .then(argument("name", string()).executes(context ->
                        setRPName(context.getSource(), getString(context, "name")))))
                .then(literal("get").executes(context ->
                    getRPName(context.getSource())))
                .then(literal("clear").executes(context ->
                    clearRPName(context.getSource()))))
            .then(literal("show_name_table").executes(context ->
                showNameTable(context.getSource())))
            .then(literal("action")
                .then(argument("action", string()).executes(context ->
                    broadcastAction(context.getSource(), getString(context, "action")))))
            .then(argument("message", string()).executes(context ->
                broadcastMessage(context.getSource(), getString(context, "message"))))));
    }

    public void importNameTable() throws IOException {
        name_table = new HashMap<>();
        BufferedReader buffer = new BufferedReader(new FileReader(file));

        String line;
        while ((line = buffer.readLine()) != null) {
            String[] parts = line.strip().split("-");
            String username = parts[0];
            String rp_name = parts[1];
            name_table.put(username, rp_name);
        }
    }

    public void exportNameTable() throws IOException {
        FileWriter writer = new FileWriter(file);
        for (Map.Entry<String, String> entry : name_table.entrySet()) {
            writer.write(entry.getKey() + "-" + entry.getValue() + "\n");
        }
        writer.close();
    }

    public int noArgs(ServerCommandSource source) {
        source.sendMessage(Text.literal("Called /rp with no arguments. Try /rp help."));

        return Command.SINGLE_SUCCESS;
    }

    public int showHelp(ServerCommandSource source) {
        source.sendMessage(Text.literal(help_text));

        return Command.SINGLE_SUCCESS;
    }

    public int showNameTable(ServerCommandSource source) {
        source.sendMessage(Text.literal("<-----[ NAME TABLE ]----->"));
        for (Map.Entry<String, String> entry : name_table.entrySet()) {
            source.sendMessage(Text.literal(entry.getKey() + ": ")
                    .append(Text.literal(entry.getValue()).formatted(Formatting.DARK_AQUA)));
        }

        return Command.SINGLE_SUCCESS;
    }

    public int setRPName(ServerCommandSource source, String rp_name) {
        if (rp_name.length() < 3) source.sendMessage(Text.literal("The name must contain at least 3 characters").formatted(Formatting.DARK_RED));
        else if (rp_name.length() > 50) source.sendMessage(Text.literal("The name must contain at most 50 characters").formatted(Formatting.DARK_RED));
        else if (rp_name.contains("-"))  source.sendMessage(Text.literal("The name cannot contain the character '-'").formatted(Formatting.DARK_RED));
        else {
            name_table.put(source.getName(), rp_name);
            source.sendMessage(Text.literal("Your roleplaying name is set to ")
                    .append(Text.literal(rp_name).formatted(Formatting.DARK_AQUA)));
            try {
                exportNameTable();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return Command.SINGLE_SUCCESS;
    }

    public int getRPName(ServerCommandSource source) {
        if (!name_table.containsKey(source.getName()))
            source.sendMessage(Text.literal("You don't have any roleplaying name").formatted(Formatting.DARK_RED));
        else source.sendMessage(Text.literal("Your current roleplaying name is ")
                .append(Text.literal(name_table.get(source.getName())).formatted(Formatting.DARK_AQUA)));

        return Command.SINGLE_SUCCESS;
    }

    public int clearRPName(ServerCommandSource source) {
        if (name_table.containsKey(source.getName())) {
            name_table.remove(source.getName());
            source.sendMessage(Text.literal("Roleplaying name cleared"));
            try {
                exportNameTable();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            source.sendMessage(Text.literal("Roleplaying name already cleared"));
        }

        return Command.SINGLE_SUCCESS;
    }

    public int broadcastMessage(ServerCommandSource source, String message) {
        String username = source.getName();
        String name = name_table.getOrDefault(username, username);

        final Text text = Text.literal("<" + name + "> ").formatted(Formatting.DARK_AQUA)
                .append(Text.literal(message).formatted(Formatting.WHITE));
        source.getServer().getPlayerManager().broadcast(text, false);

        return Command.SINGLE_SUCCESS;
    }

    public int broadcastAction(ServerCommandSource source, String action) {
        String username = source.getName();
        String name = name_table.getOrDefault(username, username);

        final Text text = Text.literal("*" + name + " " + action + "*").formatted(Formatting.LIGHT_PURPLE);
        source.getServer().getPlayerManager().broadcast(text, false);

        return Command.SINGLE_SUCCESS;
    }
}
