package com.andrewbristowx.chainacobblemon.twitch;

/** Client-facing Twitch state. Contains no OAuth tokens or secrets. */
public final class TwitchSnapshot {
    public boolean visible = true;
    public boolean enabled;
    public String mode = "development";
    public String broadcaster = "chainavt";
    public boolean channelOnline;
    public boolean linked;
    public String twitchLogin = "";
    /** 0 = no sub, 1/2/3 = Twitch subscription tier. */
    public int tier;
    public String rankLabel = "Sin vincular";
    public String lastSync = "Nunca";
    public String message = "";
    public String linkUrl = "";
    public String linkCode = "";
}
