package fr.rolenametag.mixin;

import fr.rolenametag.RoleConfig;
import fr.rolenametag.ServerTagRoles;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Nametags "text display" du serveur : ajoute le logo + les fait passer devant les objets. */
@Mixin(DisplayEntity.TextDisplayEntity.class)
public abstract class TextDisplayEntityMixin {
    @Shadow
    protected abstract Text getText();

    /** Ajoute le logo de rôle dans le texte. */
    @Inject(method = "getText()Lnet/minecraft/text/Text;", at = @At("RETURN"), cancellable = true)
    private void rolenametag$addRole(CallbackInfoReturnable<Text> cir) {
        Text modified = ServerTagRoles.process((Entity) (Object) this, cir.getReturnValue());
        if (modified != null) cir.setReturnValue(modified);
    }

    /** Toujours actif : drapeau "see through" (2), le pseudo ne passe plus derrière les cadres / blocs. */
    @Inject(method = "getDisplayFlags()B", at = @At("RETURN"), cancellable = true)
    private void rolenametag$seeThrough(CallbackInfoReturnable<Byte> cir) {
        RoleConfig cfg = RoleConfig.INSTANCE;
        if (!cfg.enabled) return;
        try {
            if (ServerTagRoles.match((Entity) (Object) this, this.getText()) != null) {
                cir.setReturnValue((byte) (cir.getReturnValue() | 2));
            }
        } catch (Throwable ignored) {
        }
    }
}
