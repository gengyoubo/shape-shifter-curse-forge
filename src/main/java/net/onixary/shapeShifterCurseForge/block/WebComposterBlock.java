package net.onixary.shapeShifterCurseForge.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

/** Fabric's three-meat nutrient cocoon composter. */
public final class WebComposterBlock extends Block {
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 4);
    private static final TagKey<Item> MEAT = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "meat"));

    public WebComposterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        int progress = state.getValue(LEVEL);
        if (progress < 3 && canCompost(held)) {
            if (!level.isClientSide) {
                if (progress == 0 || level.random.nextFloat() < chance(held)) {
                    BlockState next = state.setValue(LEVEL, progress + 1);
                    level.setBlock(pos, next, 3);
                    level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                    if (progress + 1 == 3) level.scheduleTick(pos, this, 20);
                }
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (progress == 4) {
            if (!level.isClientSide) {
                int count = 4 + level.random.nextInt(3);
                if (!player.addItem(new ItemStack(ModItems.SPIDER_FLUID_COCOON.get(), count))) {
                    player.drop(new ItemStack(ModItems.SPIDER_FLUID_COCOON.get(), count), false);
                }
                level.setBlock(pos, state.setValue(LEVEL, 0), 3);
                level.playSound(null, pos, SoundEvents.COMPOSTER_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LEVEL) == 3) {
            level.setBlock(pos, state.setValue(LEVEL, 4), 3);
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private static boolean canCompost(ItemStack stack) {
        return stack.is(MEAT) || stack.getItem().getFoodProperties() != null;
    }

    private static float chance(ItemStack stack) {
        return stack.getItem().getFoodProperties() != null ? 0.55F : 0.5F;
    }

    /** JEI-facing variant of {@link #canCompost}. */
    public static boolean canIncrease(ItemStack stack) {
        return canCompost(stack);
    }

    /** JEI-facing variant of {@link #chance}. */
    public static float getIncreaseChance(ItemStack stack) {
        return chance(stack);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }
}
