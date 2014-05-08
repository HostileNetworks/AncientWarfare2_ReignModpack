package net.shadowmage.ancientwarfare.core.util;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.oredict.OreDictionary;
import net.shadowmage.ancientwarfare.core.config.AWLog;

public class InventoryTools
{

/**
 * Attempt to merge stack into inventory via the given side, or general all-sides merge if side <0<br>
 * Resorts to default general merge if inventory is not a sided inventory.<br>
 * Double-pass merging.  First pass attempts to merge with partial stacks.  Second pass will place
 * into empty slots if available.
 * @param inventory the inventory to merge into, must not be null
 * @param stack the stack to merge, must not be null
 * @param side or <0 for none
 * @return any remaining un-merged item, or null if completely merged
 */
public static ItemStack mergeItemStack(IInventory inventory, ItemStack stack, int side)
  {
  if(side>0 && inventory instanceof ISidedInventory)
    {    
    int[] slotIndices = ((ISidedInventory)inventory).getAccessibleSlotsFromSide(side);
    if(slotIndices==null){return null;}
    int index;
    int toMove;
    ItemStack slotStack;
    for(int i = 0; i <slotIndices.length; i++)
      {
      toMove = stack.stackSize;
      index = slotIndices[i];
      slotStack = inventory.getStackInSlot(index);
      if(doItemStacksMatch(stack, slotStack))
        {
        if(toMove > slotStack.getMaxStackSize() - slotStack.stackSize)
          {
          toMove = slotStack.getMaxStackSize() - slotStack.stackSize;
          }
        stack.stackSize-=toMove;
        slotStack.stackSize+=toMove;
        inventory.setInventorySlotContents(index, slotStack);
        inventory.markDirty();
        }      
      if(stack.stackSize<=0)//merged stack fully;
        {
        return null;
        }
      }
    if(stack.stackSize>0)
      {
      for(int i = 0; i <slotIndices.length; i++)
        {        
        index = slotIndices[i];
        slotStack = inventory.getStackInSlot(index);
        if(slotStack==null)
          {
          inventory.setInventorySlotContents(index, stack);
          inventory.markDirty();
          return null;//successful merge
          }
        }
      }
    else
      {
      return null;//successful merge
      }
    }
  else
    {
    int index;
    int toMove;
    ItemStack slotStack;
    for(int i = 0; i <inventory.getSizeInventory(); i++)
      {
      toMove = stack.stackSize;
      index = i;
      slotStack = inventory.getStackInSlot(index);
      if(doItemStacksMatch(stack, slotStack))
        {
        if(toMove > slotStack.getMaxStackSize() - slotStack.stackSize)
          {
          toMove = slotStack.getMaxStackSize() - slotStack.stackSize;
          }
        stack.stackSize-=toMove;
        slotStack.stackSize+=toMove;
        inventory.setInventorySlotContents(index, slotStack);
        inventory.markDirty();
        }      
      if(stack.stackSize<=0)//merged stack fully;
        {
        return null;
        }
      }
    if(stack.stackSize>0)
      {
      for(int i = 0; i <inventory.getSizeInventory(); i++)
        {        
        index = i;
        slotStack = inventory.getStackInSlot(index);
        if(slotStack==null)
          {
          inventory.setInventorySlotContents(index, stack);
          inventory.markDirty();
          return null;//successful merge
          }
        }
      }
    else
      {
      return null;//successful merge
      }
    }
  return stack;//partial or unsuccessful merge
  }

/**
 * Attempts to remove filter * quantity from inventory.  Returns removed item in return stack, or null if
 * no items were removed.<br>
 * Will only remove and return up to filter.getMaxStackSize() items, regardless of how many are requested.
 * @param inventory
 * @param side
 * @param toRemove
 * @return the removed item.
 */
public static ItemStack removeItems(IInventory inventory, int side, ItemStack filter, int quantity)
  {  
  if(quantity>filter.getMaxStackSize())
    {
    quantity = filter.getMaxStackSize();
    }
  ItemStack returnStack = null;
  if(side>0 && inventory instanceof ISidedInventory)
    {
    int[] slotIndices = ((ISidedInventory)inventory).getAccessibleSlotsFromSide(side);
    if(slotIndices==null){return null;}
    int index;
    int toMove;
    ItemStack slotStack;
    for(int i = 0; i <slotIndices.length; i++)
      {  
      
      index = slotIndices[i];
      slotStack = inventory.getStackInSlot(index);
      if(!doItemStacksMatch(slotStack, filter)){continue;}
      if(returnStack==null)
        {
        returnStack = new ItemStack(filter.getItem());
        returnStack.stackSize = 0;
        }
      toMove = slotStack.stackSize;
      if(toMove>quantity){toMove = quantity;}
      if(toMove + returnStack.stackSize> returnStack.getMaxStackSize()){toMove = returnStack.getMaxStackSize() - returnStack.stackSize;}
      
      returnStack.stackSize+=toMove;
      slotStack.stackSize-=toMove;
      quantity-=toMove;
      if(slotStack.stackSize<=0)
        {
        inventory.setInventorySlotContents(index, null);        
        }
      inventory.markDirty();
      if(quantity<=0)
        {
        break;
        }
      }
    }
  else
    {
    int index;
    int toMove;
    ItemStack slotStack;
    for(int i = 0; i <inventory.getSizeInventory(); i++)
      {        
      index = i;
      slotStack = inventory.getStackInSlot(index);
      if(!doItemStacksMatch(slotStack, filter)){continue;}
      if(returnStack==null)
        {
        returnStack = new ItemStack(filter.getItem());
        returnStack.stackSize = 0;
        }
      toMove = slotStack.stackSize;
      if(toMove>quantity){toMove = quantity;}
      if(toMove + returnStack.stackSize> returnStack.getMaxStackSize()){toMove = returnStack.getMaxStackSize() - returnStack.stackSize;}
      
      returnStack.stackSize+=toMove;
      slotStack.stackSize-=toMove;
      quantity-=toMove;
      if(slotStack.stackSize<=0)
        {
        inventory.setInventorySlotContents(index, null);        
        }
      inventory.markDirty();
      if(quantity<=0)
        {
        break;
        }
      }
    }  
  return returnStack;
  }

/**
 * return a count of how many slots in an inventory contain a certain item stack (any size)
 * @param inv
 * @param side
 * @param filter
 * @return
 */
public static int getNumOfSlotsContaining(IInventory inv, int side, ItemStack filter)
  {
  if(inv.getSizeInventory()<=0){return 0;}
  int count = 0;
  if(side>0 && inv instanceof ISidedInventory)
    {
    int[] slotIndices = ((ISidedInventory) inv).getAccessibleSlotsFromSide(side);
    if(slotIndices==null || slotIndices.length==0){return 0;}
    ItemStack stack;
    for(int i = 0; i < slotIndices.length; i++)
      {
      stack = inv.getStackInSlot(slotIndices[i]);
      if(stack==null){continue;}
      else if(doItemStacksMatch(filter, stack))
        {
        count++;
        }
      }
    }
  else
    {
    ItemStack stack;
    for(int i = 0; i < inv.getSizeInventory(); i++)
      {
      stack = inv.getStackInSlot(i);
      if(stack==null){continue;}
      else if(doItemStacksMatch(filter, stack))
        {
        count ++;
        }
      }
    }
  return count;
  }

/**
 * return the found count of the input item stack (checks item/meta/tag, ignores qty)<br>
 * if inv is not a sided inventory, or input side < 0, counts from entire inventory<br>
 * otherwise only returns the item count from the input side
 * @param inv
 * @param side
 * @param filter
 * @return
 */
public static int getCountOf(IInventory inv, int side, ItemStack filter)
  {
  if(inv.getSizeInventory()<=0){return 0;}
  int count = 0;
  if(side>0 && inv instanceof ISidedInventory)
    {
    int[] slotIndices = ((ISidedInventory) inv).getAccessibleSlotsFromSide(side);
    if(slotIndices==null || slotIndices.length==0){return 0;}
    ItemStack stack;
    for(int i = 0; i < slotIndices.length; i++)
      {
      stack = inv.getStackInSlot(slotIndices[i]);
      if(stack==null){continue;}
      else if(doItemStacksMatch(filter, stack))
        {
        count += stack.stackSize;
        }
      }
    }
  else
    {
    ItemStack stack;
    for(int i = 0; i < inv.getSizeInventory(); i++)
      {
      stack = inv.getStackInSlot(i);
      if(stack==null){continue;}
      else if(doItemStacksMatch(filter, stack))
        {
        count += stack.stackSize;
        }
      }
    }
  return count;
  }

/**
 * validates that stacks are the same item / damage / tag, ignores quantity
 * @param stack1
 * @param stack2
 * @return
 */
public static boolean doItemStacksMatch(ItemStack stack1, ItemStack stack2)
  {
  if(stack1==null){return stack2==null;}
  if(stack2==null){return stack1==null;}
  if(stack1.getItem()==stack2.getItem() && stack1.getItemDamage()==stack2.getItemDamage() && ItemStack.areItemStackTagsEqual(stack1, stack2))
    {
    return true;
    }
  return false;
  }

/**
 * 
 * @param stack1
 * @param stack2
 * @param matchDamage
 * @param matchNBT
 * @param useOreDictionary -- NOTE: this setting overrides damage/nbt match if set to true, and uses oredict id comparison
 * @return
 */
public static boolean doItemStacksMatch(ItemStack stack1, ItemStack stack2, boolean matchDamage, boolean matchNBT, boolean useOreDictionary)
  {
  if(stack1==null){return stack2==null;}
  if(stack2==null){return stack1==null;}
  if(matchDamage && matchNBT && !useOreDictionary)
    {
    return doItemStacksMatch(stack1, stack2);
    }
  if(stack1.getItem()==stack2.getItem())
    {
    if(useOreDictionary)
      {
      int id = OreDictionary.getOreID(stack1);
      int id2 = OreDictionary.getOreID(stack2);
      return id>0 && id2>0 && id==id2;
      }
    if(matchDamage && stack1.getItemDamage()!=stack2.getItemDamage())
      {
      return false;
      }
    if(matchNBT && !ItemStack.areItemStackTagsEqual(stack1, stack2))
      {
      return false;
      }
    return true;
    }
  return false;
  }

/**
 * drops the input itemstack into the world at the input position;
 * @param world
 * @param item
 * @param x
 * @param y
 * @param z
 */
public static void dropItemInWorld(World world, ItemStack item, double x, double y, double z)
  {
  if(item==null || world==null || world.isRemote)
    {
    return;
    }
  EntityItem entityToSpawn;
  x += world.rand.nextFloat() * 0.6f - 0.3f;
  y += world.rand.nextFloat() * 0.6f + 1 - 0.3f;
  z += world.rand.nextFloat() * 0.6f - 0.3f;
  entityToSpawn = new EntityItem(world, x, y, z, item);
  entityToSpawn.setPosition(x, y, z);
  world.spawnEntityInWorld(entityToSpawn);      
  }

public static void dropInventoryInWorld(World world, IInventory localInventory, double x, double y, double z)
  {
  if(world.isRemote)
    {
    return;
    }
  if (localInventory != null)
    {
    ItemStack stack;
    for(int i = 0; i < localInventory.getSizeInventory(); i++)
      {      
      stack = localInventory.getStackInSlotOnClosing(i);      
      if(stack==null)
        {
        continue;
        }
      dropItemInWorld(world, stack, x, y, z);      
      }
    }
  }

/**
 * Writes out the input inventory to the input nbt-tag.<br>
 * The written out inventory is suitable for reading back using
 * {@link #InventoryTools.readInventoryFromNBT(IInventory, NBTTagCompound)}
 * @param inventory
 * @param tag
 */
public static void writeInventoryToNBT(IInventory inventory, NBTTagCompound tag)
  {
  NBTTagList itemList = new NBTTagList();
  NBTTagCompound itemTag;  
  ItemStack item;
  for(int i = 0; i < inventory.getSizeInventory(); i++)
    {
    item = inventory.getStackInSlot(i);
    if(item==null){continue;}
    itemTag = new NBTTagCompound();
    item.writeToNBT(itemTag);
    itemTag.setShort("slot", (short)i);
    itemList.appendTag(itemTag);
    }  
  tag.setTag("itemList", itemList);
  }

/**
 * Reads an inventory contents into the input inventory from the given nbt-tag.<br>
 * Should only be passed nbt-tags / inventories that have been saved using
 *  {@link #InventoryTools.writeInventoryToNBT(IInventory, NBTTagCompound)} 
 * @param inventory
 * @param tag
 */
public static void readInventoryFromNBT(IInventory inventory, NBTTagCompound tag)
  {
  NBTTagList itemList = tag.getTagList("itemList", Constants.NBT.TAG_COMPOUND);  
  NBTTagCompound itemTag;  
  ItemStack item;
  int slot;
  for(int i = 0; i < itemList.tagCount(); i++)
    {
    itemTag = itemList.getCompoundTagAt(i);
    slot = itemTag.getShort("slot");
    item = ItemStack.loadItemStackFromNBT(itemTag);
    inventory.setInventorySlotContents(slot, item);
    }
  }

}
