package dev.rosewood.rosestacker.stack.settings.entity;

import com.google.gson.JsonObject;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosestacker.stack.EntityStackComparisonResult;
import dev.rosewood.rosestacker.stack.StackedEntity;
import dev.rosewood.rosestacker.stack.settings.EntityStackSettings;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Slime;

public class SlimeStackSettings extends EntityStackSettings {

    private final boolean dontStackIfDifferentSize;
    private final boolean accurateDropsWithKillEntireStackOnDeath;

    public SlimeStackSettings(CommentedFileConfiguration entitySettingsFileConfiguration, JsonObject jsonObject) {
        super(entitySettingsFileConfiguration, jsonObject);

        this.dontStackIfDifferentSize = this.settingsConfiguration.getBoolean("dont-stack-if-different-size");
        this.accurateDropsWithKillEntireStackOnDeath = this.settingsConfiguration.getBoolean("accurate-drops-with-kill-entire-stack-on-death");
    }

    @Override
    protected EntityStackComparisonResult canStackWithInternal(StackedEntity stack1, StackedEntity stack2) {
        Slime slime1 = (Slime) stack1.getEntity();
        Slime slime2 = (Slime) stack2.getEntity();

        if (this.dontStackIfDifferentSize && slime1.getSize() != slime2.getSize())
            return EntityStackComparisonResult.DIFFERENT_SIZES;

        return EntityStackComparisonResult.CAN_STACK;
    }

    @Override
    protected void setDefaultsInternal() {
        this.setIfNotExists("dont-stack-if-different-size", true);
        this.setIfNotExists("accurate-drops-with-kill-entire-stack-on-death", true);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.SLIME;
    }

    public boolean isAccurateDropsWithKillEntireStackOnDeath() {
        return this.accurateDropsWithKillEntireStackOnDeath;
    }

}
