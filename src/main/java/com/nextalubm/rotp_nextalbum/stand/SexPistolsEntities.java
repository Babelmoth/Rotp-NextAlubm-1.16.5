package com.nextalubm.rotp_nextalbum.stand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.github.standobyte.jojo.action.stand.effect.StandEffectInstance;
import com.github.standobyte.jojo.action.stand.effect.StandEffectType;
import com.github.standobyte.jojo.entity.stand.StandEntity;
import com.github.standobyte.jojo.entity.stand.StandEntityType;
import com.github.standobyte.jojo.entity.stand.StandRelativeOffset;
import com.github.standobyte.jojo.network.PacketManager;
import com.github.standobyte.jojo.network.packets.fromserver.TrSetStandEntityPacket;
import com.nextalubm.rotp_nextalbum.entity.SexPistolsEntity;
import com.nextalubm.rotp_nextalbum.init.InitStandEffects;
import com.nextalubm.rotp_nextalbum.init.InitStands;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTargetMode;
import com.nextalubm.rotp_nextalbum.util.SexPistolsTransferOrder;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.vector.Vector3d;

public class SexPistolsEntities extends StandEffectInstance {
    private static final int PISTOL_COUNT = 6;
    private static final int REVIVE_TICKS = 1200;
    private static final int HUNGER_MIN_TICKS = 20 * 60 * 5;
    private static final int HUNGER_EXTRA_TICKS = 20 * 60 * 5;
    private static final Random RANDOM = new Random();
    private int pickedEntity;
    private int previousPickedEntity;
    private int loadedPistolsMask;
    private boolean summoned;
    private int sharedHungerTicks = randomHungerTicks();
    private boolean sharedHungry;
    private int fedPistolsMask;
    private SexPistolsTargetMode targetMode = SexPistolsTargetMode.PLAYERS;
    private SexPistolsTransferOrder transferOrder = SexPistolsTransferOrder.NONE;
    private final int[] reviveTicks = new int[PISTOL_COUNT];
    private final List<StandEntity> entities = new ArrayList<>();

    public SexPistolsEntities() {
        this(InitStandEffects.SEX_PISTOLS_ENTITIES.get());
    }

    public SexPistolsEntities(StandEffectType<?> effectType) {
        super(effectType);
    }

    public void addEntity(StandEntity entity) {
        if (!entities.contains(entity)) {
            entities.add(entity);
        }
    }

    public Iterable<StandEntity> getEntities() {
        return entities;
    }

    public List<StandEntity> getEntityList() {
        return entities;
    }

    public int getPickedEntity() {
        return pickedEntity;
    }

    public int getPreviousPickedEntity() {
        return previousPickedEntity;
    }

        public boolean isHungryForFood() {
        return sharedHungry;
    }

    public int getHungerTicksDebug() {
        return sharedHungerTicks;
    }

    public void debugSetHungry(boolean hungry) {
        sharedHungry = hungry;
        sharedHungerTicks = hungry ? 0 : randomHungerTicks();
        fedPistolsMask = 0;
        if (!hungry) {
            for (StandEntity entity : entities) {
                if (entity instanceof SexPistolsEntity) {
                    ((SexPistolsEntity) entity).debugRefreshSharedHunger();
                }
            }
        }
    }

    public boolean markPistolFed(int pistolIndex) {
        if (pistolIndex < 0 || pistolIndex >= PISTOL_COUNT) {
            return false;
        }
        resetSharedHungerTimer();
        refreshSharedFeedingState();
        return true;
    }

    private void refreshSharedFeedingState() {
        for (StandEntity entity : entities) {
            if (entity instanceof SexPistolsEntity) {
                ((SexPistolsEntity) entity).debugRefreshSharedHunger();
            }
        }
    }

