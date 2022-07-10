package de.varoplugin.varo.game.task.trigger.builder;

import de.varoplugin.varo.api.task.trigger.Trigger;
import de.varoplugin.varo.api.task.trigger.builder.TriggerBuilder;
import de.varoplugin.varo.config.VaroConfig;
import de.varoplugin.varo.game.State;

public interface IVaroTriggerBuilder extends TriggerBuilder {

    IVaroTriggerBuilder when(State state);

    // TODO: Remove?
    IVaroTriggerBuilder whenNot(State state);

    @Override
    Trigger build();

    @Override
    Trigger complete();

    @Override
    IVaroTriggerBuilder when(Trigger trigger);

    @Override
    TriggerBuilder when(TriggerBuilder when);

    IVaroTriggerBuilder and(VaroConfig.VaroBoolConfigEntry entry);

    @Override
    IVaroTriggerBuilder and(Trigger triggers);

    @Override
    IVaroTriggerBuilder and(TriggerBuilder and);

    @Override
    IVaroTriggerBuilder down();

    @Override
    IVaroTriggerBuilder ground();
}
