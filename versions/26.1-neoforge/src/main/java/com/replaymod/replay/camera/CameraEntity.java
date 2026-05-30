package com.replaymod.replay.camera;

import com.replaymod.core.KeyBindingRegistry;
import com.replaymod.core.ReplayMod;
import com.replaymod.core.SettingsRegistry;
import com.replaymod.core.events.KeyBindingEventCallback;
import com.replaymod.core.events.PreRenderCallback;
import com.replaymod.core.events.PreRenderHandCallback;
import com.replaymod.core.events.SettingsChangedCallback;
import com.replaymod.replay.ReplayHandler;
import com.replaymod.replay.events.RenderHotbarCallback;
import com.replaymod.replay.events.RenderSpectatorCrosshairCallback;
import com.replaymod.replay.mixin.EntityPlayerAccessor;
import de.johni0702.minecraft.gui.utils.EventRegistrations;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import com.replaymod.core.utils.Utils;
import com.replaymod.replay.ReplayModReplay;
import com.replaymod.replay.Setting;
import com.replaymod.replay.mixin.FirstPersonRendererAccessor;
import com.replaymod.replaystudio.util.Location;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.stats.StatsCounter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

//#if MC>=12109
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
//#endif

//#if MC>=12106
import net.minecraft.world.entity.player.Input;
//#endif

import net.neoforged.bus.api.SubscribeEvent;

//#if MC>=12002
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.util.Mth;
//#endif

//#if MC>=11400
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.material.Fluid;
//#if MC>=11802
import net.minecraft.tags.TagKey;
//#else
//$$ import net.minecraft.tag.Tag;
//#endif
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//#else
//$$ import com.replaymod.replay.events.ReplayChatMessageEvent;
//$$ import net.minecraft.util.math.RayTraceResult;
//$$ import net.minecraft.util.text.ITextComponent;
//$$ import net.minecraft.world.level.Level;
//$$
//#if MC>=11400
//$$ import net.minecraft.util.math.RayTraceFluidMode;
//#else
//$$ import net.minecraft.block.material.Material;
//$$ import net.minecraft.entity.EntityLivingBase;
//#endif
//#endif

//#if MC>=10904
import net.minecraft.world.entity.EquipmentSlot;
//#if MC>=11200
//#if MC>=11400
import net.minecraft.client.ClientRecipeBook;
//#else
//$$ import net.minecraft.stats.RecipeBook;
//#endif
//#endif
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.InteractionHand;
//#endif

//#if MC>=10800
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.PlayerModelPart;
//#else
//$$ import net.minecraft.client.entity.EntityClientPlayerMP;
//$$ import net.minecraft.util.Session;
//#endif

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static com.replaymod.core.versions.MCVer.*;

/**
 * The camera entity used as the main player entity during replay viewing.
 * During a replay the player should be an instance of this class.
 * Camera movement is controlled by a separate {@link CameraController}.
 */
