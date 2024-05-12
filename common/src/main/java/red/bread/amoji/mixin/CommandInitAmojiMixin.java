package red.bread.amoji.mixin;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.bread.amoji.commands.AmojiCommand;

@Mixin(Commands.class)
public abstract class CommandInitAmojiMixin {
    @Shadow
    @Final
    private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void construct(Commands.CommandSelection environment, CommandBuildContext registryAccess, CallbackInfo info) {
        AmojiCommand.register(this.dispatcher);
    }
}
