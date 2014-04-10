package net.shadowmage.ancientwarfare.structure.tile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.sun.org.apache.xml.internal.resolver.readers.XCatalogReader;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.shadowmage.ancientwarfare.core.config.AWLog;
import net.shadowmage.ancientwarfare.structure.config.AWStructureStatics;

public class SpawnerSettings
{

List<EntitySpawnGroup> spawnGroups = new ArrayList<EntitySpawnGroup>();

boolean lightSensitive;//TODO add to read/write/spawn mechanics
boolean respondToRedstone;//should this spawner respond to redstone impulses
boolean redstoneMode;//false==toggle, true==pulse/tick to spawn
boolean prevRedstoneState;//used to cache the powered status from last tick, to compare to this tick

int playerRange;

int maxDelay = 20*20;
int minDelay = 20*10;

int spawnDelay = maxDelay;

int maxNearbyMonsters;//TODO use this??

/**
 * fields for a 'fake' tile-entity...set from the real tile-entity when it has its 
 * world set (which is before first updateEntity() is called)
 */
public World worldObj;
int xCoord;
int yCoord;
int zCoord;

public SpawnerSettings()
  {
    
  }

public void setWorld(World world, int x, int y, int z)
  {
  this.worldObj = world;
  this.xCoord = x;
  this.yCoord = y;
  this.zCoord = z;
  }

public void onUpdate()
  {
  if(!respondToRedstone)
    {
    updateNormalMode();
    }
  else if(redstoneMode)
    {
    updateRedstoneModePulse();
    }
  else
    {
    updateRedstoneModeToggle();
    }
  if(spawnGroups.isEmpty())
    {
    worldObj.setBlock(xCoord, yCoord, zCoord, Blocks.air);
    }
  }

private void updateRedstoneModeToggle()
  {
  boolean powered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) || worldObj.getBlockPowerInput(xCoord, yCoord, zCoord)>0;
  prevRedstoneState = powered; 
  if(respondToRedstone && !redstoneMode && !prevRedstoneState)
    {
    //noop
    return;
    }
  if(spawnDelay>0)
    {
    spawnDelay--;
    }
  if(spawnDelay<=0)
    {
    spawnDelay = minDelay + worldObj.rand.nextInt(maxDelay-minDelay);
    spawnEntities();
    }
  }

private void updateRedstoneModePulse()
  {
  boolean powered = worldObj.isBlockIndirectlyGettingPowered(xCoord, yCoord, zCoord) || worldObj.getBlockPowerInput(xCoord, yCoord, zCoord)>0;   
  if(!prevRedstoneState && powered)
    {
    spawnEntities();
    }
  prevRedstoneState = powered;
  }

private void updateNormalMode()
  {
  if(spawnDelay>0)
    {
    spawnDelay--;
    }
  if(spawnDelay<=0)
    {
    int range = maxDelay-minDelay;
    spawnDelay = minDelay + range<=0 ? 0 : worldObj.rand.nextInt(range);
    spawnEntities();
    }
  }

private void spawnEntities()
  {
  
  if(playerRange>0)
    {
    EntityPlayer p = worldObj.getClosestPlayer(xCoord+0.5d, yCoord, zCoord+0.5d, playerRange);
    if(p==null){return;}
    }
  
  if(maxNearbyMonsters>0)
    {
    List<Entity> nearbyEntities = worldObj.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getAABBPool().getAABB(xCoord-4, yCoord-4, zCoord-4, xCoord+5, yCoord+5, zCoord+5));
    int nearbyCount = 0;
    for(Entity e : nearbyEntities)
      {
      if(e.getClass()==EntityItem.class){continue;}
      nearbyCount++;
      }
    if(nearbyCount>=maxNearbyMonsters)
      {
      AWLog.logDebug("skipping spawning because of too many nearby entities");
      return;
      }
    }
  
  int totalWeight = 0;
  for(EntitySpawnGroup group : this.spawnGroups)//count total weights
    {
    totalWeight+=group.groupWeight;
    }  
  int rand = totalWeight == 0 ? 0 : worldObj.rand.nextInt(totalWeight);//select an object
  int check = 0;
  EntitySpawnGroup toSpawn = null;
  for(EntitySpawnGroup group : this.spawnGroups)//iterate to find selected object
    {
    check+=group.groupWeight;
    if(rand<check)//object found, break
      {
      toSpawn = group;
      break;
      }
    }
  
  if(toSpawn!=null)
    {
    toSpawn.spawnEntities(worldObj, xCoord, yCoord, zCoord);
    if(toSpawn.shouldRemove())
      {
      spawnGroups.remove(toSpawn);
      }
    }
  

  }