@SuppressWarnings("EntityConstructor")
public class CameraEntity
        //#if MC>=10800
        extends LocalPlayer
        //#else
        //$$ extends EntityClientPlayerMP
        //#endif
{
    private static final UUID CAMERA_UUID = UUID.nameUUIDFromBytes("ReplayModCamera".getBytes(StandardCharsets.UTF_8));

    /**
     * Roll of this camera in degrees.
     */
    public float roll;

    // Fields for arm yaw/pitch rendering interpolation (no longer in LocalPlayer in MC 26.1)
    public float renderYaw, renderPitch, lastRenderYaw, lastRenderPitch;

    private CameraController cameraController;

    private long lastControllerUpdate = System.currentTimeMillis();

    /**
     * The entity whose hand was the last one rendered.
     */
    private Entity lastHandRendered = null;

    /**
     * The hashCode and equals methods of Entity are not stable.
     * Therefore we cannot register any event handlers directly in the CameraEntity class and
     * instead have this inner class.
     */
    private EventHandler eventHandler = new EventHandler();

    public CameraEntity(
            Minecraft mcIn,
            //#if MC>=11400
            ClientLevel worldIn,
            //#else
            //$$ World worldIn,
            //#endif
            //#if MC<10800
            //$$ Session session,
            //#endif
            ClientPacketListener netHandlerPlayClient,
            StatsCounter statisticsManager
            //#if MC>=11200
            //#if MC>=11400
            , ClientRecipeBook recipeBook
            //#else
            //$$ , RecipeBook recipeBook
            //#endif
            //#endif
    ) {
        super(mcIn,
                worldIn,
                //#if MC<10800
                //$$ session,
                //#endif
                netHandlerPlayClient,
                statisticsManager
                //#if MC>=11200
                , recipeBook
                //#endif
                //#if MC>=11600
                //#if MC>=12106
                , Input.EMPTY
                //#else
                //$$ , false
                //#endif
                , false
                //#endif
                //#if MC >= 26.1
                , mcIn.computeChatAbilities()
                //#endif
        );
        //#if MC>=10900
        setUUID(CAMERA_UUID);
        //#else
        //$$ entityUniqueID = CAMERA_UUID;
        //#endif
        eventHandler.register();
        if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
            cameraController = ReplayModReplay.instance.createCameraController(this);
        } else {
            cameraController = new SpectatorCameraController(this);
        }
    }

    public CameraController getCameraController() {
        return cameraController;
    }

    public void setCameraController(CameraController cameraController) {
        this.cameraController = cameraController;
    }

    /**
     * Moves the camera by the specified delta.
     * @param x Delta in X direction
     * @param y Delta in Y direction
     * @param z Delta in Z direction
     */
    public void moveCamera(double x, double y, double z) {
        setCameraPosition(this.getX() + x, this.getY() + y, this.getZ() + z);
    }

    /**
     * Set the camera position.
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public void setCameraPosition(double x, double y, double z) {
        this.xOld = this.xo = x;
        this.yOld = this.yo = y;
        this.zOld = this.zo = z;
        this.setPos(x, y, z);
        updateBoundingBox();
    }

    /**
     * Sets the camera rotation.
     * @param yaw Yaw in degrees
     * @param pitch Pitch in degrees
     * @param roll Roll in degrees
     */
    public void setCameraRotation(float yaw, float pitch, float roll) {
        //#if MC>=12102
        // Note: MC's `setPitch` now forces values into the [-90; 90] range, however the math it uses is incorrect, so
        //       we need to wrap our value into the [-180; 180] range first to get correct results.
        pitch = Mth.wrapDegrees(pitch);
        //#endif
        this.yRotO = yaw;
        this.xRotO = pitch;
        this.setYRot(yaw);
        this.setXRot(pitch);
        this.roll = roll;
    }

    /**
     * Sets the camera position and rotation to that of the specified AdvancedPosition
     * @param pos The position and rotation to set
     */
    public void setCameraPosRot(Location pos) {
        setCameraRotation(pos.getYaw(), pos.getPitch(), roll);
        setCameraPosition(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Sets the camera position and rotation to that of the specified entity.
     * @param to The entity whose position to copy
     */
    public void setCameraPosRot(Entity to) {
        if (to == this) return;
        //#if MC>=10800
        float yOffset = 0;
        //#else
        //$$ float yOffset = 1.62f; // Magic value (eye height) from EntityRenderer#orientCamera
        //#endif
        this.xo = to.xo;
        this.yo = to.yo + yOffset;
        this.zo = to.zo;
        this.yRotO = to.yRotO;
        this.xRotO = to.xRotO;
        this.setPos(to.getX(), to.getY(), to.getZ());
        this.setYRot(to.getYRot());
        this.setXRot(to.getXRot());
        this.xOld = to.xOld;
        this.yOld = to.yOld + yOffset;
        this.zOld = to.zOld;
        this.wrapArmYaw();
        updateBoundingBox();
    }

    //#if MC>=11400
    @Override
    public float getYRot(float partialTick) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return this.yRotO + (this.getYRot() - this.yRotO) * partialTick;
        }
        return super.getYRot(partialTick);
    }

    @Override
    public float getXRot(float partialTick) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return this.xRotO + (this.getXRot() - this.xRotO) * partialTick;
        }
        return super.getXRot(partialTick);
    }
    //#endif

    private void updateBoundingBox() {
        //#if MC>=11400
        float width = getBbWidth();
        float height = getBbHeight();
        //#endif
        //#if MC>=10800
        setBoundingBox(new AABB(
        //#else
        //$$ this.boundingBox.setBB(AxisAlignedBB.getBoundingBox(
        //#endif
                this.getX() - width / 2, this.getY(), this.getZ() - width / 2,
                this.getX() + width / 2, this.getY() + height, this.getZ() + width / 2));
    }

    @Override
    public void tick() {
        //#if MC>=10800
        Entity view =
        //#else
        //$$ EntityLivingBase view =
        //#endif
            this.minecraft.getCameraEntity();
        if (view != null) {
            // Make sure we're always spectating the right entity
            // This is important if the spectated player respawns as their
            // entity is recreated and we have to spectate a new entity
            UUID spectating = ReplayModReplay.instance.getReplayHandler().getSpectatedUUID();
            // FIXME remap bug: Pattern doesn't work when these two are inlined
            Level cameraWorld = this.level();
            Level viewWorld = view.level();
            if (spectating != null && (view.getUUID() != spectating
                    || viewWorld != cameraWorld)
                    || cameraWorld.getEntity(view.getId()) != view) {
                if (spectating == null) {
                    // Entity (non-player) died, stop spectating
                    ReplayModReplay.instance.getReplayHandler().spectateEntity(this);
                    return;
                }
                view = cameraWorld.getEntity(spectating);
                if (view != null) {
                    this.minecraft.setCameraEntity(view);
                } else {
                    this.minecraft.setCameraEntity(this);
                    return;
                }
            }
            // Move cmera to their position so when we exit the first person view
            // we don't jump back to where we entered it
            if (view != this) {
                setCameraPosRot(view);
            }
        }
    }

    // init() was removed in MC 26.1 - world sync is handled elsewhere
    public void onAddedToLevel() {
        if (this.minecraft.level != null) {
            this.setLevel(this.minecraft.level);
        }
    }

    @Override
    protected void setRot(float yaw, float pitch) {
        if (this.minecraft.getCameraEntity() == this) {
            // Only update camera rotation when the camera is the view
            super.setRot(yaw, pitch);
        }
    }

    // isInsideWall() removed in MC 26.1 - suffocation overlay handled differently

    //#if MC<11400
    //$$ @Override
    //$$ public boolean isInsideOfMaterial(Material materialIn) {
    //$$     return falseUnlessSpectating(e -> e.isInsideOfMaterial(materialIn)); // Make sure no overlays are rendered
    //$$ }
    //#endif

    //#if MC>=11400
    @Override
    public boolean isEyeInFluid(
            //#if MC>=11802
            TagKey<Fluid> fluid
            //#else
            //$$ Tag<Fluid> fluid
            //#endif
    ) {
        return falseUnlessSpectating(entity -> entity.isEyeInFluid(fluid));
    }

    // getUnderwaterVisibility() removed in MC 26.1
    //#else
    //#if MC>=10800
    //$$ @Override
    //$$ public boolean isInLava() {
    //$$     return falseUnlessSpectating(Entity::isInLava); // Make sure no lava overlay is rendered
    //$$ }
    //#else
    //$$ @Override
    //$$ public boolean handleLavaMovement() {
    //$$     return falseUnlessSpectating(Entity::handleLavaMovement); // Make sure no lava overlay is rendered
    //$$ }
    //#endif
    //$$
    //$$ @Override
    //$$ public boolean isInWater() {
    //$$     return falseUnlessSpectating(Entity::isInWater); // Make sure no water overlay is rendered
    //$$ }
    //#endif

    @Override
    public boolean isOnFire() {
        return falseUnlessSpectating(Entity::isOnFire); // Make sure no fire overlay is rendered
    }

    private boolean falseUnlessSpectating(Function<Entity, Boolean> property) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != null && view != this) {
            return property.apply(view);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false; // We are in full control of ourselves
    }

    // spawnSprintingParticles() removed in MC 26.1 - camera produces no particles

    // canHit() removed in MC 26.1

    //#if MC>=10800
    @Override
    public boolean isSpectator() {
        ReplayHandler replayHandler = ReplayModReplay.instance.getReplayHandler();
        return replayHandler == null || replayHandler.isCameraView(); // Make sure we're treated as spectator
    }
    //#endif

    //#if MC>=11400
    @Override
    public boolean shouldRender(double double_1, double double_2, double double_3) {
        return false; // never render the camera otherwise it'd be visible e.g. in 3rd-person or with shaders
    }
    //#else
    //$$ @Override
    //$$ public boolean shouldRenderInPass(int pass) {
    //$$     // Never render the camera
    //$$     // This is necessary to hide the player head in third person mode and to not
    //$$     // cause any unwanted shadows when rendering with shaders.
    //$$     return false;
    //$$ }
    //#endif

    //#if MC>=12102
    @Override
    public float getFieldOfViewModifier(boolean firstPerson, float fovEffectScale) {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getFieldOfViewModifier(firstPerson, fovEffectScale);
        }
        return 1;
    }
    //#elseif MC>=10800
    //$$ @Override
    //$$ public float getFovMultiplier() {
    //$$     Entity view = this.minecraft.getCameraEntity();
    //$$     if (view != this && view instanceof AbstractClientPlayer) {
    //$$         return ((AbstractClientPlayer) view).getFovMultiplier();
    //$$     }
    //$$     return 1;
    //$$ }
    //#else
    //$$ @Override
    //$$ public float getFOVMultiplier() {
    //$$     return 1;
    //$$ }
    //#endif

    @Override
    public boolean isInvisible() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this) {
            return view.isInvisible();
        }
        return super.isInvisible();
    }

    //#if MC>=12109
    @Override
    public PlayerSkin getSkin() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof AbstractClientPlayer) {
            return ((AbstractClientPlayer) view).getSkin();
        }
        return super.getSkin();
    }
    //#elseif MC>=12002
    //$$ @Override
    //$$ public PlayerSkin getPlayerSkin() {
    //$$     Entity view = this.minecraft.getCameraEntity();
    //$$     if (view != this && view instanceof AbstractClientPlayer) {
    //$$         return ((AbstractClientPlayer) view).getPlayerSkin();
    //$$     }
    //$$     return super.getPlayerSkin();
    //$$ }
    //#else
    //$$ @Override
    //$$ public Identifier getSkinTexture() {
    //$$     Entity view = this.minecraft.getCameraEntity();
    //$$     if (view != this && view instanceof AbstractClientPlayer) {
    //$$         return ((AbstractClientPlayer) view).getSkinTexture();
    //$$     }
    //$$     return super.getSkinTexture();
    //$$ }
    //$$
    //#if MC>=10800
    //$$ @Override
    //$$ public String getModel() {
    //$$     Entity view = this.minecraft.getCameraEntity();
    //$$     if (view != this && view instanceof AbstractClientPlayer) {
    //$$         return ((AbstractClientPlayer) view).getModel();
    //$$     }
    //$$     return super.getModel();
    //$$ }
    //#endif
    //#endif

    //#if MC>=12109
    // isModelPartVisible removed from supertype in MC 26.1 - model parts always visible for spectated players
    //#elseif MC>=10800
    //$$ @Override
    //$$ public boolean isPartVisible(PlayerModelPart modelPart) {
    //$$     Entity view = this.minecraft.getCameraEntity();
    //$$     if (view != this && view instanceof Player) {
    //$$         return ((Player) view).isPartVisible(modelPart);
    //$$     }
    //$$     return super.isPartVisible(modelPart);
    //$$ }
    //#endif

    //#if MC>=10904
    @Override
    public HumanoidArm getMainArm() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).getMainArm();
        }
        return super.getMainArm();
    }
    //#endif

    // getHandSwingProgress removed in MC 26.1
    // getAttackCooldownProgressPerTick removed in MC 26.1 (use getAttackStrengthScale)
    // getAttackCooldownProgress removed in MC 26.1
    // getActiveHand removed in MC 26.1

    //#if MC>=10904
    @Override
    public boolean isUsingItem() {
        Entity view = this.minecraft.getCameraEntity();
        if (view != this && view instanceof Player) {
            return ((Player) view).isUsingItem();
        }
        return super.isUsingItem();
    }

    //#if MC>=11400
    @Override
    //#if MC>=11900
    public void onEquipItem(EquipmentSlot slot, ItemStack stack, ItemStack itemStack) {
    //#else
    //$$ protected void onEquipStack(ItemStack itemStack_1) {
    //#endif
        // Suppress equip sounds
    }
    //#endif

    //#if MC>=11400
    @Override
    public HitResult pick(double maxDistance, float tickDelta, boolean fluids) {
        HitResult result = super.pick(maxDistance, tickDelta, fluids);

        // Make sure we can never look at blocks (-> no outline)
        if (result instanceof BlockHitResult) {
            BlockHitResult blockResult = (BlockHitResult) result;
            result = BlockHitResult.miss(result.getLocation(), blockResult.getDirection(), blockResult.getBlockPos());
        }

        return result;
    }
    //#else
    //#if MC>=11400
    //$$ @Override
    //$$ public RayTraceResult rayTrace(double blockReachDistance, float partialTicks, RayTraceFluidMode p_174822_4_) {
    //$$     RayTraceResult pos = super.rayTrace(blockReachDistance, partialTicks, p_174822_4_);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.type == RayTraceResult.Type.BLOCK) {
    //$$         pos.type = RayTraceResult.Type.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#else
    //$$ @Override
    //$$ public RayTraceResult rayTrace(double p_174822_1_, float p_174822_3_) {
    //$$     RayTraceResult pos = super.rayTrace(p_174822_1_, 1f);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.typeOfHit == RayTraceResult.Type.BLOCK) {
    //$$         pos.typeOfHit = RayTraceResult.Type.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#endif
    //#endif
    //#else
    //$$ @Override
    //$$ public MovingObjectPosition rayTrace(double p_174822_1_, float p_174822_3_) {
    //$$     MovingObjectPosition pos = super.rayTrace(p_174822_1_, 1f);
    //$$
    //$$     // Make sure we can never look at blocks (-> no outline)
    //$$     if(pos != null && pos.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
    //$$         pos.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
    //$$     }
    //$$
    //$$     return pos;
    //$$ }
    //#endif

    //#if MC<11400
    //$$ @Override
    //$$ public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {
    //$$     // Do not open any block GUIs for the camera entities
    //$$     // Note: Vanilla GUIs are filtered out on a packet level, this only applies to mod GUIs
    //$$ }
    //#endif

    @Override
    //#if MC>=11700
    public void remove(RemovalReason reason) {
        super.remove(reason);
    //#else
    //$$ public void remove() {
    //$$     super.remove();
    //#endif
        if (eventHandler != null) {
            eventHandler.unregister();
            eventHandler = null;
        }
    }

    private void update() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != this.level()) {
            if (eventHandler != null) {
                eventHandler.unregister();
                eventHandler = null;
            }
            return;
        }

        long now = System.currentTimeMillis();
        long timePassed = now - lastControllerUpdate;
        cameraController.update(timePassed / 50f);
        lastControllerUpdate = now;

        handleInputEvents();

        Map<String, KeyBindingRegistry.Binding> keyBindings = ReplayMod.instance.getKeyBindingRegistry().getBindings();
        if (keyBindings.get("replaymod.input.rollclockwise").keyBinding.isDown()) {
            roll += Utils.isCtrlDown() ? 0.2 : 1;
        }
        if (keyBindings.get("replaymod.input.rollcounterclockwise").keyBinding.isDown()) {
            roll -= Utils.isCtrlDown() ? 0.2 : 1;
        }

        //#if MC>=10800
        this.noPhysics = this.isSpectator();
        //#endif

        syncInventory();
    }

    private final Inventory originalInventory = this.getInventory();

    // If we are spectating a player, "steal" its inventory so the rendering code knows what item(s) to render
    // and if we aren't, then reset ours.
    private void syncInventory() {
        Entity view = this.minecraft.getCameraEntity();
        Player viewPlayer = view != this && view instanceof Player ? (Player) view : null;
        EntityPlayerAccessor cameraA = (EntityPlayerAccessor) this;
        EntityPlayerAccessor viewPlayerA = (EntityPlayerAccessor) viewPlayer;

        //#if MC>=11100
        ItemStack empty = ItemStack.EMPTY;
        //#else
        //$$ ItemStack empty = null;
        //#endif

        // TODO switch to replacing the entire inventory for 1.14+ as well, should be easier and faster
        //#if MC>=11400
        this.setItemSlot(EquipmentSlot.HEAD, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.HEAD) : empty);
        this.setItemSlot(EquipmentSlot.MAINHAND, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.MAINHAND) : empty);
        this.setItemSlot(EquipmentSlot.OFFHAND, viewPlayer != null ? viewPlayer.getItemBySlot(EquipmentSlot.OFFHAND) : empty);
        //#else
        //$$ this.inventory = viewPlayer != null ? viewPlayer.inventory : originalInventory;
        //#endif

        //#if MC>=10904
        cameraA.setItemStackMainHand(viewPlayerA != null ? viewPlayerA.getItemStackMainHand() : empty);
        this.swingingArm = viewPlayer != null ? viewPlayer.swingingArm : InteractionHand.MAIN_HAND;
        this.useItem = viewPlayer != null ? viewPlayer.getActiveItem() : empty;
        cameraA.setActiveItemStackUseCount(viewPlayerA != null ? viewPlayerA.getActiveItemStackUseCount() : 0);
        //#else
        //$$ cameraA.setItemInUse(viewPlayerA != null ? viewPlayerA.getItemInUse() : empty);
        //$$ cameraA.setItemInUseCount(viewPlayerA != null ? viewPlayerA.getItemInUseCount() : 0);
        //#endif
    }

    private void handleInputEvents() {
        if (this.minecraft.options.keyAttack.consumeClick() || this.minecraft.options.keyUse.consumeClick()) {
            if (this.minecraft.screen == null && canSpectate(this.minecraft.crosshairPickEntity)) {
                ReplayModReplay.instance.getReplayHandler().spectateEntity(
                        //#if MC<=10710
                        //$$ (EntityLivingBase)
                        //#endif
                        this.minecraft.crosshairPickEntity);
                // Make sure we don't exit right away
                //noinspection StatementWithEmptyBody
                while (this.minecraft.options.keyShift.consumeClick());
            }
        }
    }

    private void updateArmYawAndPitch() {
        this.lastRenderYaw = this.renderYaw;
        this.lastRenderPitch = this.renderPitch;
        this.renderPitch = this.renderPitch +  (this.getXRot() - this.renderPitch) * 0.5f;
        this.renderYaw = this.renderYaw + wrapDegrees(this.getYRot() - this.renderYaw) * 0.5f;
        this.wrapArmYaw();
    }

    /**
     * Minecraft renders the arm offset based on the difference between {@link #yaw} and {@link #renderYaw}. It does not
     * wrap around the difference though, so if {@link #yaw} just wrapped around from 350 to 10 but {@link #renderYaw}
     * is still at 355, then the difference will be inappropriately large. To fix this, we always wrap the
     * {@link #renderYaw} such that it is no more than 180 degrees away from {@link #yaw}, even if that requires going
     * outside the normal range.
     */
    private void wrapArmYaw() {
        this.renderYaw = wrapDegreesTo(this.renderYaw, this.getYRot());
        this.lastRenderYaw = wrapDegreesTo(this.lastRenderYaw, this.renderYaw);
    }

    private static float wrapDegreesTo(float value, float towardsValue) {
        while (towardsValue - value < -180) {
            value -= 360;
        }
        while (towardsValue - value >= 180) {
            value += 360;
        }
        return value;
    }

    private static float wrapDegrees(float value) {
        value %= 360;
        return wrapDegreesTo(value, 0);
    }

    public boolean canSpectate(Entity e) {
        return e != null
                //#if MC<10800
                //$$ && e instanceof EntityPlayer // cannot be more generic since 1.7.10 has no concept of eye height
                //#endif
                && !e.isInvisible();
    }

    //#if MC<11400
    //#if MC>=11102
    //$$ @Override
    //$$ public void sendMessage(ITextComponent message) {
    //$$     if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
    //$$     super.sendMessage(message);
    //$$ }
    //#else
    //$$ @Override
    //$$ public void addChatMessage(ITextComponent message) {
    //$$     if (MinecraftForge.EVENT_BUS.post(new ReplayChatMessageEvent(this))) return;
    //$$     super.addChatMessage(message);
    //$$ }
    //#endif
    //#endif

    //#if MC>=10800
    private
    //#else
    //$$ public // All event handlers need to be public in 1.7.10
    //#endif
    class EventHandler extends EventRegistrations {
        private final Minecraft mc = getMinecraft();

        private EventHandler() {}

        { on(PreTickCallback.EVENT, this::onPreClientTick); }
        private void onPreClientTick() {
            updateArmYawAndPitch();
        }

        { on(PreRenderCallback.EVENT, this::onRenderUpdate); }
        private void onRenderUpdate() {
            update();
        }

        { on(KeyBindingEventCallback.EVENT, CameraEntity.this::handleInputEvents); }

        { on(RenderSpectatorCrosshairCallback.EVENT, this::shouldRenderSpectatorCrosshair); }
        private Boolean shouldRenderSpectatorCrosshair() {
            return canSpectate(mc.crosshairPickEntity);
        }

        { on(RenderHotbarCallback.EVENT, this::shouldRenderHotbar); }
        private Boolean shouldRenderHotbar() {
            return false;
        }

        { on(SettingsChangedCallback.EVENT, this::onSettingsChanged); }
        private void onSettingsChanged(SettingsRegistry registry, SettingsRegistry.SettingKey<?> key) {
            if (key == Setting.CAMERA) {
                if (ReplayModReplay.instance.getReplayHandler().getSpectatedUUID() == null) {
                    cameraController = ReplayModReplay.instance.createCameraController(CameraEntity.this);
                } else {
                    cameraController = new SpectatorCameraController(CameraEntity.this);
                }
            }
        }

        { on(PreRenderHandCallback.EVENT, this::onRenderHand); }
        private boolean onRenderHand() {
            // Unless we are spectating another player, don't render our hand
            Entity view = mc.getCameraEntity();
            if (view == CameraEntity.this || !(view instanceof Player)) {
                return true; // cancel hand rendering
            } else {
                Player player = (Player) view;
                // When the spectated player has changed, force equip their items to prevent the equip animation
                if (lastHandRendered != player) {
                    lastHandRendered = player;

                    FirstPersonRendererAccessor acc = (FirstPersonRendererAccessor) mc.gameRenderer.itemInHandRenderer;
                    //#if MC>=10904
                    acc.setPrevEquippedProgressMainHand(1);
                    acc.setPrevEquippedProgressOffHand(1);
                    acc.setEquippedProgressMainHand(1);
                    acc.setEquippedProgressOffHand(1);
                    acc.setItemStackMainHand(player.getItemBySlot(EquipmentSlot.MAINHAND));
                    acc.setItemStackOffHand(player.getItemBySlot(EquipmentSlot.OFFHAND));
                    //#else
                    //$$ acc.setPrevEquippedProgress(1);
                    //$$ acc.setEquippedProgress(1);
                    //$$ acc.setItemToRender(player.inventory.getCurrentItem());
                    //$$ acc.setEquippedItemSlot(player.inventory.currentItem);
                    //#endif


                    if (mc.player instanceof CameraEntity cam) {
                        cam.renderYaw = cam.lastRenderYaw = player.getYRot();
                        cam.renderPitch = cam.lastRenderPitch = player.getXRot();
                    }
                }
                return false;
            }
        }

        //#if MC>=11400
        // Moved to MixinCamera
        //#else
        //#if MC>=10800
        //$$ @SubscribeEvent
        //$$ public void onEntityViewRenderEvent(EntityViewRenderEvent.CameraSetup event) {
        //$$     if (mc.getRenderViewEntity() == CameraEntity.this) {
                //#if MC>=10904
                //$$ event.setRoll(roll);
                //#else
                //$$ event.roll = roll;
                //#endif
        //$$     }
        //$$ }
        //#endif
        //#endif

        private boolean heldItemTooltipsWasTrue;

        // NeoForge: RenderGameOverlayEvent removed; HUD suppression not implemented (TODO)
    }
}
