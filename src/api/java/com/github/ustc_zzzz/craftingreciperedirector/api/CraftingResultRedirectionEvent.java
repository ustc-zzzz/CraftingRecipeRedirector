package com.github.ustc_zzzz.craftingreciperedirector.api;

import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.event.item.inventory.AffectItemStackEvent;
import org.spongepowered.api.item.inventory.ItemStackSnapshot;
import org.spongepowered.api.item.inventory.crafting.CraftingGridInventory;
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.api.world.World;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Fired when redirection of crafting result is occurred.
 *
 * @author ustc_zzzz
 */
@NonnullByDefault
public interface CraftingResultRedirectionEvent extends AffectItemStackEvent
{
    /**
     * Returns the corresponding recipe used for crafting.
     *
     * Invoking {@link CraftingRecipe#isValid(CraftingGridInventory, World)} or
     * {@link CraftingRecipe#getResult(CraftingGridInventory)} will fire another
     * of type {@link CraftingResultRedirectionEvent}, which may cause infinite
     * recursion.
     *
     * @return the corresponding recipe
     */
    Optional<CraftingRecipe> getRecipe();

    /**
     * Returns the transaction used to redirect the crafting result.
     *
     * The result item stack will not be redirected if it is marked invalid or
     * the event is cancelled. The corresponding recipe used for crafting will
     * be skipped if the final result is set to {@link ItemStackSnapshot#NONE}.
     *
     * @return the transaction
     */
    Transaction<ItemStackSnapshot> getTransaction();

    @Override
    default List<? extends Transaction<ItemStackSnapshot>> getTransactions()
    {
        return Collections.singletonList(this.getTransaction());
    }
}
