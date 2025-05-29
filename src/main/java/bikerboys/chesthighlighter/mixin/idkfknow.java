package bikerboys.chesthighlighter.mixin;


import bikerboys.chesthighlighter.ChestHighlighter;
import bikerboys.chesthighlighter.ChestTag;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.AbstractList;
import java.util.ArrayList;

@Mixin(ChestBlockEntity.class)
public class idkfknow {



    @Inject(method = "<init>(Lnet/minecraft/block/entity/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V", at = @At("TAIL"))
    private void whatevers(BlockEntityType blockEntityType, BlockPos pos, BlockState state, CallbackInfo ci) {
        ChestBlockEntity chestBlock = (ChestBlockEntity)(Object)this;
        BlockPos blockPos = chestBlock.getPos();



        }
    }