    public void playSharedEatingAnimation(PlayerEntity player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        for (StandEntity entity : entities) {
            if (entity instanceof SexPistolsEntity && !entity.removed && entity.isAlive()) {
                ((SexPistolsEntity) entity).startSharedEatingAnimation(player, stack);
            }
        }
    }
    public int getFedPistolsMaskDebug() {
        return fedPistolsMask & ((1 << PISTOL_COUNT) - 1);
    }

    public boolean isPistolFedThisRound(int pistolIndex) {
        return pistolIndex >= 0 && pistolIndex < PISTOL_COUNT && (fedPistolsMask & (1 << pistolIndex)) != 0;
    }

    public void resetSharedHungerTimer() {
        sharedHungerTicks = randomHungerTicks();
        sharedHungry = false;
        fedPistolsMask = 0;
    }

    public boolean isPistolAvailable(int pistolIndex) {
        return pistolIndex >= 0 && pistolIndex < reviveTicks.length && reviveTicks[pistolIndex] <= 0 && !isPistolLoaded(pistolIndex);
    }

    public boolean isPistolLoaded(int pistolIndex) {
        return pistolIndex >= 0 && pistolIndex < PISTOL_COUNT && (loadedPistolsMask & (1 << pistolIndex)) != 0;
    }

    public int getLoadedPistolsMask() {
        return loadedPistolsMask & ((1 << PISTOL_COUNT) - 1);
    }

    public boolean markPistolLoaded(int pistolIndex) {
        if (!isPistolAvailable(pistolIndex)) {
            return false;
        }
        reviveTicks[pistolIndex] = 0;
        loadedPistolsMask |= 1 << pistolIndex;
        removePistolEntityForLoading(pistolIndex);
        return true;
    }

    public SexPistolsEntity releaseLoadedPistol(int pistolIndex, Vector3d position) {
        if (pistolIndex < 0 || pistolIndex >= PISTOL_COUNT) {
            return null;
        }
        loadedPistolsMask &= ~(1 << pistolIndex);
        reviveTicks[pistolIndex] = 0;
        SexPistolsEntity entity = getPistolEntity(pistolIndex);
        if (entity == null || entity.removed || !entity.isAlive()) {
            removePistolEntityReference(pistolIndex);
            entity = createPistolEntity(pistolIndex, position);
        }
        else {
            entity.setPos(position.x, position.y, position.z);
            entity.setHealth(entity.getMaxHealth());
        }
        if (entity != null) {
            summoned = true;
            repickAfterEntityChange();
        }
        return entity;
    }


    public void clearLoadedPistols() {
        loadedPistolsMask = 0;
        Arrays.fill(reviveTicks, 0);
        repickAfterEntityChange();
    }

    public void clearLoadedPistols(int pistolMask) {
        for (int i = 0; i < PISTOL_COUNT; i++) {
            if ((pistolMask & (1 << i)) != 0) {
                loadedPistolsMask &= ~(1 << i);
                reviveTicks[i] = 0;
            }
        }
        repickAfterEntityChange();
    }

    public int recallRemoteControlPistols() {
        int recalled = 0;
        for (StandEntity entity : entities) {
            if (!(entity instanceof SexPistolsEntity) || entity.removed || !entity.isAlive()) {
                continue;
            }
            if (entity.isManuallyControlled() || entity.isRemotePositionFixed()) {
                SexPistolsEntity pistol = (SexPistolsEntity) entity;
                entity.setManualControl(false, false);
                pistol.setDefaultOffsetFromUser(getDefaultOffset(pistol.getPistolIndex()));
                recalled++;
            }
        }
        repickAfterEntityChange();
        return recalled;
    }

    public int recallLoadedPistols(int pistolMask, Vector3d position) {
        int recalled = 0;
        for (int i = 0; i < PISTOL_COUNT; i++) {
            if ((pistolMask & (1 << i)) != 0 && releaseLoadedPistol(i, position) != null) {
                recalled++;
            }
        }
        return recalled;
    }

