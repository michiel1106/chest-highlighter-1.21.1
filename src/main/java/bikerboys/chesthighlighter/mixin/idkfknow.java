package bikerboys.chesthighlighter.mixin;


import bikerboys.chesthighlighter.ChestHighlighter;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class idkfknow {



    @Inject(method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("TAIL"))
    private void whatevers(BlockEntityType blockEntityType, BlockPos pos, BlockState state, CallbackInfo ci) {
        ChestBlockEntity chestBlock = (ChestBlockEntity)(Object)this;
        BlockPos blockPos = chestBlock.getPos();









            if (ChestHighlighter.world != null) {
                if(!ChestHighlighter.world.isClient) {


                    if (!ChestHighlighter.chestpos.containsKey(blockPos)) {
                        ChestHighlighter.chestpos.put(chestBlock, blockPos);
                    }
                }
            }
        }
    }

