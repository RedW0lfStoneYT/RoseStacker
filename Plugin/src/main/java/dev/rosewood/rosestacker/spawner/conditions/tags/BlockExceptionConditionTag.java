package dev.rosewood.rosestacker.spawner.conditions.tags;

import dev.rosewood.rosestacker.manager.LocaleManager;
import dev.rosewood.rosestacker.spawner.conditions.ConditionTag;
import dev.rosewood.rosestacker.stack.StackedSpawner;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

public class BlockExceptionConditionTag extends ConditionTag {

    private List<Material> blocks;

    public BlockExceptionConditionTag(String tag) {
        super(tag, true);
    }

    @Override
    public boolean check(StackedSpawner stackedSpawner, Block spawnBlock) {
        return !this.blocks.contains(spawnBlock.getRelative(BlockFace.DOWN).getType());
    }

    @Override
    public boolean parseValues(String[] values) {
        this.blocks = new ArrayList<>();

        if (values.length == 0)
            return false;

        for (String value : values) {
            try {
                Material blockMaterial = Material.matchMaterial(value);
                if (blockMaterial != null && blockMaterial.isBlock() && blockMaterial.isSolid())
                    this.blocks.add(blockMaterial);
            } catch (Exception ignored) { }
        }

        return !this.blocks.isEmpty();
    }

    @Override
    protected List<String> getInfoMessageValues(LocaleManager localeManager) {
        return this.blocks.stream().map(Enum::name).collect(Collectors.toList());
    }

}
