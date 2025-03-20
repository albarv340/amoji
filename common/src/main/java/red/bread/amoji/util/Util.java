package red.bread.amoji.util;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class Util {
    public static boolean isKeyDown(int keyCode) {
        return GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), keyCode) == 1;
    }

    public static boolean isShiftDown() {
        return isKeyDown(GLFW.GLFW_KEY_LEFT_SHIFT) || isKeyDown(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
