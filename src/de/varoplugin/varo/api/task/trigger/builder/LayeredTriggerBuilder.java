package de.varoplugin.varo.api.task.trigger.builder;

import de.varoplugin.varo.api.task.trigger.VaroTrigger;
import de.varoplugin.varo.util.Pair;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class LayeredTriggerBuilder implements TriggerBuilder {

    private final Plugin plugin;
    private final List<VaroTrigger> triggers;
    private final Map<Integer, LayeredTriggerBuilder> layers;
    private int y;

    public LayeredTriggerBuilder(Plugin plugin) {
        this.plugin = plugin;
        this.triggers = new ArrayList<>();
        this.layers = new HashMap<>();
    }

    private LayeredTriggerBuilder getNext() {
        return this.layers.get(this.triggers.size());
    }

    private LayeredTriggerBuilder findNext() {
        LayeredTriggerBuilder next = this;
        for (int i = 0; i < this.y; i++) {
            LayeredTriggerBuilder nextTemp = next.getNext();
            next = nextTemp != null ? nextTemp : next.addLayer();
        }
        return next;
    }

    private LayeredTriggerBuilder addLayer() {
        LayeredTriggerBuilder next = new LayeredTriggerBuilder(this.plugin);
        this.layers.put(this.triggers.size(), next);
        return next;
    }

    private VaroTrigger buildHead(Set<VaroTrigger> triggers) {
        VaroTrigger head;
        if (triggers.isEmpty()) return null;
        else if (triggers.size() == 1) {
            head = this.triggers.get(0);
        } else {
            head = new BitchTrigger(this.plugin);
            head.addChildren(triggers.toArray(new VaroTrigger[0]));
        }
        return head;
    }

    private Pair<VaroTrigger, Integer> buildHead(int start) {
        Set<VaroTrigger> triggers = new HashSet<>();
        LayeredTriggerBuilder child = null;
        int i;
        for (i = start; i < this.triggers.size(); i++) {
            triggers.add(this.triggers.get(i));
            if (this.layers.containsKey(i + 1)) {
                child = this.layers.get(i);
                break;
            }
        }
        VaroTrigger head = this.buildHead(triggers);
        if (head != null && child != null) head.addChildren(child.build());
        return new Pair<>(head, i);
    }

    @Override
    public VaroTrigger build() {
        Set<VaroTrigger> heads = new HashSet<>();
        for (int i = 0; i < this.triggers.size(); i++) {
            Pair<VaroTrigger, Integer> head = this.buildHead(i);
            if (head.getKey() != null) heads.add(head.getKey());
            i = head.getValue();
        }
        return this.buildHead(heads);
    }

    @Override
    public VaroTrigger complete() {
        VaroTrigger built = this.build();
        built.activate();
        return built;
    }

    @Override
    public TriggerBuilder when(VaroTrigger when) {
        TriggerBuilder next = this.findNext();
        if (next != this) next.when(when);
        else this.triggers.add(when);
        return this;
    }

    @Override
    public TriggerBuilder when(TriggerBuilder when) {
        return this.when(when.build());
    }

    @Override
    public TriggerBuilder and(VaroTrigger and) {
        this.y++;
        LayeredTriggerBuilder next = this.findNext();
        next.when(and);
        return this;
    }

    @Override
    public TriggerBuilder and(TriggerBuilder and) {
        return this.and(and.build());
    }

    @Override
    public TriggerBuilder down() {
        this.y--;
        return this;
    }

    @Override
    public TriggerBuilder ground() {
        this.y = 0;
        return this;
    }
}
