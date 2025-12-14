package lol.sylvie.nocombatelytra.mixin;

import com.mojang.authlib.GameProfile;
import lol.sylvie.nocombatelytra.NoCombatElytra;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow public abstract GameProfile getGameProfile();
    @Shadow public abstract void displayClientMessage(Component message, boolean overlay);

    @Unique
    Map<UUID, Long> lastDamaged = new HashMap<>();

    @Unique
    private static boolean isCombat(DamageSource source) {
        Entity entity = source.getEntity();
        if (entity == null) return false;
        if (NoCombatElytra.CONFIG.get().mobDamage()) return entity.isAlive();
        return source.is(DamageTypeTags.IS_PLAYER_ATTACK) || entity.isAlwaysTicking();
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("TAIL"))
    public void nocombatelytra$storeLastDamaged(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Player thisPlayer = (Player) (Object) this;
        if (thisPlayer.equals(source.getEntity())) return;

        if (isCombat(source)) {
            UUID uuid = getGameProfile().id();

            lastDamaged.put(uuid, System.currentTimeMillis());
            thisPlayer.stopFallFlying();
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    public void nocombatelytra$resetTimerOnDeath(Entity.RemovalReason reason, CallbackInfo ci) {
        UUID uuid = getGameProfile().id();
        lastDamaged.remove(uuid);
    }

    @Inject(method = "tryToStartFallFlying", at = @At("HEAD"), cancellable = true)
    public void nocombatelytra$addDamageCheck(CallbackInfoReturnable<Boolean> cir) {
        UUID uuid = getGameProfile().id();
        int combatDuration = NoCombatElytra.CONFIG.get().combatDuration();

        if (lastDamaged.containsKey(uuid)) {
            long playerLastDamaged = lastDamaged.get(uuid);
            long combatEndTimestamp = playerLastDamaged + (combatDuration * 1000L);
            long now = System.currentTimeMillis();
            if (combatEndTimestamp > now) {
                cir.setReturnValue(false);
                int seconds = (int) Math.ceilDiv(combatEndTimestamp - now, 1000);
                displayClientMessage(Component.literal("You may use your elytra in %s second(s)!"
                                .replace("%s", String.valueOf(seconds)))
                                .withStyle(ChatFormatting.RED), true);
            }
        }
    }
}
