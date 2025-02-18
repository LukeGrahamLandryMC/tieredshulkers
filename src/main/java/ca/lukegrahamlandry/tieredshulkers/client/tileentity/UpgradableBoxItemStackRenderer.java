package ca.lukegrahamlandry.tieredshulkers.client.tileentity;

import ca.lukegrahamlandry.tieredshulkers.common.ShulkerColour;
import ca.lukegrahamlandry.tieredshulkers.common.boxes.UpgradableBoxBlock;
import ca.lukegrahamlandry.tieredshulkers.common.boxes.UpgradableBoxTier;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public class UpgradableBoxItemStackRenderer<T extends BlockEntity> extends BlockEntityWithoutLevelRenderer {
  private static HashMap<UpgradableBoxTier, HashMap<ShulkerColour, BlockEntity>> memoizedTiles;

  private static void init() {
    memoizedTiles = new HashMap<>();
    for (UpgradableBoxTier tier : UpgradableBoxTier.values()){
      HashMap<ShulkerColour, BlockEntity> map = new HashMap<>();
      for (ShulkerColour color : ShulkerColour.values()){
        map.put(color, tier.tiles.get(color).get().create(BlockPos.ZERO, tier.blocks.get(color).get().defaultBlockState()));
      }
      memoizedTiles.put(tier, map);
    }
  }

  public UpgradableBoxItemStackRenderer() {
    super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
  }

  @Override
  public void renderByItem(ItemStack stack, ItemTransforms.TransformType transformType, PoseStack matrixStackIn, MultiBufferSource bufferIn, int combinedLightIn, int combinedOverlayIn) {
    if (memoizedTiles == null) init();

    UpgradableBoxBlock block = ((UpgradableBoxBlock)((BlockItem)stack.getItem()).getBlock());
    BlockEntity tile = memoizedTiles.get(block.getTier()).get(block.getColor());
    Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(tile, matrixStackIn, bufferIn, combinedLightIn, combinedOverlayIn);
  }
}
