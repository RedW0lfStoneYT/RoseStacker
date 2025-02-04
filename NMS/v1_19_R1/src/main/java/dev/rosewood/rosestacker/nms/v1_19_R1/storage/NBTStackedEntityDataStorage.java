package dev.rosewood.rosestacker.nms.v1_19_R1.storage;

import dev.rosewood.rosestacker.nms.storage.StackedEntityDataEntry;
import dev.rosewood.rosestacker.nms.storage.StackedEntityDataIOException;
import dev.rosewood.rosestacker.nms.storage.StackedEntityDataStorage;
import dev.rosewood.rosestacker.nms.util.ReflectionUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.trading.MerchantOffers;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftLivingEntity;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.LivingEntity;

public class NBTStackedEntityDataStorage implements StackedEntityDataStorage {

    private static final Field field_AbstractVillager_offers = ReflectionUtils.getFieldByPositionAndType(net.minecraft.world.entity.npc.AbstractVillager.class, 0, MerchantOffers.class);
    private final CompoundTag base;
    private final List<CompoundTag> data;

    public NBTStackedEntityDataStorage(LivingEntity livingEntity) {
        this.base = new CompoundTag();

        this.saveToTag(livingEntity, this.base);
        this.stripUnneeded(this.base);
        this.stripAttributeUuids(this.base);

        this.data = Collections.synchronizedList(new LinkedList<>());
    }

