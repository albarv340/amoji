package red.bread.amoji.commands;

import com.mojang.brigadier.CommandDispatcher;
import dev.architectury.event.events.client.ClientCommandRegistrationEvent;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import red.bread.amoji.ClientEmojiHandler;
import red.bread.amoji.Constants;
import red.bread.amoji.util.WebUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class AmojiCommand {
    public static void register(CommandDispatcher<ClientCommandRegistrationEvent.ClientCommandSourceStack> commandDispatcher) {
        commandDispatcher.register(ClientCommandRegistrationEvent.literal("amoji")
                .executes(context -> {
                    context.getSource().arch$sendSuccess(() -> Component.literal("§aCurrently added custom emoji sources:"), false);
                    context.getSource().arch$sendSuccess(() -> Component.literal(Constants.CUSTOM_SOURCES.entrySet().stream()
                            .map(entry -> "§b" +entry.getKey() + "§6:§7 " + entry.getValue())
                            .collect(Collectors.joining("\n "))), false);
                    return 1;
                })
                .then(ClientCommandRegistrationEvent.literal("add")
                        .then(ClientCommandRegistrationEvent.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    String[] words = context.getArgument("name", String.class).split("\\s+");
//                                    MessageArgument.getMessage(, "name").getString().split("\\s+");
                                    if (words.length < 2) {
                                        context.getSource().arch$sendFailure( Component.literal("Missing arguments"));
                                        return 0;
                                    }
                                    String name = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                                    String url = words[words.length - 1];
                                    if (!WebUtils.isValidUrl(url)) {
                                        context.getSource().arch$sendFailure(Component.literal("Invalid URL. Make sure to include https"));
                                        return 0;
                                    }
                                    try{
                                        WebUtils.readJsonFromUrl(url);
                                    } catch (Exception e) {
                                        context.getSource().arch$sendFailure(Component.literal("Malformed JSON returned from URL"));
                                        return 0;
                                    }
                                    ClientEmojiHandler.addCustomEmojiSource(name, url);
                                    ClientEmojiHandler.loadCustomEmojis();
                                    context.getSource().arch$sendSuccess(() -> Component.literal("§7Successfully added custom source §b" + name + " §7with url §b" + url), false);
                                    return 1;
                                })
                                .then(ClientCommandRegistrationEvent.argument("url", MessageArgument.message()))
                        )
                )
        );
    }
}