public void writeToNBT(NBTTagCompound tag)
  {
  tag.setBoolean("respondToRedstone", respondToRedstone);
  if(respondToRedstone)
    {
    tag.setBoolean("redstoneMode", redstoneMode);   
    tag.setBoolean("prevRedstoneState", prevRedstoneState);
    }
  tag.setInteger("minDelay", minDelay);
  tag.setInteger("maxDelay", maxDelay);
  tag.setInteger("spawnDelay", spawnDelay);
  tag.setInteger("playerRange", playerRange);
  tag.setInteger("maxNearbyMonsters", maxNearbyMonsters);
  NBTTagList groupList = new NBTTagList();
  NBTTagCompound groupTag;
  for(EntitySpawnGroup group : this.spawnGroups)
    {
    groupTag = new NBTTagCompound();
    group.writeToNBT(groupTag);
    groupList.appendTag(groupTag);
    }
  tag.setTag("spawnGroups", groupList);
  }

public void readFromNBT(NBTTagCompound tag)
  {
  spawnGroups.clear();
  respondToRedstone = tag.getBoolean("respondToRedstone");
  if(respondToRedstone)
    {
    redstoneMode = tag.getBoolean("redstoneMode");
    prevRedstoneState = tag.getBoolean("prevRedstoneState");
    }
  minDelay = tag.getInteger("minDelay");
  maxDelay = tag.getInteger("maxDelay");
  spawnDelay = tag.getInteger("spawnDelay");
  playerRange = tag.getInteger("playerRange");
  maxNearbyMonsters = tag.getInteger("maxNearbyMonsters");
  NBTTagList groupList = tag.getTagList("spawnGroups", Constants.NBT.TAG_COMPOUND);
  EntitySpawnGroup group;
  for(int i = 0; i < groupList.tagCount(); i++)
    {
    group = new EntitySpawnGroup();
    group.readFromNBT(groupList.getCompoundTagAt(i));
    spawnGroups.add(group);
    }
  }

public void addSpawnGroup(EntitySpawnGroup group)
  {
  spawnGroups.add(group);
  }

public List<EntitySpawnGroup> getSpawnGroups()
  {
  return spawnGroups;
  }

public final boolean isLightSensitive()
  {
  return lightSensitive;
  }

public final void setLightSensitive(boolean lightSensitive)
  {
  this.lightSensitive = lightSensitive;
  }

public final boolean isRespondToRedstone()
  {
  return respondToRedstone;
  }

public final void setRespondToRedstone(boolean respondToRedstone)
  {
  this.respondToRedstone = respondToRedstone;
  }

public final boolean getRedstoneMode()
  {
  return redstoneMode;
  }

public final void setRedstoneMode(boolean redstoneMode)
  {
  this.redstoneMode = redstoneMode;
  }

public final int getPlayerRange()
  {
  return playerRange;
  }

public final void setPlayerRange(int playerRange)
  {
  this.playerRange = playerRange;
  }

public final int getMaxDelay()
  {
  return maxDelay;
  }

public final void setMaxDelay(int maxDelay)
  {
  this.maxDelay = maxDelay;
  }

public final int getMinDelay()
  {
  return minDelay;
  }

public final void setMinDelay(int minDelay)
  {
  this.minDelay = minDelay;
  }

public final int getSpawnDelay()
  {
  return spawnDelay;
  }

public final void setSpawnDelay(int spawnDelay)
  {
  this.spawnDelay = spawnDelay;
  }