    public void shortCooldownLoadedPistols(int pistolMask) {
        for (int i = 0; i < PISTOL_COUNT; i++) {
            if ((pistolMask & (1 << i)) != 0) {
                loadedPistolsMask &= ~(1 << i);
                reviveTicks[i] = Math.max(reviveTicks[i], 200);
                removePistolEntityReference(i);
            }
        }
        repickAfterEntityChange();
    }

    public void shortCooldownPistol(int pistolIndex) {
        if (pistolIndex >= 0 && pistolIndex < PISTOL_COUNT) {
            shortCooldownLoadedPistols(1 << pistolIndex);
        }
    }

    public void setSummoned(boolean summoned) {
        this.summoned = summoned;
        if (!summoned && world != null && !world.isClientSide()) {
            for (StandEntity entity : entities) {
                entity.remove();
            }
            entities.clear();
            if (userPower != null) {
                userPower.setStandManifestation(null);
            }
            if (user != null) {
                PacketManager.sendToClientsTrackingAndSelf(new TrSetStandEntityPacket(user.getId(), -1), user);
            }
        }
    }

    public void pickNextEntity() {
        if (entities.isEmpty()) {
            return;
        }
        int start = pickedEntity;
        for (int i = 1; i <= entities.size(); i++) {
            int index = (start + i) % entities.size();
            if (canPick(index)) {
                pickEntity(index);
                return;
            }
        }
    }

    public void pickEntity(int index) {
        if (!canPick(index)) {
            int available = findFirstPickableEntity();
            if (available < 0) {
                clearPickedEntity();
                return;
            }
            index = available;
        }
        previousPickedEntity = pickedEntity;
        pickedEntity = index;
        if (!summoned || userPower == null || user == null || world == null || world.isClientSide()) {
            return;
        }
        StandEntity standEntity = entities.get(index);
        userPower.setStandManifestation(standEntity);
        PacketManager.sendToClientsTrackingAndSelf(new TrSetStandEntityPacket(user.getId(), standEntity.getId()), user);
    }

    public void onSummon() {
        pickEntity(pickedEntity);
    }

    public List<SexPistolsEntity> getAvailableLivingPistols() {
        List<SexPistolsEntity> living = new ArrayList<>();
        for (StandEntity entity : entities) {
            if (entity instanceof SexPistolsEntity && isEntityAvailable(entity)) {
                living.add((SexPistolsEntity) entity);
            }
        }
        return living;
    }

    public SexPistolsTargetMode getTargetMode() {
        return targetMode;
    }

    public void cycleTargetMode(boolean backwards) {
        targetMode = targetMode.cycle(backwards);
    }

    public SexPistolsTransferOrder getTransferOrder() {
        return transferOrder;
    }

    public void cycleTransferOrder(boolean backwards) {
        transferOrder = transferOrder.cycle(backwards);
    }

