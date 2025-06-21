package com.leclowndu93150.inventorymanagement.mixin;

import com.leclowndu93150.inventorymanagement.duck.AbstractContainerScreenAccessor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements AbstractContainerScreenAccessor {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;

    @Override
    public int inventorymanagement$getLeftPos() {
        return this.leftPos;
    }

    @Override
    public int inventorymanagement$getTopPos() {
        return this.topPos;
    }

    @Override
    public int inventorymanagement$getImageWidth() {
        return this.imageWidth;
    }
}