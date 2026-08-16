package com.andrewbristowx.chainacobblemon.systems;

import java.util.ArrayList;
import java.util.List;

/** Simple Gson-friendly snapshots shared by server and client. */
public final class SystemSnapshots {
    private SystemSnapshots() {}

    public static final class GachaSnapshot {
        public String banner = "standard";
        public int pity;
        public int softPity;
        public int hardPity;
        public int standardTickets;
        public int chainaTickets;
        public long standardRolls;
        public long chainaRolls;
        public String message = "";
        public List<GachaResultView> results = new ArrayList<>();
    }

    public static final class GachaResultView {
        public String species = "";
        public String name = "";
        public String tier = "COMMON";
        public int level;
        public boolean shiny;
        public boolean pity;
    }

    public static final class DailySnapshot {
        public boolean eligible;
        public int streak;
        public int totalClaims;
        public String lastReward = "";
        public long nextClaimEpochMillis;
        public String message = "";
        public List<DailyRewardView> possibleRewards = new ArrayList<>();
    }

    public static final class DailyRewardView {
        public String type = "";
        public String value = "";
        public int amount;
        public int weight;
        public String label = "";
    }

    public static final class PassSnapshot {
        public String playerName = "";
        public long experience;
        public int level;
        public long levelStartXp;
        public long nextLevelXp;
        public boolean premium;
        public int page;
        public long chainaRolls;
        public String message = "";
        public List<PassRewardSlot> free = new ArrayList<>();
        public List<PassRewardSlot> premiumTrack = new ArrayList<>();
    }

    public static final class PassRewardSlot {
        public int level;
        public int amount;
        public String label = "";
        public boolean unlocked;
        public boolean claimed;
        public boolean claimable;
    }
}
