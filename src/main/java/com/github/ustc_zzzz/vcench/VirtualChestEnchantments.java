package com.github.ustc_zzzz.vcench;

import com.github.ustc_zzzz.virtualchest.api.action.VirtualChestActionExecutor.Context;
import com.github.ustc_zzzz.virtualchest.api.action.VirtualChestActionExecutor.HandheldItemContext;
import com.github.ustc_zzzz.virtualchest.api.action.VirtualChestActionExecutor.LoadEvent;
import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.Iterators;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.data.manipulator.immutable.item.ImmutableEnchantmentData;
import org.spongepowered.api.data.manipulator.mutable.item.EnchantmentData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.item.enchantment.Enchantment;
import org.spongepowered.api.item.enchantment.EnchantmentType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.Slot;
import org.spongepowered.api.item.inventory.entity.PlayerInventory;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.util.Coerce;
import org.spongepowered.api.util.annotation.NonnullByDefault;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * @author ustc_zzzz
 */
@NonnullByDefault
@Plugin(id = "vcench", name = "VirtualChestEnchantments", version = "1.0.0", description = "VirtualChestEnchantments")
public class VirtualChestEnchantments
{
    private final Function<PlayerInventory, ItemStackSnapshot> getCursorItem;
    private final BiConsumer<PlayerInventory, ItemStackSnapshot> setCursorItem;

    public VirtualChestEnchantments()
    {
        try
        {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            this.getCursorItem = this.getGetCursorItem(lookup);
            this.setCursorItem = this.getSetCursorItem(lookup);
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    @Listener
    public void on(LoadEvent event)
    {
        event.register("enchant", this::execute);
    }

    private Optional<Enchantment> getEnchantment(String s)
    {
        try
        {
            int colonFirst = s.indexOf(':'), colonIndex = s.lastIndexOf(':');
            String id = colonFirst == colonIndex ? s : s.substring(0, colonIndex);
            int level = colonFirst == colonIndex ? 1 : Coerce.toInteger(s.substring(colonIndex + 1));
            return Sponge.getRegistry().getType(EnchantmentType.class, id).map(type -> Enchantment.of(type, level));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }

    private CompletableFuture<CommandResult> execute(CommandResult result, String value, ClassToInstanceMap<Context> context)
    {
        Enchantment e = this.getEnchantment(value).orElseThrow(IllegalArgumentException::new);
        Optional<Player> playerOptional = context.getInstance(Context.PLAYER).getPlayer();
        HandheldItemContext handheldItem = context.getInstance(Context.HANDHELD_ITEM);
        if (Objects.nonNull(handheldItem) && playerOptional.isPresent())
        {
            PlayerInventory i = (PlayerInventory) playerOptional.get().getInventory();
            ItemStackSnapshot snapshot = this.getCursorItem.apply(i);
            if (handheldItem.matchItem(snapshot))
            {
                EnchantmentData data = snapshot.getOrCreate(ImmutableEnchantmentData.class).get().asMutable();
                this.setCursorItem.accept(i, snapshot.with(data.addElement(e).asImmutable()).get());
                return CompletableFuture.completedFuture(CommandResult.success());
            }
            for (Slot slot : i.<Slot>slots())
            {
                snapshot = slot.peek().orElse(ItemStack.empty()).createSnapshot();
                if (handheldItem.matchItem(snapshot))
                {
                    EnchantmentData data = snapshot.getOrCreate(ImmutableEnchantmentData.class).get().asMutable();
                    slot.set(snapshot.with(data.addElement(e).asImmutable()).get().createStack());
                    return CompletableFuture.completedFuture(CommandResult.success());
                }
            }
        }
        return CompletableFuture.completedFuture(CommandResult.success());
    }

    private Function<PlayerInventory, ItemStackSnapshot> getGetCursorItem(MethodHandles.Lookup lookup) throws ReflectiveOperationException
    {
        MethodHandle getItemStack = lookup.unreflect(Iterators.getOnlyElement(Arrays
                .stream(Class.forName("net.minecraft.entity.player.InventoryPlayer").getDeclaredMethods())
                .filter(m -> "func_70445_o".equals(m.getName())).iterator()));

        MethodHandle snapshotOf = lookup.unreflect(Iterators.getOnlyElement(Arrays
                .stream(Class.forName("org.spongepowered.common.item.inventory.util.ItemStackUtil").getDeclaredMethods())
                .filter(m -> Arrays.toString(m.getParameterTypes()).equals("[class net.minecraft.item.ItemStack]"))
                .filter(m -> "snapshotOf".equals(m.getName())).iterator()));

        MethodHandle getCursorItem = MethodHandles
                .filterReturnValue(getItemStack, snapshotOf)
                .asType(MethodType.methodType(ItemStackSnapshot.class, PlayerInventory.class));

        return inventory ->
        {
            try
            {
                return (ItemStackSnapshot) getCursorItem.invokeExact(inventory);
            }
            catch (Throwable e)
            {
                throw new IllegalArgumentException(e);
            }
        };
    }

    private BiConsumer<PlayerInventory, ItemStackSnapshot> getSetCursorItem(MethodHandles.Lookup lookup) throws ReflectiveOperationException
    {
        MethodHandle setItemStack = lookup.unreflect(Iterators.getOnlyElement(Arrays
                .stream(Class.forName("net.minecraft.entity.player.InventoryPlayer").getDeclaredMethods())
                .filter(m -> "func_70437_b".equals(m.getName())).iterator()));

        MethodHandle updateHeldItem = lookup.unreflect(Iterators.getOnlyElement(Arrays
                .stream(Class.forName("net.minecraft.entity.player.EntityPlayerMP").getDeclaredMethods())
                .filter(m -> "func_71113_k".equals(m.getName())).iterator()))
                .asType(MethodType.methodType(void.class, Player.class));

        MethodHandle fromSnapshotToNative = lookup.unreflect(Iterators.getOnlyElement(Arrays
                .stream(Class.forName("org.spongepowered.common.item.inventory.util.ItemStackUtil").getDeclaredMethods())
                .filter(m -> m.getReturnType().toString().equals("class net.minecraft.item.ItemStack"))
                .filter(m -> "fromSnapshotToNative".equals(m.getName())).iterator()));

        MethodHandle setCursorItem = MethodHandles
                .filterArguments(setItemStack, 1, fromSnapshotToNative)
                .asType(MethodType.methodType(void.class, PlayerInventory.class, ItemStackSnapshot.class));

        return (inventory, snapshot) ->
        {
            try
            {
                setCursorItem.invokeExact(inventory, snapshot);
                updateHeldItem.invokeExact(inventory.getCarrier().get());
            }
            catch (Throwable e)
            {
                throw new IllegalArgumentException(e);
            }
        };
    }
}
