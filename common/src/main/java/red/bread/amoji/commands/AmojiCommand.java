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
                            .map(entry -> "§b" + entry.getKey() + "§6:§7 " + entry.getValue())
                            .collect(Collectors.joining("\n "))), false);
                    return 1;
                })
                .then(Commands.literal("add")
                        .then(Commands.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    new Thread(() -> {
                                        String[] words = context.getArgument("name", MessageArgument.Message.class).getText().split("\\s+");
                                        if (words.length < 2) {
                                            context.getSource().sendFailure(Component.literal("§cMissing arguments"));
                                            return;
                                        }
                                        String name = String.join(" ", Arrays.copyOfRange(words, 0, words.length - 1));
                                        String url = words[words.length - 1];
                                        if (!WebUtils.isValidUrl(url)) {
                                            context.getSource().sendFailure(Component.literal("§cInvalid URL. Make sure to include https"));
                                            return;
                                        }

                                        context.getSource().sendSuccess(() -> Component.literal("§7Attempting to add emoji source..."), false);

                                        int amount;
                                        try {
                                            amount = ClientEmojiHandler.loadCustomEmojis(name, url);
                                        } catch (Exception e) {
                                            context.getSource().sendFailure(Component.literal("§cFailed to load emojis from URL. See logs for more info."));
                                            Constants.LOG.error("Error trying to load custom emoji url", e);
                                            return;
                                        }
                                        ClientEmojiHandler.addCustomEmojiSource(name, url);
                                        context.getSource().sendSuccess(() -> Component.literal("§7Successfully added §b" + amount + "§7 emojis from custom source §b" + name + " §7with url §b" + url), false);
                                    }).start();

                                    return 1;
                                })
                                .then(Commands.argument("url", MessageArgument.message()))
                        )
                ).then(Commands.literal("remove")
                        .then(Commands.argument("name", MessageArgument.message())
                                .executes(context -> {
                                    String name = context.getArgument("name", MessageArgument.Message.class).getText();
                                    if (Constants.CUSTOM_SOURCES.containsKey(name)) {
                                        Constants.CUSTOM_SOURCES.remove(name);
                                        context.getSource().sendSuccess(() -> Component.literal("§7Successfully removed custom source §b" + name), false);
                                        context.getSource().sendSuccess(() -> Component.literal("§7Its emojis will be gone at next start up"), false);
                                    }
                                    return 1;
                                })
                        )
                )
        );
    }
}
