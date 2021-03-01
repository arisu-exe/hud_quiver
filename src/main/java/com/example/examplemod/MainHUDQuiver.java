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
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.*;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

@Mod(MainHUDQuiver.MODID)
public class MainHUDQuiver {
    public static final String MODID = "hud_quiver";
    private static final Logger LOGGER = LogManager.getLogger();
    //private static @Nullable ServerPlayerEntity player;
    private static @Nullable ClientPlayerEntity player;

    public MainHUDQuiver() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    }
    private void doClientStuff(final FMLClientSetupEvent event) {
    }
    private void enqueueIMC(final InterModEnqueueEvent event) {
    }
    private void processIMC(final InterModProcessEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
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
            } else {
                predicate = ((ShootableItem)shootable.getItem()).getInventoryAmmoPredicate();

                for(int i = 0; i < player.inventory.getSizeInventory(); ++i) {
                    ItemStack itemstack1 = player.inventory.getStackInSlot(i);
                    if (predicate.test(itemstack1)) {
                        list.add(itemstack1);
                    }
                }

                if(list.isEmpty() && player.abilities.isCreativeMode) {
                    list.add(new ItemStack(Items.ARROW));
                }
            }

            return list;
        }
    }

    @Mod.EventBusSubscriber
    public static class Events {
        /*@SubscribeEvent
        public static void onPlayerTick(final TickEvent.PlayerTickEvent event) {
            if(event.player instanceof ServerPlayerEntity) {
                player = (ServerPlayerEntity) event.player;
            }
        }*/
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            if(player == null) {
                player = Minecraft.getInstance().player;
            }
            if(event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR && player != null) {
                ItemStack playerHand;
                if(player.getHeldItemMainhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemMainhand();
                } else if(player.getHeldItemOffhand().getItem() instanceof ShootableItem) {
                    playerHand = player.getHeldItemOffhand();
                } else {
                    return;
                }

                List<ItemStack> readyArrows = findAmmos(player, playerHand);

                for(int i = 0; i < readyArrows.size(); ++ i) {
                    ItemStack readyArrow = readyArrows.get(i);

                    event.getMatrixStack().push();
                    int x = 16 * (i + 1), y = 16;
                    event.getMatrixStack().translate(x, y, 0);
                    event.getMatrixStack().scale(16, -16, 1);
                    IRenderTypeBuffer.Impl buffer = Minecraft.getInstance().getRenderTypeBuffers().getBufferSource();
                    RenderSystem.enableBlend();

                    Minecraft.getInstance().getItemRenderer().renderItem(readyArrow, ItemCameraTransforms.TransformType.GUI, 15728880, OverlayTexture.NO_OVERLAY, event.getMatrixStack(), buffer);
                    RenderSystem.disableBlend();
                    buffer.finish();
                    event.getMatrixStack().pop();
                    if(readyArrow.getCount() > 0) {
                        event.getMatrixStack().push();
                        if(player.isCreative() || readyArrow.getItem() instanceof ArrowItem && ((ArrowItem) readyArrow.getItem()).isInfinite(readyArrow, playerHand, player)) {
                            AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent("Infinity"), x, y + 16, 16777215);
                        } else {
                            AbstractGui.drawString(event.getMatrixStack(), Minecraft.getInstance().fontRenderer, new StringTextComponent(String.valueOf(readyArrow.getCount())), x, y + 16, player.getItemInUseCount() > 0 ? 16777045 : 16777215);
                        }
                        event.getMatrixStack().pop();
                    }
                }
            }
        }
    }
}