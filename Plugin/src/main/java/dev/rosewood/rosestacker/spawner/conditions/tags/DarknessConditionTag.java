package dev.rosewood.rosestacker.spawner.conditions.tags;

import dev.rosewood.rosestacker.manager.LocaleManager;
import dev.rosewood.rosestacker.spawner.conditions.ConditionTag;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import dev.rosewood.rosestacker.utils.StackerUtils;
import java.util.Collections;
import java.util.List;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;

public class DarknessConditionTag extends ConditionTag {

    public DarknessConditionTag(String tag) {
        super(tag, true);
    }

    @Override
    public boolean check(StackedSpawner stackedSpawner, Block spawnBlock) {
        if (StackerUtils.isOccluding(spawnBlock.getType()))
            return false;

        if (stackedSpawner.getSpawnerTile().getSpawnedType() == EntityType.BLAZE || stackedSpawner.getSpawnerTile().getSpawnedType() == EntityType.SILVERFISH)
            return spawnBlock.getLightLevel() <= 11;

        return spawnBlock.getLightLevel() <= 7;
    }

    @Override
    public boolean parseValues(String[] values) {
        return values.length == 0;
    }

    @Override
    protected List<String> getInfoMessageValues(LocaleManager localeManager) {
        return Collections.emptyList();
    }

}
