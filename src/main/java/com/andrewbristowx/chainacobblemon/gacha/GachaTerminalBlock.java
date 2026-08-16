package com.andrewbristowx.chainacobblemon.gacha;

import com.andrewbristowx.chainacobblemon.registry.ChainaRegistries;
import com.andrewbristowx.chainacobblemon.systems.SystemsNetworking;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/** Simple functional machine for alpha.4; final animated Chaina machine comes later. */
public final class GachaTerminalBlock extends Block {
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;
    public static final MapCodec<GachaTerminalBlock> CODEC = createCodec(GachaTerminalBlock::new);

    public GachaTerminalBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override protected MapCodec<? extends Block> getCodec() { return CODEC; }

    @Nullable
    @Override public BlockState getPlacementState(ItemPlacementContext context) {
        return getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        String banner = state.isOf(ChainaRegistries.CHAINA_GACHA_MACHINE) ? "chaina" : "standard";
        SystemsNetworking.openGacha(serverPlayer, banner, "");
        return ActionResult.SUCCESS;
    }

    @Override protected BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }
    @Override protected void appendProperties(StateManager.Builder<Block, BlockState> builder) { builder.add(FACING); }
}
