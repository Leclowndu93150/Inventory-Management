package com.leclowndu93150.inventorymanagement.mixin;

import com.leclowndu93150.inventorymanagement.client.gui.screen.InventorySettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforge.client.gui.widget.ModListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ModListScreen.class)
public class ModListMixin {
    
    @Shadow
    private ModListWidget.ModEntry selected;
    
    @Shadow
    private Button configButton;
    
    @Inject(method = "displayModConfig", at = @At("HEAD"), cancellable = true, remap = false)
    private void openInventoryManagementConfig(CallbackInfo ci) {
        if (this.selected != null && "inventorymanagement".equals(this.selected.getInfo().getModId())) {
            ModListScreen self = (ModListScreen) (Object) this;
            Minecraft.getInstance().setScreen(new InventorySettingsScreen(self));
            ci.cancel();
        }
    }
    
    @Inject(method = "updateCache", at = @At("TAIL"), remap = false)
    private void enableConfigButtonForInventoryManagement(CallbackInfo ci) {
        if (this.selected != null && "inventorymanagement".equals(this.selected.getInfo().getModId())) {
            this.configButton.active = true;
        }
    }
}