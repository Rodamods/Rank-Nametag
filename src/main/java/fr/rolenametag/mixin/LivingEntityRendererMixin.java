package fr.rolenametag.mixin;

import fr.rolenametag.RoleConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Affiche ton propre nametag en 3ème personne. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true)
    private void rolenametag$ownLabel(LivingEntity entity, double squaredDistance, CallbackInfoReturnable<Boolean> cir) {
        RoleConfig cfg = RoleConfig.INSTANCE;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (cfg.enabled && cfg.showOnSelf
                && entity == mc.player
                && !mc.options.getPerspective().isFirstPerson()
                && MinecraftClient.isHudEnabled()
                && !entity.isInvisible()) {
            cir.setReturnValue(true);
        }
    }
}
