package com.littlesquad.dungeon.api;

import com.littlesquad.dungeon.api.status.Status;
import org.bukkit.entity.Player;

import java.util.Date;

public interface TimedDungeon extends Status, Dungeon {

    void startTimer(Player player);

    long getTimeRemaining(Player player);

    boolean isTimeExpired(Player player);

    void onTimeExpired();

    void startDungeonWithTimer(Player... players);

    void setTimeExpiredCommand(String command);

    boolean isTimeLimited();

}
