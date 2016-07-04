package mca.ai;

import java.util.List;

import mca.core.Constants;
import mca.entity.EntityHuman;
import mca.enums.EnumMovementState;
import mca.enums.EnumWorkdayState;
import mca.util.Utilities;
import net.minecraft.block.BlockDoor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.village.Village;
import net.minecraft.village.VillageDoorInfo;
import radixcore.constant.Time;
import radixcore.util.RadixLogic;
import radixcore.util.RadixMath;

public class AIWorkday extends AbstractAI
{
	private EnumWorkdayState state = EnumWorkdayState.IDLE;
	private Vec3 vecTarget;
	private EntityLivingBase lookTarget;
	private int ticksActive;
	public AIWorkday(EntityHuman owner) 
	{
		super(owner);
	}

	@Override
	public void onUpdateCommon() 
	{

	}

	@Override
	public void onUpdateClient() 
	{

	}

	@Override
	public void onUpdateServer() 
	{
		//Prevent the workday while we're down for sleep or doing something with the player.
		if (owner.getMovementState() == EnumMovementState.STAY 
				|| owner.getMovementState() == EnumMovementState.FOLLOW 
				|| owner.getAI(AISleep.class).getIsSleeping()
				|| owner.getAIManager().isToggleAIActive())
		{
			//Allow looking at the player while staying.
			if (owner.getMovementState() == EnumMovementState.STAY)
			{
				handleWatchClosestPlayer();
			}

			return;
		}

		//Process according to our state.
		switch (state)
		{
		case MOVE_INDOORS: 
			handleMoveIndoors(); break;
		case WANDER: 
			handleWander(); break;
		case WATCH_CLOSEST_ANYTHING: 
			handleWatchClosestAnything(); break;
		case WATCH_CLOSEST_PLAYER: 
			handleWatchClosestPlayer(); break;
		case IDLE: 
			handleIdle(); break;
		}

		//Increment our ticks active in this state and switch if necessary.
		ticksActive++;

		if (ticksActive % Time.MINUTE == 0)
		{
			ticksActive = 0;
		}
	}

	@Override
	public void reset() 
	{

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) 
	{
		nbt.setInteger("state", state.getId());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) 
	{
		state = EnumWorkdayState.getById(nbt.getInteger("state"));
	}

	private void handleMoveIndoors()
	{

	}

	private void handleWander()
	{
		handleLook();
		
		//Every few seconds, grab a target if we don't have one.
		if (vecTarget == null && ticksActive % (Time.SECOND * (owner.getRNG().nextInt(5) + 5)) == 0)
		{
			vecTarget = RandomPositionGenerator.findRandomTarget(owner, 10, 5);
		}

		else if (vecTarget != null) //Otherwise if we do have a target, try moving to it.
		{
			owner.getNavigator().tryMoveToXYZ(vecTarget.xCoord, vecTarget.yCoord, vecTarget.zCoord, Constants.SPEED_WALK - 0.1F);

			//If we didn't find a path, or we've been pathing too long, clear and stop.
			if (owner.getNavigator().noPath() || (ticksActive % Time.SECOND * 20) == 0)
			{
				vecTarget = null;
			}
		}
	}

	private void handleWatchClosestAnything()
	{
		efficientGetLookTarget(false);
		lookAtLookTarget();
	}

	private void handleWatchClosestPlayer()
	{
		efficientGetLookTarget(true);
		lookAtLookTarget();
	}

	private void handleIdle()
	{

	}

	private void handleLook()
	{
		//Always look at the player when this function is called.
		efficientGetLookTarget(true);
		
		if (lookTarget instanceof EntityPlayer)
		{
			lookAtLookTarget();
		}
	}
	
	private void efficientGetLookTarget(boolean playerOnly)
	{
		//Efficiently check for a look target every half second.
		if (ticksActive % Time.SECOND / 2 == 0)
		{
			//Check and make sure our target is still in range.
			if (lookTarget != null && RadixMath.getDistanceToEntity(owner, lookTarget) > 3.0D)
			{
				lookTarget = null;
			}

			getLookTarget(playerOnly);
		}
	}

	private void getLookTarget(boolean playerOnly)
	{
		final Class entityClass = playerOnly ? EntityPlayer.class : EntityLivingBase.class;
		final int maxDistanceAway = 3;
		final List<Entity> entitiesAroundMe = owner.worldObj.getEntitiesWithinAABB(entityClass, AxisAlignedBB.fromBounds(owner.posX - maxDistanceAway, owner.posY - maxDistanceAway, owner.posZ - maxDistanceAway, owner.posX + maxDistanceAway, owner.posY + maxDistanceAway, owner.posZ + maxDistanceAway));

		double lastDistance = 100.0D;
		Entity target = null;

		for (Entity entity : entitiesAroundMe)
		{
			if (entity == owner) //Don't look at yourself...
			{
				continue;
			}

			double dist = RadixMath.getDistanceToEntity(owner, entity);

			if (dist < lastDistance)
			{
				lastDistance = dist;
				target = entity;
			}
		}

		lookTarget = (EntityLivingBase) target;
	}

	private void lookAtLookTarget()
	{
		if (lookTarget != null)
		{
			owner.getLookHelper().setLookPositionWithEntity(lookTarget, 9.0F, 3.0F);
		}
	}
}
