package net.onixary.shapeShifterCurseForge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.onixary.shapeShifterCurseForge.blockentity.AlterBlockEntity;
import net.onixary.shapeShifterCurseForge.registry.ModBlockEntities;
import org.jetbrains.annotations.Nullable;

public class AlterBlock extends BaseEntityBlock {
    public AlterBlock(Properties p) { super(p); }
    @Override public RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlterBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l, BlockState s, BlockEntityType<T> t) {
        return l.isClientSide ? null : createTickerHelper(t, ModBlockEntities.ALTER.get(), AlterBlockEntity::tick);
    }
    @Override public InteractionResult use(BlockState st, Level lvl, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit) {
        if (lvl.isClientSide) return InteractionResult.SUCCESS;
        var be = lvl.getBlockEntity(pos);
        if (be instanceof AlterBlockEntity alter) {
            alter.lastUser = p.getUUID();
            if (p instanceof net.minecraft.server.level.ServerPlayer sp) net.minecraftforge.network.NetworkHooks.openScreen(sp, alter, pos);
        }
        return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState a, Level b, BlockPos c, BlockState d, boolean e) {
        if (!a.is(d.getBlock())) {
            var be = b.getBlockEntity(c);
            if (be instanceof AlterBlockEntity alter) net.minecraft.world.Containers.dropContents(b,c,alter);
        }
        super.onRemove(a,b,c,d,e);
    }
}
