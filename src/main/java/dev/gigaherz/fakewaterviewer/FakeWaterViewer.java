package dev.gigaherz.fakewaterviewer;

import com.mojang.serialization.codecs.KeyDispatchCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3i;

import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(FakeWaterViewer.MODID)
public class FakeWaterViewer
{
    public static final String MODID = "fakewaterviewer";

    public FakeWaterViewer(IEventBus modEventBus)
    {
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientHandlers
    {
        @SubscribeEvent
        public static void reanderLast(RenderLevelStageEvent event)
        {
            if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS)
                return;

            var mc = Minecraft.getInstance();
            var player = mc.player;

            if (player == null)
                return;

            if (!player.isHolding(Items.KELP))
                return;

            var level = Objects.requireNonNull(mc.level);
            var pose = event.getPoseStack();
            var buffers = mc.renderBuffers();
            var bufferSource = buffers.bufferSource();
            var entityRenderDispatcher = mc.getEntityRenderDispatcher();
            var itemRenderer = mc.getItemRenderer();
            var partialTicks = mc.getFrameTime();
            var eyePosition = player.getEyePosition(partialTicks);
            var camPosition = event.getCamera().getPosition();
            var kelp = Items.KELP.getDefaultInstance();

            pose.pushPose();
            pose.translate(-camPosition.x, -camPosition.y, -camPosition.z);

            var center = BlockPos.containing(eyePosition);
            for(var pos : BlockPos.betweenClosed(center.offset(-16,-16,-16), center.offset(16,16,16)))
            {
                var state = level.getFluidState(pos);
                if (!state.is(Fluids.FLOWING_WATER) || state.getValue(WaterFluid.LEVEL) < 8)
                    continue;

                var renderPos = Vec3.atCenterOf(pos);

                pose.pushPose();
                pose.translate(renderPos.x, renderPos.y, renderPos.z);
                pose.scale(0.5f,0.5f, 0.5f);

                pose.mulPose(entityRenderDispatcher.cameraOrientation());

                itemRenderer.renderStatic(null, kelp, ItemDisplayContext.NONE, false, pose, bufferSource, level, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);

                pose.popPose();
            }

            bufferSource.endBatch();

            pose.popPose();
        }
    }
}
