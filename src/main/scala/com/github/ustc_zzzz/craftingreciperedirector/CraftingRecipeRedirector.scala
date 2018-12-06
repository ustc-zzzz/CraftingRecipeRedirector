package com.github.ustc_zzzz.craftingreciperedirector

import net.minecraft.launchwrapper.IClassTransformer
import net.minecraftforge.fml.common.ModMetadata
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import org.objectweb.asm._
import org.apache.logging.log4j.{LogManager, Logger}

/**
  * @author ustc_zzzz
  */
object CraftingRecipeRedirector {

  class ReturnBooleanVisitor(parent: MethodVisitor, name: String) extends MethodVisitor(Opcodes.ASM5, parent) {
    logger.debug(s"Inject crafting recipe redirector into $name")

    override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = super.visitMaxs(maxStack + 2, maxLocals)

    override def visitInsn(opcode: Int): Unit = if (opcode != Opcodes.IRETURN) super.visitInsn(opcode) else {
      val owner = "com/github/ustc_zzzz/craftingreciperedirector/CraftingRecipeRedirectorModContainer"
      val desc = "(ZLjava/lang/Object;)Z"

      super.visitVarInsn(Opcodes.ALOAD, 0)
      super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "transformBoolean", desc, false)
      super.visitInsn(opcode)
    }
  }

  class ReturnItemStackVisitor(parent: MethodVisitor, name: String) extends MethodVisitor(Opcodes.ASM5, parent) {
    logger.debug(s"Inject crafting recipe redirector into $name")

    override def visitMaxs(maxStack: Int, maxLocals: Int): Unit = super.visitMaxs(maxStack + 2, maxLocals)

    override def visitInsn(opcode: Int): Unit = if (opcode != Opcodes.ARETURN) super.visitInsn(opcode) else {
      val owner = "com/github/ustc_zzzz/craftingreciperedirector/CraftingRecipeRedirectorModContainer"
      val desc = "(Lnet/minecraft/item/ItemStack;Ljava/lang/Object;)Lnet/minecraft/item/ItemStack;"

      super.visitVarInsn(Opcodes.ALOAD, 0)
      super.visitMethodInsn(Opcodes.INVOKESTATIC, owner, "transformItemStack", desc, false)
      super.visitInsn(opcode)
    }
  }

  class Visitor(parent: ClassVisitor, name: String) extends ClassVisitor(Opcodes.ASM5, parent) {
    override def visitMethod(a: Int, n: String, d: String, s: String, e: Array[String]): MethodVisitor = n + d match {
      case `srgResult` | `srgOutput` => new ReturnItemStackVisitor(super.visitMethod(a, n, d, s, e), name)
      case `srgMatches` => new ReturnBooleanVisitor(super.visitMethod(a, n, d, s, e), name)
      case _ => super.visitMethod(a, n, d, s, e)
    }

    final val srgMatches = "func_77569_a(Lnet/minecraft/inventory/InventoryCrafting;Lnet/minecraft/world/World;)Z"
    final val srgResult = "func_77572_b(Lnet/minecraft/inventory/InventoryCrafting;)Lnet/minecraft/item/ItemStack;"
    final val srgOutput = "func_77571_b()Lnet/minecraft/item/ItemStack;"
  }

  class Transformer extends IClassTransformer {
    override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
      val classReader = new ClassReader(basicClass)
      val classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_FRAMES)

      classReader.accept(new Visitor(classWriter, transformedName), ClassReader.EXPAND_FRAMES)
      classWriter.toByteArray
    }
  }

  def createMetadata(): ModMetadata = {
    val metadata = new ModMetadata

    metadata.authorList = java.util.Arrays.asList("ustc-zzzz")
    metadata.modId = "crafting" + "recipe" + "redirector"
    metadata.description = "CraftingRecipeRedirector"
    metadata.name = "CraftingRecipeRedirector"
    metadata.version = "0.1.0"

    metadata
  }

  private lazy val logger: Logger = {
    val logger = LogManager.getLogger("CraftingRecipeRedirector")
    logger.info("Injection started for crafting recipes")
    logger
  }
}

@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("CraftingRecipeRedirector")
class CraftingRecipeRedirector extends IFMLLoadingPlugin {
  override def getASMTransformerClass: Array[String] = Array.empty[String]

  override def injectData(data: java.util.Map[String, AnyRef]): Unit = ()

  override def getAccessTransformerClass: String = transformerName

  override def getModContainerClass: String = modContainerName

  override def getSetupClass: String = null

  private final val projectGroup = "com.github.ustc_zzzz.craftingreciperedirector"
  private final val transformerName = projectGroup + ".CraftingRecipeRedirector$Transformer"
  private final val modContainerName = projectGroup + ".CraftingRecipeRedirectorModContainer"
}
