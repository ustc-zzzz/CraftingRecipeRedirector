package com.github.ustc_zzzz.craftingreciperedirector

import java.util.Optional

import com.github.ustc_zzzz.craftingreciperedirector.api.CraftingResultRedirectionEvent
import com.google.common.eventbus.{EventBus, Subscribe}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.fml.common.event.FMLConstructionEvent
import net.minecraftforge.fml.common.{DummyModContainer, LoadController}
import org.spongepowered.api.Sponge
import org.spongepowered.api.data.Transaction
import org.spongepowered.api.event.cause.Cause
import org.spongepowered.api.item.inventory.ItemStackSnapshot
import org.spongepowered.api.item.recipe.crafting.CraftingRecipe
import org.spongepowered.common.item.inventory.util.ItemStackUtil

/**
  * @author ustc_zzzz
  */
object CraftingRecipeRedirectorModContainer {
  def transformItemStack(itemStack: ItemStack, obj: AnyRef): ItemStack = obj match {
    case iRecipe: IRecipe => apply(iRecipe, itemStack)
    case _ => itemStack
  }

  def transformBoolean(boolean: Boolean, obj: AnyRef): Boolean = obj match {
    case iRecipe: IRecipe => boolean && !apply(iRecipe, iRecipe.getRecipeOutput).isEmpty
    case _ => boolean
  }

  def apply(recipe: IRecipe, itemStack: ItemStack): ItemStack = if (beforeConstruction) itemStack else {
    val snapshot = ItemStackUtil.snapshotOf(itemStack)
    val cause = Sponge.getCauseStackManager.getCurrentCause
    val transaction = new Transaction[ItemStackSnapshot](snapshot, snapshot)
    val cancelled = Sponge.getEventManager post new CraftingResultRedirectionEvent {
      override def getRecipe: Optional[CraftingRecipe] = recipe match {
        case craftingRecipe: CraftingRecipe => Optional.of(craftingRecipe)
        case _ => Optional.empty()
      }

      override def getTransaction: Transaction[ItemStackSnapshot] = transaction

      override def setCancelled(boolean: Boolean): Unit = cancel = boolean

      override def isCancelled: Boolean = cancel

      override def getCause: Cause = cause

      final var cancel: Boolean = false
    }

    if (cancelled || !transaction.isValid) itemStack else ItemStackUtil.fromSnapshotToNative(transaction.getFinal)
  }

  private final var beforeConstruction = true
}

class CraftingRecipeRedirectorModContainer extends DummyModContainer(CraftingRecipeRedirector.createMetadata()) {
  override def registerBus(bus: EventBus, controller: LoadController): Boolean = {
    bus.register(this)
    true
  }

  @Subscribe
  def on(event: FMLConstructionEvent): Unit = CraftingRecipeRedirectorModContainer.beforeConstruction = false
}
