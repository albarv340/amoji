package red.bread.amoji.fabric.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import red.bread.amoji.ClientEmojiHandler;
import red.bread.amoji.Constants;
import red.bread.amoji.util.WebUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class AmojiCommandFabric {
    public static void register(CommandDispatcher<FabricClientCommandSource> commandDispatcher) {
        commandDispatcher.register(ClientCommandManager.literal("amoji")
                .executes(context -> {
                    context.getSource().sendFeedback(Component.literal("§aCurrently added custom emoji sources:"));
                    context.getSource().sendFeedback(Component.literal(Constants.CUSTOM_SOURCES.entrySet().stream()
                            .map(entry -> "§b" + entry.getKey() + "§6:§7 " + entry.getValue())
                            .collect(Collectors.joining("\n "))));
                    return 1;
                })
                .then(ClientCommandManager.literal("add")
                        .then(ClientCommandManager.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    String[] words = context.getArgument("name", MessageArgument.Message.class).getText().split("\\s+");
                                    if (words.length < 2) {
                                        context.getSource().sendFeedback(Component.literal("Missing arguments"));
                                        return 0;
                                    }
                                    String name = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                                    String url = words[words.length - 1];
                                    if (!WebUtils.isValidUrl(url)) {
                                        context.getSource().sendFeedback(Component.literal("Invalid URL. Make sure to include https"));
                                        return 0;
                                    }
                                    try {
                                        WebUtils.readJsonFromUrl(url);
                                    } catch (Exception e) {
                                        context.getSource().sendFeedback(Component.literal("Malformed JSON returned from URL"));
                                        return 0;
                                    }
                                    ClientEmojiHandler.addCustomEmojiSource(name, url);
                                    ClientEmojiHandler.loadCustomEmojis();
                                    context.getSource().sendFeedback(Component.literal("§7Successfully added custom source §b" + name + " §7with url §b" + url));
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("url", MessageArgument.message()))
                        )
                )
        );
    }
}
