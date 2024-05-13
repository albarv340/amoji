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
                                    new Thread(() -> {
                                        String[] words = context.getArgument("name", MessageArgument.Message.class).getText().split("\\s+");
                                        if (words.length < 2) {
                                            context.getSource().sendFeedback(Component.literal("§cMissing arguments"));
                                            return;
                                        }
                                        String name = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                                        String url = words[words.length - 1];
                                        if (!WebUtils.isValidUrl(url)) {
                                            context.getSource().sendFeedback(Component.literal("§cInvalid URL. Make sure to include https"));
                                            return;
                                        }
                                        context.getSource().sendFeedback(Component.literal("§7Attempting to add emoji source..."));
                                        int amount;
                                        try {
                                            amount = ClientEmojiHandler.loadCustomEmojis(name, url);
                                        } catch (Exception e) {
                                            context.getSource().sendFeedback(Component.literal("§cFailed to load emojis from URL. See logs for more info."));
                                            Constants.LOG.error("Error trying to load custom emoji url", e);
                                            return;
                                        }
                                        ClientEmojiHandler.addCustomEmojiSource(name, url);
                                        context.getSource().sendFeedback(Component.literal("§7Successfully added §b" + amount + "§7 emojis from custom source §b" + name + " §7with url §b" + url));
                                    }).start();
                                    return 1;
                                })
                                .then(ClientCommandManager.argument("url", MessageArgument.message()))
                        )
                ).then(ClientCommandManager.literal("remove")
                        .then(ClientCommandManager.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    String name = context.getArgument("name", MessageArgument.Message.class).getText();
                                    if (Constants.CUSTOM_SOURCES.containsKey(name)) {
                                        Constants.CUSTOM_SOURCES.remove(name);
                                        context.getSource().sendFeedback(Component.literal("§7Successfully removed custom source §b" + name));
                                        context.getSource().sendFeedback(Component.literal("§7Its emojis will be gone at next start up"));
                                    }
                                    return 1;
                                })
                        )
                )

        );
    }
}
