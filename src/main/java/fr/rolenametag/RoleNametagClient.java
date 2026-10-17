package fr.rolenametag;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.rolenametag.gui.RoleConfigScreen;
import fr.rolenametag.mixin.TextDisplayRefresher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class RoleNametagClient implements ClientModInitializer {
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        RoleConfig.load();

        // Touche modifiable dans Options > Contrôles > Role Nametag (par défaut : Maj droite)
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rolenametag.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("rolenametag", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new RoleConfigScreen(null));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(ClientCommandManager.literal("rolenametag")
                .then(ClientCommandManager.literal("toggle").executes(ctx -> {
                    RoleConfig.INSTANCE.enabled = !RoleConfig.INSTANCE.enabled;
                    RoleConfig.save();
                    refreshTags();
                    ctx.getSource().sendFeedback(Text.literal("RoleNametag : " + (RoleConfig.INSTANCE.enabled ? "activé" : "désactivé")));
                    return 1;
                }))
                .then(ClientCommandManager.literal("reset").executes(ctx -> {
                    RoleConfig.INSTANCE.glyphRoles.clear();
                    RoleConfig.save();
                    refreshTags();
                    ctx.getSource().sendFeedback(Text.literal("RoleNametag : logos appris effacés"));
                    return 1;
                }))
                .then(ClientCommandManager.literal("dump").executes(ctx -> {
                    ClientPlayNetworkHandler net = MinecraftClient.getInstance().getNetworkHandler();
                    if (net == null) return 0;
                    for (PlayerListEntry e : net.getPlayerList()) {
                        String raw = RoleDetector.tabText(e, "?").getString();
                        StringBuilder sb = new StringBuilder();
                        long n = raw.codePoints().filter(RoleDetector::isGlyph).count();
                        sb.append("(").append(n).append(" logo").append(n > 1 ? "s" : "").append(") ");
                        raw.codePoints().forEach(cp -> {
                            if (RoleDetector.isGlyph(cp)) sb.append(String.format("[U+%04X]", cp));
                            else sb.appendCodePoint(cp);
                        });
                        ctx.getSource().sendFeedback(Text.literal(sb.toString()));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("map")
                    .then(ClientCommandManager.argument("hex", StringArgumentType.word())
                        .then(ClientCommandManager.argument("role", StringArgumentType.word()).executes(ctx -> {
                            String hex = StringArgumentType.getString(ctx, "hex").toUpperCase(Locale.ROOT);
                            String role = StringArgumentType.getString(ctx, "role").toUpperCase(Locale.ROOT);
                            RoleConfig.INSTANCE.glyphRoles.put(hex, role);
                            RoleConfig.save();
                            refreshTags();
                            ctx.getSource().sendFeedback(Text.literal("U+" + hex + " -> " + role));
                            return 1;
                        }))))));
    }

    /** Recalcule le texte des nametags du serveur après un changement de réglage. */
    public static void refreshTags() {
        try {
            RoleDetector.clearCache();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null) return;
            for (Entity e : mc.world.getEntities()) {
                Object o = e;
                if (o instanceof TextDisplayRefresher r) r.rolenametag$refreshData(false, 1.0f);
            }
        } catch (Throwable ignored) {
        }
    }
}
