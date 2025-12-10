package com.littlesquad.api.checkpoint;

public abstract class AbstractInteractionCheckPoint implements Checkpoint {

    public AbstractInteractionCheckPoint() {
        /*new Thread(() -> {
            Each of those classes has an internal thread
            the check periodically if player in the dungeon he's
            associated to have reached or interacted it
        }).start();*/
    }

}