    public List<Integer> getAvailablePistolIndices() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < PISTOL_COUNT; i++) {
            if (isPistolAvailable(i)) {
                indices.add(i);
            }
        }
        return indices;
    }

    public void markPistolDead(SexPistolsEntity entity) {
        int pistolIndex = entity.getPistolIndex();
        boolean loaded = isPistolLoaded(pistolIndex);
        if (pistolIndex >= 0 && pistolIndex < reviveTicks.length && !loaded) {
            reviveTicks[pistolIndex] = REVIVE_TICKS;
        }
        int removedIndex = entities.indexOf(entity);
        entities.remove(entity);
        if (!entity.removed) {
            entity.remove();
        }
        if (loaded) {
            return;
        }
        if (!summoned || world == null || world.isClientSide()) {
            return;
        }
        if (entities.isEmpty()) {
            clearPickedEntity();
            return;
        }
        if (removedIndex >= 0 && pickedEntity >= removedIndex && pickedEntity > 0) {
            pickedEntity--;
        }
        pickEntity(pickedEntity);
    }

    private void removePistolEntityForLoading(int pistolIndex) {
        int removedIndex = -1;
        Iterator<StandEntity> iterator = entities.iterator();
        for (int i = 0; iterator.hasNext(); i++) {
            StandEntity entity = iterator.next();
            if (entity instanceof SexPistolsEntity && ((SexPistolsEntity) entity).getPistolIndex() == pistolIndex) {
                removedIndex = i;
                iterator.remove();
                if (!entity.removed) {
                    entity.remove();
                }
                break;
            }
        }
        if (removedIndex >= 0 && pickedEntity >= removedIndex && pickedEntity > 0) {
            pickedEntity--;
        }
        repickAfterEntityChange();
    }

    private void removePistolEntityReference(int pistolIndex) {
        Iterator<StandEntity> iterator = entities.iterator();
        while (iterator.hasNext()) {
            StandEntity entity = iterator.next();
            if (entity instanceof SexPistolsEntity && ((SexPistolsEntity) entity).getPistolIndex() == pistolIndex) {
                iterator.remove();
            }
        }
    }

    private SexPistolsEntity createPistolEntity(int pistolIndex, Vector3d position) {
        if (world == null || world.isClientSide() || user == null || userPower == null || pistolIndex < 0 || pistolIndex >= InitStands.SEX_PISTOLS_ENTITY_TYPES.size()) {
            return null;
        }
        StandEntityType<?> entityType = InitStands.SEX_PISTOLS_ENTITY_TYPES.get(pistolIndex).get();
        SexPistolsEntity entity = (SexPistolsEntity) entityType.create(world);
        if (entity == null) {
            return null;
        }
        entity.setPistolIndex(pistolIndex);
        entity.setDefaultOffsetFromUser(getDefaultOffset(pistolIndex));
        entity.setPos(position.x, position.y, position.z);
        entity.setUserAndPower(user, userPower);
        entity.setHealth(entity.getMaxHealth());
        addEntity(entity);
        world.addFreshEntity(entity);
        entity.onStandSummonServerSide();
        return entity;
    }

    private void summonForFoodBegging() {
        if (world == null || world.isClientSide() || user == null || userPower == null) {
            return;
        }
        boolean anySummoned = false;
        Vector3d position = user.position();
        for (int i = 0; i < InitStands.SEX_PISTOLS_ENTITY_TYPES.size(); i++) {
            if (!isPistolAvailable(i)) {
                continue;
            }
            SexPistolsEntity entity = createPistolEntity(i, position);
            if (entity != null) {
                entity.playSummonAnimation();
                anySummoned = true;
            }
        }
        if (anySummoned) {
            summoned = true;
            onSummon();
        }
    }

    private SexPistolsEntity getPistolEntity(int pistolIndex) {
        for (StandEntity entity : entities) {
            if (entity instanceof SexPistolsEntity && ((SexPistolsEntity) entity).getPistolIndex() == pistolIndex) {
                return (SexPistolsEntity) entity;
            }
        }
        return null;
    }

    private StandRelativeOffset getDefaultOffset(int pistolIndex) {
        double angle = Math.PI * 2.0D * (double) pistolIndex / (double) PISTOL_COUNT;
        double radius = 1.05D;
        double left = Math.cos(angle) * radius;
        double forward = Math.sin(angle) * radius;
        double y = 0.15D + (double) (pistolIndex % 3) * 0.575D;
        return StandRelativeOffset.withYOffset(left, y, forward);
    }

    private boolean canPick(int index) {
        if (index < 0 || index >= entities.size()) {
            return false;
        }
        return isEntityAvailable(entities.get(index));
    }

    private boolean isEntityAvailable(StandEntity entity) {
        if (entity == null || entity.removed || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof SexPistolsEntity) {
            return isPistolAvailable(((SexPistolsEntity) entity).getPistolIndex());
        }
        return true;
    }

    private int findFirstPickableEntity() {
        for (int i = 0; i < entities.size(); i++) {
            if (canPick(i)) {
                return i;
            }
        }
        return -1;
    }

    private void repickAfterEntityChange() {
        if (!summoned || world == null || world.isClientSide()) {
            return;
        }
        if (entities.isEmpty()) {
            clearPickedEntity();
            return;
        }
        if (pickedEntity >= entities.size()) {
            pickedEntity = entities.size() - 1;
        }
        pickEntity(pickedEntity);
    }

    private void clearPickedEntity() {
        if (userPower != null) {
            userPower.setStandManifestation(null);
        }
        if (user != null) {
            PacketManager.sendToClientsTrackingAndSelf(new TrSetStandEntityPacket(user.getId(), -1), user);
        }
    }

    @Override
    protected void start() {
    }

    @Override
    protected void tick() {
                tickSharedHunger();
        tickFoodBeggingAutoSummon();
for (int i = 0; i < reviveTicks.length; i++) {
            if (reviveTicks[i] > 0) {
                reviveTicks[i]--;
            }
        }
        if (summoned && !entities.isEmpty() && entities.stream().allMatch(entity -> entity.removed)) {
            setSummoned(false);
        }
    }

    private void tickSharedHunger() {
        if (world == null || world.isClientSide() || sharedHungry) {
            return;
        }
        if (sharedHungerTicks <= 0) {
            sharedHungerTicks = 0;
            sharedHungry = true;
            fedPistolsMask = 0;
            return;
        }
        sharedHungerTicks--;
        if (sharedHungerTicks <= 0) {
            sharedHungry = true;
            fedPistolsMask = 0;
        }
    }

    private void tickFoodBeggingAutoSummon() {
        if (world == null || world.isClientSide() || !sharedHungry || summoned || user == null || userPower == null || !user.isAlive() || !isHoldingFood(user)) {
            return;
        }
        summonForFoodBegging();
    }

    private boolean isHoldingFood(LivingEntity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }
        PlayerEntity player = (PlayerEntity) entity;
        return isFood(player.getMainHandItem()) || isFood(player.getOffhandItem());
    }

    private boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null;
    }

    private static int randomHungerTicks() {
        return HUNGER_MIN_TICKS + RANDOM.nextInt(HUNGER_EXTRA_TICKS + 1);
    }
    @Override
    protected void stop() {
        setSummoned(false);
    }

    @Override
    protected boolean needsTarget() {
        return false;
    }

    @Override
    protected void writeAdditionalSaveData(CompoundNBT nbt) {
        nbt.putIntArray("ReviveTicks", reviveTicks);
        nbt.putInt("LoadedPistolsMask", loadedPistolsMask);
        nbt.putInt("TargetMode", targetMode.ordinal());
        nbt.putInt("TransferOrder", transferOrder.ordinal());
        nbt.putInt("SharedHungerTicks", sharedHungerTicks);
        nbt.putBoolean("SharedHungry", sharedHungry);
        nbt.putInt("FedPistolsMask", fedPistolsMask);
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        int[] savedReviveTicks = nbt.getIntArray("ReviveTicks");
        for (int i = 0; i < reviveTicks.length && i < savedReviveTicks.length; i++) {
            reviveTicks[i] = savedReviveTicks[i];
        }
        loadedPistolsMask = nbt.getInt("LoadedPistolsMask") & ((1 << PISTOL_COUNT) - 1);
        targetMode = SexPistolsTargetMode.byId(nbt.getInt("TargetMode"));
        transferOrder = SexPistolsTransferOrder.byId(nbt.getInt("TransferOrder"));
        sharedHungerTicks = nbt.contains("SharedHungerTicks") ? nbt.getInt("SharedHungerTicks") : randomHungerTicks();
        sharedHungry = nbt.getBoolean("SharedHungry");
        fedPistolsMask = nbt.getInt("FedPistolsMask") & ((1 << PISTOL_COUNT) - 1);
    }
}