package com.connorlinfoot.actionbarapi.events;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Getter
public class ActionBarMessageEvent extends Event {
    private final Player player;
    @Setter
    private String message;
    @Setter
    private boolean cancelled = false;

    public ActionBarMessageEvent(Player player, String message) {
        this.player = player;
        this.message = message;
    }

    @Override
    public HandlerList getHandlers() {
        return null;
    }
}

