package com.littlesquad.dungeon.api.rewards;

import java.util.List;

public interface Reward {

    String id();

    List<ItemReward> rewards();

    double experience();

    List<String> commands();

}
