package com.littlesquad.dungeon.internal.event;

import com.littlesquad.dungeon.api.event.ObjectiveEvent;

//Used for marking Requirements as 'error printer' for not breaking the plugin throwing errors!
public interface FailingRequirement extends ObjectiveEvent.Requirement {}
