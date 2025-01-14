package ca.lukegrahamlandry.tieredshulkers.common.boxes.tile;

import ca.lukegrahamlandry.tieredshulkers.common.ShulkerColour;
import ca.lukegrahamlandry.tieredshulkers.common.boxes.UpgradableBoxBlock;
import ca.lukegrahamlandry.tieredshulkers.common.boxes.UpgradableBoxContainer;
import ca.lukegrahamlandry.tieredshulkers.common.boxes.UpgradableBoxTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

public class UpgradableBoxTile extends RandomizableContainerBlockEntity implements WorldlyContainer {
  private final int[] SLOTS;
  private NonNullList<ItemStack> items;
  private int openCount;
  private UpgradableBoxTile.AnimationStatus animationStatus = UpgradableBoxTile.AnimationStatus.CLOSED;
  private float progress;
  private float progressOld;
  private boolean needsColorFromWorld;

  private ShulkerColour color;
  private UpgradableBoxTier tier;

  public UpgradableBoxTile(@Nullable ShulkerColour color, UpgradableBoxTier tier, BlockPos pos, BlockState state) {
    super(tier.tiles.get(color).get(), pos, state);

    this.SLOTS = IntStream.range(0, tier.size).toArray();
    this.items = NonNullList.<ItemStack>withSize(tier.size, ItemStack.EMPTY);

    this.color = color;
    this.tier = tier;

    if (color == null) this.needsColorFromWorld = true;
  }

  public void tick() {
    this.updateAnimation();

    if (this.animationStatus == UpgradableBoxTile.AnimationStatus.OPENING || this.animationStatus == UpgradableBoxTile.AnimationStatus.CLOSING) {
      this.moveCollidedEntities();
    }
  }

  protected void updateAnimation() {
    this.progressOld = this.progress;

    switch (this.animationStatus) {
      case CLOSED:
        this.progress = 0.0F;

        break;
      case OPENING:
        this.progress += 0.1F;

        if (this.progress >= 1.0F) {
          this.moveCollidedEntities();
          this.animationStatus = UpgradableBoxTile.AnimationStatus.OPENED;
          this.progress = 1.0F;
          this.updateNeighbors();
        }

        break;
      case CLOSING:
        this.progress -= 0.1F;

        if (this.progress <= 0.0F) {
          this.animationStatus = UpgradableBoxTile.AnimationStatus.CLOSED;
          this.progress = 0.0F;
          this.updateNeighbors();
        }

        break;
      case OPENED:
        this.progress = 1.0F;
    }
  }

  public UpgradableBoxTile.AnimationStatus getAnimationStatus() {
    return this.animationStatus;
  }

  public AABB getBoundingBox(BlockState blockState) {
    return this.getBoundingBox(blockState.getValue(UpgradableBoxBlock.FACING));
  }

  public AABB getBoundingBox(Direction directionIn) {
    float f = this.getProgress(1.0F);

    return Shapes.block().bounds().expandTowards((double) (0.5F * f * (float) directionIn.getStepX()), (double) (0.5F * f * (float) directionIn.getStepY()), (double) (0.5F * f * (float) directionIn.getStepZ()));
  }

  private AABB getTopBoundingBox(Direction directionIn) {
    Direction direction = directionIn.getOpposite();
    return this.getBoundingBox(directionIn).contract((double) direction.getStepX(), (double) direction.getStepY(), (double) direction.getStepZ());
  }

