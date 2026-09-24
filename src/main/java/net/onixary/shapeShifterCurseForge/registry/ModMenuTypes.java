package net.onixary.shapeShifterCurseForge.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.block.entity.AltarBlockEntity;
import net.onixary.shapeShifterCurseForge.other.menu.AltarMenu;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ShapeShifterCurseForge.RESOURCE_NAMESPACE);
    public static final RegistryObject<MenuType<AltarMenu>> ALTAR = MENUS.register("altar", () -> IForgeMenuType.create((id, inv, buf) -> {
        var pos = buf.readBlockPos();
        var level = inv.player.level();
        var be = level.getBlockEntity(pos);
        if (be instanceof AltarBlockEntity altar) {
            return new AltarMenu(id, inv, altar, altar.dataAccess);
        }
        // fallback dummy
        return new AltarMenu(id, inv, new net.minecraft.world.SimpleContainer(11), new net.minecraft.world.inventory.SimpleContainerData(3));
    }));
    private ModMenuTypes(){}
}
