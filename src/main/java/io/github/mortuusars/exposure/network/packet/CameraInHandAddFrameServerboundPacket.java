package io.github.mortuusars.exposure.network.packet;

import com.google.common.base.Preconditions;
import io.github.mortuusars.exposure.item.CameraItem;
import io.github.mortuusars.exposure.network.Packets;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.Registry;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public record CameraInHandAddFrameServerboundPacket(InteractionHand hand, CompoundTag frame) {
    public static void register(SimpleChannel channel, int id) {
        channel.messageBuilder(CameraInHandAddFrameServerboundPacket.class, id, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CameraInHandAddFrameServerboundPacket::toBuffer)
                .decoder(CameraInHandAddFrameServerboundPacket::fromBuffer)
                .consumerMainThread(CameraInHandAddFrameServerboundPacket::handle)
                .add();
    }

    public static void send(InteractionHand hand, CompoundTag frame) {
        Packets.sendToServer(new CameraInHandAddFrameServerboundPacket(hand, frame));
    }

    public void toBuffer(FriendlyByteBuf buffer) {
        buffer.writeEnum(hand);
        buffer.writeNbt(frame);
    }

    public static CameraInHandAddFrameServerboundPacket fromBuffer(FriendlyByteBuf buffer) {
        InteractionHand hand = buffer.readEnum(InteractionHand.class);
        @Nullable CompoundTag frame = buffer.readAnySizeNbt();
        if (frame == null)
            frame = new CompoundTag();
        return new CameraInHandAddFrameServerboundPacket(hand, frame);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        @Nullable ServerPlayer player = context.getSender();
        Preconditions.checkState(player != null, "Cannot handle packet: Player was null");

        ItemStack itemInHand = player.getItemInHand(hand);
        if (!(itemInHand.getItem() instanceof CameraItem cameraItem))
            throw new IllegalStateException("Item in hand in not a Camera.");

        addStructuresInfo(player);

        cameraItem.exposeFilmFrame(itemInHand, frame);
//        player.setItemInHand(hand, itemInHand);
        return true;
    }

    private void addStructuresInfo(@NotNull ServerPlayer player) {
        Map<Structure, LongSet> allStructuresAt = player.getLevel().structureManager().getAllStructuresAt(player.blockPosition());

        List<Structure> inside = new ArrayList<>();

        for (Structure structure : allStructuresAt.keySet()) {
            StructureStart structureAt = player.getLevel().structureManager().getStructureAt(player.blockPosition(), structure);
            if (structureAt.isValid()) {
                inside.add(structure);
            }
        }

        Registry<Structure> structures = player.getLevel().registryAccess().registryOrThrow(BuiltinRegistries.STRUCTURES.key());
        ListTag structuresTag = new ListTag();

        for (Structure structure : inside) {
            ResourceLocation key = structures.getKey(structure);
            if (key != null)
                structuresTag.add(StringTag.valueOf(key.toString()));
        }

        if (structuresTag.size() > 0) {
            frame.put("Structures", structuresTag);
        }
    }
}