    public NBTStackedEntityDataStorage(byte[] data) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
             ObjectInputStream dataInput = new ObjectInputStream(inputStream)) {

            this.base = NbtIo.read(dataInput);
            int length = dataInput.readInt();
            List<CompoundTag> tags = new LinkedList<>();
            for (int i = 0; i < length; i++)
                tags.add(NbtIo.read(dataInput));
            this.data = Collections.synchronizedList(tags);
        } catch (Exception e) {
            throw new StackedEntityDataIOException(e);
        }
    }

    @Override
    public void addFirst(LivingEntity entity) {
        this.addAt(0, entity);
    }

    @Override
    public void addLast(LivingEntity entity) {
        this.addAt(this.data.size(), entity);
    }

    @Override
    public void addAllFirst(List<StackedEntityDataEntry<?>> stackedEntityDataEntry) {
        stackedEntityDataEntry.forEach(x -> this.addAt(0, x));
    }

    @Override
    public void addAllLast(List<StackedEntityDataEntry<?>> stackedEntityDataEntry) {
        stackedEntityDataEntry.forEach(x -> this.addAt(this.data.size(), x));
    }

    @Override
    public NBTStackedEntityDataEntry peek() {
        return new NBTStackedEntityDataEntry(this.rebuild(this.data.get(0)));
    }

    @Override
    public NBTStackedEntityDataEntry pop() {
        return new NBTStackedEntityDataEntry(this.rebuild(this.data.remove(0)));
    }

    @Override
    public int size() {
        return this.data.size();
    }

    @Override
    public boolean isEmpty() {
        return this.data.isEmpty();
    }

    @Override
    public List<StackedEntityDataEntry<?>> getAll() {
        List<StackedEntityDataEntry<?>> wrapped = new ArrayList<>(this.data.size());
        for (CompoundTag compoundTag : new ArrayList<>(this.data))
            wrapped.add(new NBTStackedEntityDataEntry(this.rebuild(compoundTag)));
        return wrapped;
    }

    @Override
    public List<StackedEntityDataEntry<?>> getTop(int count) {
        if (count > this.data.size())
            count = this.data.size();

        List<StackedEntityDataEntry<?>> wrapped = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
            wrapped.add(new NBTStackedEntityDataEntry(this.rebuild(this.data.get(i))));
        return wrapped;
    }

    @Override
    public byte[] serialize(int maxAmount) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream)) {

            int targetAmount = Math.min(maxAmount, this.data.size());
            List<CompoundTag> tagsToSave = new ArrayList<>(targetAmount);
            for (int i = 0; i < targetAmount; i++)
                tagsToSave.add(this.data.get(i));

            NbtIo.write(this.base, dataOutput);
            dataOutput.writeInt(tagsToSave.size());
            for (CompoundTag compoundTag : tagsToSave)
                NbtIo.write(compoundTag, dataOutput);

            dataOutput.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new StackedEntityDataIOException(e);
        }
    }

    private void addAt(int index, LivingEntity livingEntity) {
        CompoundTag compoundTag = new CompoundTag();
        this.saveToTag(livingEntity, compoundTag);
        this.stripUnneeded(compoundTag);
        this.stripAttributeUuids(compoundTag);
        this.removeDuplicates(compoundTag);
        this.data.add(index, compoundTag);
    }

    private void addAt(int index, StackedEntityDataEntry<?> stackedEntityDataEntry) {
        CompoundTag compoundTag = (CompoundTag) stackedEntityDataEntry.get();
        this.stripUnneeded(compoundTag);
        this.stripAttributeUuids(compoundTag);
        this.removeDuplicates(compoundTag);
        this.data.add(index, compoundTag);
    }

    private void removeDuplicates(CompoundTag compoundTag) {
        for (String key : new ArrayList<>(compoundTag.getAllKeys())) {
            Tag baseValue = this.base.get(key);
            Tag thisValue = compoundTag.get(key);
            if (baseValue != null && baseValue.equals(thisValue))
                compoundTag.remove(key);
        }
    }

    private CompoundTag rebuild(CompoundTag compoundTag) {
        CompoundTag merged = new CompoundTag();
        merged.merge(this.base);
        merged.merge(compoundTag);
        this.fillAttributeUuids(merged);
        return merged;
    }

    private void saveToTag(LivingEntity livingEntity, CompoundTag compoundTag) {
        // Async villager "fix", if the trades aren't loaded yet force them to save as empty, they will get loaded later
        if (livingEntity instanceof AbstractVillager) {
            try {
                net.minecraft.world.entity.npc.AbstractVillager villager = ((CraftAbstractVillager) livingEntity).getHandle();

                // Set the trades to empty if they are null to prevent trades from generating during the saveWithoutId call
                boolean bypassTrades = field_AbstractVillager_offers.get(villager) == null;
                if (bypassTrades)
                    field_AbstractVillager_offers.set(villager, new MerchantOffers());

                ((CraftLivingEntity) livingEntity).getHandle().saveWithoutId(compoundTag);

                // Restore the offers back to null and make sure nothing is written to the NBT
                if (bypassTrades) {
                    field_AbstractVillager_offers.set(villager, null);
                    compoundTag.remove("Offers");
                }
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
        } else {
            ((CraftLivingEntity) livingEntity).getHandle().saveWithoutId(compoundTag);
        }
    }

    private void stripUnneeded(CompoundTag compoundTag) {
        compoundTag.remove("UUID");
        compoundTag.remove("Pos");
        compoundTag.remove("Rotation");
        compoundTag.remove("WorldUUIDMost");
        compoundTag.remove("WorldUUIDLeast");
        compoundTag.remove("Motion");
        compoundTag.remove("OnGround");
        compoundTag.remove("FallDistance");
        compoundTag.remove("Leash");
        compoundTag.remove("Spigot.ticksLived");
        compoundTag.remove("Paper.OriginWorld");
        compoundTag.remove("Paper.Origin");
        CompoundTag bukkitValues = compoundTag.getCompound("BukkitValues");
        bukkitValues.remove("rosestacker:stacked_entity_data");
    }

    private void stripAttributeUuids(CompoundTag compoundTag) {
        ListTag attributes = compoundTag.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag attribute = attributes.getCompound(i);
            attribute.remove("UUID");
            ListTag modifiers = attribute.getList("Modifiers", Tag.TAG_COMPOUND);
            for (int j = 0; j < modifiers.size(); j++) {
                CompoundTag modifier = modifiers.getCompound(j);
                if (modifier.getString("Name").equals("Random spawn bonus")) {
                    modifiers.remove(j);
                    j--;
                } else {
                    modifier.remove("UUID");
                }
            }
        }
    }

    private void fillAttributeUuids(CompoundTag compoundTag) {
        ListTag attributes = compoundTag.getList("Attributes", Tag.TAG_COMPOUND);
        for (int i = 0; i < attributes.size(); i++) {
            CompoundTag attribute = attributes.getCompound(i);
            attribute.putUUID("UUID", UUID.randomUUID());
            ListTag modifiers = attribute.getList("Modifiers", Tag.TAG_COMPOUND);
            for (int j = 0; j < modifiers.size(); j++) {
                CompoundTag modifier = modifiers.getCompound(j);
                modifier.putUUID("UUID", UUID.randomUUID());
            }
            if (modifiers.size() == 0)
                attribute.remove("Modifiers");
        }
    }

}
