package dev.rosewood.rosestacker.hook;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.compat.layers.persistentdata.MobMetaFlagType;
import org.bukkit.entity.LivingEntity;

public class OldMcMMOHook implements McMMOHook {

    @Override
    public void flagSpawnerMetadata(LivingEntity entity) {
        mcMMO.getCompatibilityManager().getPersistentDataLayer().flagMetadata(MobMetaFlagType.MOB_SPAWNER_MOB, entity);
    }

}
