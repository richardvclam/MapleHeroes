package net.server.login.handlers;

import java.util.List;

import client.MapleCharacter;
import client.MapleClient;
import constants.ServerConstants;
import constants.WorldConstants.TespiaWorldOption;
import constants.WorldConstants.WorldOption;
import net.AbstractMaplePacketHandler;
import net.login.LoginServer;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;

public class ViewServerListHandler extends AbstractMaplePacketHandler {

	@Override
	public void handlePacket(final LittleEndianAccessor lea, final MapleClient c) {
		if(lea.readByte() == 0) {
			c.getSession().write(LoginPacket.changeBackground(ServerConstants.backgrounds));
	    	if (ServerConstants.TESPIA) {
	            for (TespiaWorldOption tespiaservers : TespiaWorldOption.values()) {
	                if (TespiaWorldOption.getById(tespiaservers.getWorld()).show() && TespiaWorldOption.getById(tespiaservers.getWorld()) != null) {
	                    c.getSession().write(LoginPacket.getServerList(Integer.parseInt(tespiaservers.getWorld().replace("t", "")), LoginServer.getLoad()));
	                }
	            }
	        } else {
	            for (WorldOption servers : WorldOption.values()) {
	                if (WorldOption.getById(servers.getWorld()).show() && servers != null) {
	                    c.getSession().write(LoginPacket.getServerList(servers.getWorld(), LoginServer.getLoad()));
	                }
	            }
	        }
	        c.getSession().write(LoginPacket.getEndOfServerList());
	        boolean hasCharacters = false;
	        for (int world = 0; world < WorldOption.values().length; world++) {
	            final List<MapleCharacter> chars = c.loadCharacters(world);
	            if (chars != null) {
	                hasCharacters = true;
	                break;
	            }
	        }
	        if (ServerConstants.TESPIA) {
	            for (TespiaWorldOption value : TespiaWorldOption.values()) {
	                String world = value.getWorld();
	                //final List<MapleCharacter> chars = c.loadTespiaCharacters(world);
	                //if (chars != null) {
	                //    hasCharacters = true;
	                //    break;
	                //}
	            }
	        }
	        if (!hasCharacters) {
	            c.getSession().write(LoginPacket.enableRecommended(WorldOption.recommended));
	        }
	        if (WorldOption.recommended >= 0) {
	            c.getSession().write(LoginPacket.sendRecommended(WorldOption.recommended, WorldOption.recommendedmsg));
	        }
		}

	}

}