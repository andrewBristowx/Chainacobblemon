package com.andrewbristowx.chainacobblemon.admin;

import com.andrewbristowx.chainacobblemon.gameplay.GameplayDataStore;
import com.andrewbristowx.chainacobblemon.gameplay.GameplaySystems;
import com.andrewbristowx.chainacobblemon.gameplay.LevelSyncService;
import com.andrewbristowx.chainacobblemon.rewards.ChainaKits;
import com.andrewbristowx.chainacobblemon.systems.ChainaSystems;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.Locale;

/** Server-authoritative reset scopes used by the Chaina admin panel. */
public final class PlayerResetService {
    private PlayerResetService() {}

    public static String reset(ServerPlayerEntity player, String requestedScope) {
        if (player == null) return "Jugador no disponible.";
        String scope=requestedScope==null?"todo":requestedScope.toLowerCase(Locale.ROOT);
        boolean all="todo".equals(scope);
        if(all||"gasha".equals(scope)){var d=ChainaSystems.data(player);d.gacha.standardPity=0;d.gacha.chainaPity=0;d.gacha.standardRolls=0;d.gacha.chainaRolls=0;}
        if(all||"diario".equals(scope)){var d=ChainaSystems.data(player);d.daily.lastClaimDate="";d.daily.streak=0;d.daily.totalClaims=0;d.daily.lastRewardLabel="";d.daily.pendingType="";d.daily.pendingValue="";d.daily.pendingAmount=0;}
        if(all||"pase".equals(scope)){var d=ChainaSystems.data(player);d.pass.experience=0;d.pass.activeSecondsBank=0;d.pass.claimedFree=new HashSet<>();d.pass.claimedPremium=new HashSet<>();}
        GameplayDataStore.PlayerData gp=GameplaySystems.data(player);
        if(all||"economia".equals(scope)) GameplaySystems.setBalance(player,GameplaySystems.config().economy.startingBalance);
        if(all||"trabajos".equals(scope)){gp.activeJobs.clear();gp.jobProgress.clear();GameplaySystems.setBalance(player,GameplaySystems.balance(player));}
        if(all||"misiones".equals(scope)){gp.questProgress.clear();gp.claimedQuests.clear();GameplaySystems.setBalance(player,GameplaySystems.balance(player));}
        if(all||"mazmorras".equals(scope)){gp.dungeonCooldownUntil.clear();gp.trainerCooldownUntil.clear();GameplaySystems.setBalance(player,GameplaySystems.balance(player));}
        if(all||"kits".equals(scope)) ChainaKits.reset(player.getUuid(),"");
        LevelSyncService.restore(player,"reinicio administrativo",false);
        ChainaSystems.store().save(player.getUuid());
        return "Datos de "+player.getName().getString()+" reiniciados: "+scope+".";
    }
}
