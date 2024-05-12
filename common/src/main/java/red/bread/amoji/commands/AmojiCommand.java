package red.bread.amoji.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import red.bread.amoji.ClientEmojiHandler;
import red.bread.amoji.Constants;
import red.bread.amoji.util.WebUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class AmojiCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(Commands.literal("amoji")
                .executes(context -> {
                    context.getSource().sendSuccess(() -> Component.literal("§aCurrently added custom emoji sources:"), false);
                    context.getSource().sendSuccess(() -> Component.literal(Constants.CUSTOM_SOURCES.entrySet().stream()
                            .map(entry -> "§b" +entry.getKey() + "§6:§7 " + entry.getValue())
                            .collect(Collectors.joining("\n "))), false);
                    return 1;
                })
                .then(Commands.literal("add")
                        .then(Commands.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    String[] words = MessageArgument.getMessage(context, "name").getString().split("\\s+");
                                    if (words.length < 2) {
                                        context.getSource().sendFailure( Component.literal("Missing arguments"));
                                        return 0;
                                    }
                                    String name = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                                    String url = words[words.length - 1];
                                    if (!WebUtils.isValidUrl(url)) {
                                        context.getSource().sendFailure(Component.literal("Invalid URL. Make sure to include https"));
                                        return 0;
                                    }
                                    try{
                                        WebUtils.readJsonFromUrl(url);
                                    } catch (Exception e) {
                                        context.getSource().sendFailure(Component.literal("Malformed JSON returned from URL"));
                                        return 0;
                                    }
                                    ClientEmojiHandler.addCustomEmojiSource(name, url);
                                    ClientEmojiHandler.loadCustomEmojis();
                                    context.getSource().sendSuccess(() -> Component.literal("§7Successfully added custom source §b" + name + " §7with url §b" + url), false);
                                    return 1;
                                })
                                .then(Commands.argument("url", MessageArgument.message()))
                        )
                )
        );
    }
}
