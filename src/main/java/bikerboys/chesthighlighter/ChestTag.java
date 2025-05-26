package bikerboys.chesthighlighter;

import net.minecraft.util.math.BlockPos;

public class ChestTag {
    public final BlockPos pos;
    public final String itemName;

    public ChestTag(BlockPos pos, String itemName) {
        this.pos = pos;
        this.itemName = itemName;
    }
}
