package dev.rosewood.rosestacker.spawner.conditions.tags;

import dev.rosewood.rosestacker.manager.LocaleManager;
import dev.rosewood.rosestacker.spawner.conditions.ConditionTag;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import java.util.Collections;
import java.util.List;
import org.bukkit.block.Block;

public class AboveYAxisConditionTag extends ConditionTag {

    private int yValue;

    public AboveYAxisConditionTag(String tag) {
        super(tag, true);
    }

    @Override
    public boolean check(StackedSpawner stackedSpawner, Block spawnBlock) {
        return spawnBlock.getY() >= this.yValue;
    }

    @Override
    public boolean parseValues(String[] values) {
        if (values.length != 1)
            return false;

        try {
            this.yValue = Integer.parseInt(values[0]);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    @Override
    protected List<String> getInfoMessageValues(LocaleManager localeManager) {
        return Collections.singletonList(String.valueOf(this.yValue));
    }

}
