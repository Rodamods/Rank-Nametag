package fr.rolenametag.mixin;

import fr.rolenametag.Role;
import fr.rolenametag.RoleConfig;
import fr.rolenametag.RoleDetector;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Nametag vanilla : retire les logos du serveur (anti-superposition) puis place nos icônes avant le pseudo. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/render/entity/state/EntityRenderState;F)V",
            at = @At("TAIL"))
    private void rolenametag$icons(Entity entity, EntityRenderState state, float tickDelta, CallbackInfo ci) {
        RoleConfig cfg = RoleConfig.INSTANCE;
        if (!cfg.enabled) return;
        if (!(entity instanceof PlayerEntity) || state.displayName == null) return;

        Role role = RoleDetector.detect(entity.getUuid(), entity.getName().getString());
        if (role == null) return;

        boolean self = entity == MinecraftClient.getInstance().player;
        MutableText icon = cfg.iconFor(role, self);
        // autres joueurs : on ne touche à rien (les logos du serveur restent) sauf pour ajouter le losange des membres
        if (!self && icon == null) return;

        Text clean = RoleDetector.stripServerGlyphs(state.displayName);
        if (clean.getString().isBlank()) clean = Text.literal(entity.getName().getString());

        MutableText result = Text.empty(); // racine vide : le pseudo ne prend pas la police des icônes
        if (icon != null) result.append(icon);
        result.append(clean);
        state.displayName = result;
    }
}