  private void moveCollidedEntities() {
    BlockState blockstate = this.level.getBlockState(this.getBlockPos());

    if (blockstate.getBlock() instanceof UpgradableBoxBlock) {
      Direction direction = blockstate.getValue(UpgradableBoxBlock.FACING);
      AABB axisalignedbb = this.getTopBoundingBox(direction).move(this.worldPosition);
      List<Entity> list = this.level.getEntities((Entity) null, axisalignedbb);

      if (!list.isEmpty()) {
        for (Entity entity : list) {
          if (entity.getPistonPushReaction() != PushReaction.IGNORE) {
            double d0 = 0.0D;
            double d1 = 0.0D;
            double d2 = 0.0D;
            AABB boundingBox = entity.getBoundingBox();

            switch (direction.getAxis()) {
              case X:
                if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                  d0 = axisalignedbb.maxX - boundingBox.minX;
                }
                else {
                  d0 = boundingBox.maxX - axisalignedbb.minX;
                }

                d0 = d0 + 0.01D;

                break;
              case Y:
                if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                  d1 = axisalignedbb.maxY - boundingBox.minY;
                }
                else {
                  d1 = boundingBox.maxY - axisalignedbb.minY;
                }

                d1 = d1 + 0.01D;

                break;
              case Z:
                if (direction.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
                  d2 = axisalignedbb.maxZ - boundingBox.minZ;
                }
                else {
                  d2 = boundingBox.maxZ - axisalignedbb.minZ;
                }

                d2 = d2 + 0.01D;
            }

            entity.move(MoverType.SHULKER_BOX, new Vec3(d0 * (double) direction.getStepX(), d1 * (double) direction.getStepY(), d2 * (double) direction.getStepZ()));
          }
        }
      }
    }
  }

  @Override
  public int getContainerSize() {
    return this.items.size();
  }

  @Override
  public boolean triggerEvent(int id, int type) {
    if (id == 1) {
      this.openCount = type;

      if (type == 0) {
        this.animationStatus = UpgradableBoxTile.AnimationStatus.CLOSING;
        this.updateNeighbors();
      }

      if (type == 1) {
        this.animationStatus = UpgradableBoxTile.AnimationStatus.OPENING;
        this.updateNeighbors();
      }

      return true;
    }
    else {
      return super.triggerEvent(id, type);
    }
  }

  private void updateNeighbors() {
    this.getBlockState().updateNeighbourShapes(this.getLevel(), this.getBlockPos(), 3);
  }

  @Override
  public void startOpen(Player player) {
    if (!player.isSpectator()) {
      if (this.openCount < 0) {
        this.openCount = 0;
      }

      ++this.openCount;
      this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);

      if (this.openCount == 1) {
        this.level.playSound((Player) null, this.worldPosition, SoundEvents.SHULKER_BOX_OPEN, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
      }
    }
  }

  @Override
  public void stopOpen(Player player) {
    if (!player.isSpectator()) {
      --this.openCount;
      this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openCount);

      if (this.openCount <= 0) {
        this.level.playSound((Player) null, this.worldPosition, SoundEvents.SHULKER_BOX_CLOSE, SoundSource.BLOCKS, 0.5F, this.level.random.nextFloat() * 0.1F + 0.9F);
      }
    }
  }

  @Override
  protected Component getDefaultName() {
    return Component.translatable("container.shulkerBox");
  }

  @Override
  public void load(CompoundTag compound) {
    super.load(compound);
    this.loadFromNbt(compound);
  }

  @Override
  public void saveAdditional(CompoundTag compound) {
    super.saveAdditional(compound);
    this.saveToNbt(compound);
  }

  public void loadFromNbt(CompoundTag compound) {
    this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);

    if (!this.tryLoadLootTable(compound) && compound.contains("Items", 9)) {
      ContainerHelper.loadAllItems(compound, this.items);
    }
  }

  public CompoundTag saveToNbt(CompoundTag compound) {
    if (!this.trySaveLootTable(compound)) {
      ContainerHelper.saveAllItems(compound, this.items, false);
    }

    return compound;
  }

  @Override
  public NonNullList<ItemStack> getItems() {
    return this.items;
  }

  @Override
  public void setItems(NonNullList<ItemStack> itemsIn) {
    this.items = NonNullList.<ItemStack>withSize(this.getShulkerBoxType().size, ItemStack.EMPTY);

    for (int i = 0; i < itemsIn.size(); i++) {
      if (i < this.items.size()) {
        this.getItems().set(i, itemsIn.get(i));
      }
    }
  }

  @Override
  public int[] getSlotsForFace(Direction side) {
    return SLOTS;
  }

  @Override
  public boolean canPlaceItemThroughFace(int index, ItemStack itemStackIn, @Nullable Direction direction) {
    return !(Block.byItem(itemStackIn.getItem()) instanceof ShulkerBoxBlock) || !(Block.byItem(itemStackIn.getItem()) instanceof UpgradableBoxBlock);
  }

  @Override
  public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
    return true;
  }

  public float getProgress(float p_190585_1_) {
    return Mth.lerp(p_190585_1_, this.progressOld, this.progress);
  }

  @Nullable
  public ShulkerColour getColor() {
    if (this.needsColorFromWorld) {
      this.color = UpgradableBoxBlock.getColorFromBlock(this.getBlockState().getBlock());
      this.needsColorFromWorld = false;
    }

    return this.color;
  }


//
//  @Override
//  protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
//    return tieredshulkersContainer.createObsidianContainer(id, playerInventory, this);
//  }
  @Override
  protected AbstractContainerMenu createMenu(int windowId, Inventory playerInventory) {
    return new UpgradableBoxContainer(this.tier, windowId, playerInventory, this);
  }

  @Override
  protected net.minecraftforge.items.IItemHandler createUnSidedHandler() {
    return new net.minecraftforge.items.wrapper.SidedInvWrapper(this, Direction.UP);
  }

  public UpgradableBoxTier getShulkerBoxType() {
    UpgradableBoxTier type = UpgradableBoxTier.IRON;

    if (this.hasLevel()) {
      UpgradableBoxTier typeNew = UpgradableBoxBlock.getTypeFromBlock(this.getBlockState().getBlock());

      if (typeNew != null) {
        type = typeNew;
      }
    }

    return type;
  }

  public Block getBlockToUse() {
    return this.tier.blocks.get(this.color).get();
  }

  public boolean isClosed() {
    return this.animationStatus == UpgradableBoxTile.AnimationStatus.CLOSED;
  }

  public enum AnimationStatus {
    CLOSED,
    OPENING,
    OPENED,
    CLOSING;
  }
}