public final int getMaxNearbyMonsters()
  {
  return maxNearbyMonsters;
  }

public final void setMaxNearbyMonsters(int maxNearbyMonsters)
  {
  this.maxNearbyMonsters = maxNearbyMonsters;
  }



public static final class EntitySpawnGroup
{
private int groupWeight;
List<EntitySpawnSettings> entitiesToSpawn = new ArrayList<EntitySpawnSettings>();

public EntitySpawnGroup()
  {
  
  }

public void setWeight(int weight)
  {
  if(weight<=0){weight = 1;}
  this.groupWeight = weight;  
  }

public void addSpawnSetting(EntitySpawnSettings setting)
  {
  entitiesToSpawn.add(setting);
  }

public void spawnEntities(World world, int x, int y, int z)
  {
  EntitySpawnSettings settings;
  Iterator<EntitySpawnSettings> it = entitiesToSpawn.iterator();
  while(it.hasNext() && (settings = it.next())!=null)
    {
    settings.spawnEntities(world, x, y, z);
    if(settings.shouldRemove())
      {
      it.remove();
      }
    }
  }

public boolean shouldRemove()
  {
  return entitiesToSpawn.isEmpty();
  }

public List<EntitySpawnSettings> getEntitiesToSpawn()
  {
  return entitiesToSpawn;
  }

public int getWeight()
  {
  return groupWeight;
  }

public void writeToNBT(NBTTagCompound tag)
  {
  tag.setInteger("groupWeight", groupWeight);
  NBTTagList settingsList = new NBTTagList();
  
  NBTTagCompound settingTag;
  for(EntitySpawnSettings setting : this.entitiesToSpawn)
    {
    settingTag = new NBTTagCompound();
    setting.writeToNBT(settingTag);
    settingsList.appendTag(settingTag);
    }  
  tag.setTag("settingsList", settingsList);
  }

public void readFromNBT(NBTTagCompound tag)
  {
  groupWeight = tag.getInteger("groupWeight");
  NBTTagList settingsList = tag.getTagList("settingsList", Constants.NBT.TAG_COMPOUND);
  EntitySpawnSettings setting;
  for(int i = 0; i < settingsList.tagCount(); i++)
    {
    setting = new EntitySpawnSettings();
    setting.readFromNBT(settingsList.getCompoundTagAt(i));
    this.entitiesToSpawn.add(setting);
    }
  }
}

