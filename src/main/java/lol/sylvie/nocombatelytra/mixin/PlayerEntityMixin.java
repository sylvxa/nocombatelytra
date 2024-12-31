package lol.sylvie.nocombatelytra.mixin;

import com.mojang.authlib.GameProfile;
import lol.sylvie.nocombatelytra.NoCombatElytra;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Shadow public abstract GameProfile getGameProfile();
    @Shadow public abstract void sendMessage(Text message, boolean overlay);
    @Shadow public abstract void stopGliding();
    Map<UUID, Long> lastDamaged = new HashMap<>();
    @Inject(method = "damage(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/damage/DamageSource;F)Z", at = @At("TAIL"))
    public void nocombatelytra$storeLastDamaged(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity thisPlayer = (PlayerEntity) (Object) this;
        if (thisPlayer.equals(source.getAttacker())) return;

        if (source.isIn(DamageTypeTags.IS_PLAYER_ATTACK) || source.getAttacker() instanceof PlayerEntity) {
            UUID uuid = getGameProfile().getId();

            lastDamaged.put(uuid, System.currentTimeMillis());
            stopGliding();
        }
    }

    @Inject(method = "remove", at = @At("TAIL"))
    public void nocombatelytra$resetTimerOnDeath(Entity.RemovalReason reason, CallbackInfo ci) {
        UUID uuid = getGameProfile().getId();
        lastDamaged.remove(uuid);
    }

    @Inject(method = "checkGliding", at = @At("HEAD"), cancellable = true)
    public void nocombatelytra$addDamageCheck(CallbackInfoReturnable<Boolean> cir) {
        UUID uuid = getGameProfile().getId();
        int combatDuration = NoCombatElytra.CONFIG.get().combatDuration();

        if (lastDamaged.containsKey(uuid)) {
            long playerLastDamaged = lastDamaged.get(uuid);
            long combatEndTimestamp = playerLastDamaged + (combatDuration * 1000L);
            long now = System.currentTimeMillis();
            if (combatEndTimestamp > now) {
                cir.setReturnValue(false);
                int seconds = (int) Math.ceilDiv(combatEndTimestamp - now, 1000);
                sendMessage(Text.literal("You may use your elytra in %s second(s)!"
                                .replace("%s", String.valueOf(seconds)))
                                .formatted(Formatting.RED), true);
            }
        }
    }
}
