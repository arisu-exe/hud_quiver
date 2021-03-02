package com.example.examplemod;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ShootableItem;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mod(MainHUDQuiver.MODID)
public class MainHUDQuiver {
    public static final String MODID = "hud_quiver";
    private static final ResourceLocation WIDGETS = new ResourceLocation("hud_quiver","textures/gui/widgets.png");

    public MainHUDQuiver() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public static List<ItemStack> findAmmos(PlayerEntity player, ItemStack shootable) {
        if (!(shootable.getItem() instanceof ShootableItem)) {
            return Lists.newLinkedList();
        } else {
            List<ItemStack> list = Lists.newLinkedList();

            Predicate<ItemStack> predicate = ((ShootableItem)shootable.getItem()).getAmmoPredicate();
            ItemStack itemstack = ShootableItem.getHeldAmmo(player, predicate);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
            }
            predicate = ((ShootableItem)shootable.getItem()).getInventoryAmmoPredicate();

            for(int i = 0; i < player.inventory.getSizeInventory(); ++i) {
                ItemStack itemstack1 = player.inventory.getStackInSlot(i);
                if (predicate.test(itemstack1) && itemstack1 != itemstack) {
                    list.add(itemstack1);
                }
            }

            if(list.isEmpty() && player.abilities.isCreativeMode) {
                list.add(new ItemStack(Items.ARROW));
            }

            return list;
        }
    }

    @Mod.EventBusSubscriber
    public static class Events {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        @SuppressWarnings("unused")
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            @Nullable ClientPlayerEntity player = Minecraft.getInstance().player;

            if(event.getType() == RenderGameOverlayEvent.ElementType.ALL && player != null) {
                ItemStack playerHand;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemMainhand();
                } else if(player.getHeldItemOffhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemOffhand();
                } else {
                    return;
                }

                event.getMatrixStack().push();
                event.getMatrixStack().translate(16, 16, 0);
                Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS);
                AbstractGui.blit(event.getMatrixStack(), -12, -12, 0, 0, 24, 24, 36, 24);
                event.getMatrixStack().pop();

                List<ItemStack> readyArrows = findAmmos(player, playerHand);
                List<Integer> skips = Lists.newLinkedList();
                int xMultiplier = 0;

                for(int i = 0; i < readyArrows.size(); ++ i) {
                    if(skips.contains(i)) {
                        continue;
                    }

                    ItemStack readyArrow = readyArrows.get(i);
                    int x = 24 * xMultiplier + 16, y = 16;

                    event.getMatrixStack().push();
                    event.getMatrixStack().translate(x, y, 1);
                    event.getMatrixStack().scale(16, -16, 1);
                    IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                    RenderSystem.enableBlend();
                    Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.GUI, 15728880, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                    RenderSystem.disableBlend();
                    buffer.finish();
                    event.getMatrixStack().pop();

                    int count = readyArrow.getCount();

                    for(int j = i + 1; j < readyArrows.size(); ++ j) {
                        ItemStack nextArrow = readyArrows.get(j);
                        if(nextArrow.isItemEqual(readyArrow) && ItemStack.areItemStackTagsEqual(nextArrow, readyArrow)) {
                            count += nextArrow.getCount();
                            skips.add(j);
                        } else {
                            break;
                        }
                    }

                    event.getMatrixStack().push();
                    if(player.isCreative() || readyArrow.getItem() instanceof ArrowItem && ((ArrowItem) readyArrow.getItem()).isInfinite(readyArrow, playerHand, player)) {
                        event.getMatrixStack().push();
                        event.getMatrixStack().translate(x + 3, y + 5, 0);
                        Minecraft.getInstance().getTextureManager().bindTexture(WIDGETS);
                        AbstractGui.blit(event.getMatrixStack(), -6, -4, 24, 0, 12, 8, 36, 24);
                        event.getMatrixStack().pop();
                    } else {
                        boolean using = player.getItemInUseCount() > 0 && readyArrow == player.findAmmo(playerHand);
                        String displayCount = using ? String.valueOf(count - 1) : String.valueOf(count); // TODO make text red is 0 is remaining
                        AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent(displayCount), x - 3, y + 1, using ? 16777045 : 16777215); // TODO anchor text to right alignment
                    }
                    event.getMatrixStack().pop();

                    ++ xMultiplier;
                }
            }
        }
    }
}