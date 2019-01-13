package com.github.ustc_zzzz.craftingreciperedirector

import java.util.Optional

import com.github.ustc_zzzz.craftingreciperedirector.api.CraftingResultRedirectionEvent
import com.google.common.eventbus.{EventBus, Subscribe}
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.common.event.{FMLConstructionEvent, FMLModIdMappingEvent}
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.common.{DummyModContainer, LoadController}
import net.minecraftforge.registries.IForgeRegistry
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
  def transformSrgOutput(itemStack: ItemStack, cls: String, obj: AnyRef): ItemStack = obj match {
    case iRecipe: IRecipe if apply(iRecipe, cls, srgOutputMap) => apply(iRecipe, itemStack)
    case _ => itemStack
  }

  def transformSrgResult(itemStack: ItemStack, cls: String, obj: AnyRef): ItemStack = obj match {
    case iRecipe: IRecipe if apply(iRecipe, cls, srgResultMap) => apply(iRecipe, itemStack)
    case _ => itemStack
  }

  def transformSrgMatches(boolean: Boolean, cls: String, obj: AnyRef): Boolean = obj match {
    case iRecipe: IRecipe if apply(iRecipe, cls, srgMatchesMap) => boolean && !iRecipe.getRecipeOutput.isEmpty
    case _ => boolean
  }

  def apply(recipe: IRecipe, cls: String, map: Map[ResourceLocation, String]): Boolean = {
    cls == map.get(recipe.getRegistryName).orNull
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

  private final var srgOutputMap = Map.empty[ResourceLocation, String]
  private final var srgResultMap = Map.empty[ResourceLocation, String]
  private final var srgMatchesMap = Map.empty[ResourceLocation, String]
}

class CraftingRecipeRedirectorModContainer extends DummyModContainer(CraftingRecipeRedirector.createMetadata()) {
  override def registerBus(bus: EventBus, controller: LoadController): Boolean = {
    bus.register(this)
    true
  }

  @Subscribe
  def on(event: FMLConstructionEvent): Unit = CraftingRecipeRedirectorModContainer.beforeConstruction = false

  @Subscribe
  def on(event: FMLModIdMappingEvent): Unit = {
    import CraftingRecipeRedirectorModContainer._

    val registry = GameRegistry.findRegistry(classOf[IRecipe])

    def toMap(registry: IForgeRegistry[IRecipe], methodName: String) = {
      import scala.collection.JavaConverters._
      val builder = Map.newBuilder[ResourceLocation, String]
      for {
        entry <- registry.getEntries.asScala
        method <- entry.getValue.getClass.getMethods
        if methodName == method.getName + org.objectweb.asm.Type.getMethodDescriptor(method)
      } {
        val key = entry.getKey
        builder += key -> method.getDeclaringClass.getName
        CraftingRecipeRedirector.logger.debug(s"Enable crafting recipe redirector for $key")
      }
      builder.result()
    }

    srgOutputMap = toMap(registry, CraftingRecipeRedirector.srgOutput)
    srgResultMap = toMap(registry, CraftingRecipeRedirector.srgResult)
    srgMatchesMap = toMap(registry, CraftingRecipeRedirector.srgMatches)

    val methods = srgOutputMap.size + srgResultMap.size + srgMatchesMap.size

    CraftingRecipeRedirector.logger.info(s"Enable crafting recipe redirector for $methods methods")
  }
}
