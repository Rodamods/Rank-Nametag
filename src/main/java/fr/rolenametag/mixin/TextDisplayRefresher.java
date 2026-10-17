package fr.rolenametag.mixin;

import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Permet de recalculer le texte des nametags quand on change un réglage. */
@Mixin(DisplayEntity.TextDisplayEntity.class)
public interface TextDisplayRefresher {
    @Invoker("refreshData")
    void rolenametag$refreshData(boolean shouldLerp, float lerpProgress);
}
