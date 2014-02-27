package net.shadowmage.ancientwarfare.structure.network;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.shadowmage.ancientwarfare.core.network.PacketBase;
import net.shadowmage.ancientwarfare.structure.template.StructureTemplateManager;

public class PacketStructure extends PacketBase
{

public NBTTagCompound packetData = new NBTTagCompound();

public PacketStructure()
  {
  // TODO Auto-generated constructor stub
  }

@Override
protected void writeToStream(ByteBuf data)
  {
  if(packetData!=null)
    {
    ByteBufOutputStream bbos = new ByteBufOutputStream(data);
    try
      {
      CompressedStreamTools.writeCompressed(packetData, bbos);
      } 
    catch (IOException e)
      {
      e.printStackTrace();
      }
    }
  }

@Override
protected void readFromStream(ByteBuf data)
  {
  try
    {
    packetData = CompressedStreamTools.readCompressed(new ByteBufInputStream(data));
    } 
  catch (IOException e)
    {
    e.printStackTrace();
    }
  }

@Override
protected void execute()
  {
  StructureTemplateManager.instance().onTemplateData(packetData);
  }

}