public static final class EntitySpawnSettings
{
String entityId = "Pig";
NBTTagCompound customTag;
int minToSpawn = 1;
int maxToSpawn = 4;
int remainingSpawnCount =-1;

public EntitySpawnSettings()
  {
  
  }

public EntitySpawnSettings(String entityId)
  {
  setEntityToSpawn(entityId);
  }

public final void writeToNBT(NBTTagCompound tag)
  {
  tag.setString("entityId", entityId);
  if(customTag!=null){tag.setTag("customTag", customTag);}
  tag.setInteger("minToSpawn", minToSpawn);
  tag.setInteger("maxToSpawn", maxToSpawn);
  tag.setInteger("remainingSpawnCount", remainingSpawnCount);
  }
  
public final void readFromNBT(NBTTagCompound tag)
  {
  setEntityToSpawn(tag.getString("entityId"));
  if(tag.hasKey("customTag")){customTag = tag.getCompoundTag("customTag");}
  minToSpawn = tag.getInteger("minToSpawn");
  maxToSpawn = tag.getInteger("maxToSpawn");
  remainingSpawnCount = tag.getInteger("remainingSpawnCount");
  }

public final void setEntityToSpawn(String entityId)
  {
  this.entityId = entityId;
  Class<? extends Entity> entityClass = (Class<? extends Entity>) EntityList.stringToClassMapping.get(this.entityId);
  if( entityClass==null)
    {
    AWLog.logError(entityId+" is not a valid entityId.  Spawner default to Zombie.");
    this.entityId = "Zombie";
    } 
  if(AWStructureStatics.excludedSpawnerEntities.contains(this.entityId))
    {
    AWLog.logError(entityId+" has been set as an invalid entity for spawners!  Spawner default to Zombie.");
    this.entityId = "Zombie";
    }
  }

public final void setCustomSpawnTag(NBTTagCompound tag)
  {
  this.customTag = tag;
  }

public final void setSpawnCountMin(int min)
  {
  this.minToSpawn = min;
  }

public final void setSpawnCountMax(int max)
  {
  this.maxToSpawn = max;
  }

public final void setSpawnLimitTotal(int total)
  {
  this.remainingSpawnCount = total;
  }

private final boolean shouldRemove()
  {
  return remainingSpawnCount==0;
  }

public final String getEntityId()
  {
  return entityId;
  }

public final int getSpawnMin()
  {
  return minToSpawn;
  }

public final int getSpawnMax()
  {
  return maxToSpawn;
  }

public final int getSpawnTotal()
  {
  return remainingSpawnCount;
  }

public final NBTTagCompound getCustomTag()
  {
  return customTag;
  }

private final int getNumToSpawn(Random rand)
  {  
  int randRange = maxToSpawn - minToSpawn;
  int toSpawn =  0;
  if(randRange<=0)
    {
    toSpawn = minToSpawn;    
    }
  else
    {
    toSpawn = minToSpawn + rand.nextInt(randRange);
    }
  if(remainingSpawnCount>=0 && toSpawn>remainingSpawnCount)
    {
    toSpawn = remainingSpawnCount;
    }
  return toSpawn;
  }

private final void decrementSpawnCounter(int numSpawned)
  {
  if(remainingSpawnCount==-1)
    {
    //noop, unlimited spawns
    }
  else
    {
    remainingSpawnCount-=numSpawned;
    if(remainingSpawnCount<0)
      {
      remainingSpawnCount = 0;
      }
    }
  }

private final void spawnEntities(World world, int xCoord, int yCoord, int zCoord)
  {
  int toSpawn = getNumToSpawn(world.rand);
  decrementSpawnCounter(toSpawn);
  AWLog.logDebug("spawning entities... from:"+this + " of entity type: "+entityId +" count: "+toSpawn);
  
  int x = xCoord, y = yCoord+1, z = zCoord;
  int spawnTry = 0;
  boolean doSpawn;
  for(int i = 0; i < toSpawn; i++)
    {
    doSpawn = false;
    while(true)
      {
      x = xCoord - 4 + world.rand.nextInt(9);
      z = zCoord - 4 + world.rand.nextInt(9);
      for(y = yCoord -5; y <= yCoord+4; y++)
        {
        if(world.isAirBlock(x, y, z) && world.isAirBlock(x, y+1, z))
          {
          doSpawn = true;
          break;
          }
        }   
      
      spawnTry++;  
      if(spawnTry>=10 || doSpawn)
        {
        break;
        }
      }
    /**
     * find a block coordinate to spawn at
     */
    if(doSpawn)
      {
      spawnEntityAt(world, x, y, z);      
      }
    }
  }

private final void spawnEntityAt(World world, int x, int y, int z)
  {
  Entity e = EntityList.createEntityByName(entityId, world);
  if(e!=null)
    {
    if(customTag!=null)
      {
      e.readFromNBT(customTag);
      }
    e.setPosition(x+0.5d, y, z+0.5d);
    world.spawnEntityInWorld(e);
    }
  }

}


public static SpawnerSettings getDefaultSettings()
  {
  SpawnerSettings settings = new SpawnerSettings();
  settings.maxDelay = 20*20;
  settings.minDelay = 10*20;
  settings.playerRange =  16;
  settings.maxNearbyMonsters = 8;
  settings.respondToRedstone = false;
  
  EntitySpawnGroup group = new EntitySpawnGroup();
  group.groupWeight = 1;
  settings.addSpawnGroup(group);
  
  EntitySpawnSettings entity = new EntitySpawnSettings();
  entity.setEntityToSpawn("Pig");
  entity.setSpawnCountMin(2);
  entity.setSpawnCountMax(4);
  entity.remainingSpawnCount = -1;
  group.addSpawnSetting(entity);
  
  return settings;
  }

}
