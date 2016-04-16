package net.server.handlers;

import client.MapleClient;
import net.MaplePacketHandler;
import net.SendPacketOpcode;
import tools.data.LittleEndianAccessor;
import tools.packet.LoginPacket;

public class AuthRequestHandler implements MaplePacketHandler {

	public void handlePacket(final LittleEndianAccessor lea, final MapleClient c) {
		c.getSession().write(LoginPacket.sendAuthResponse(SendPacketOpcode.AUTH_RESPONSE.getValue() ^ lea.readInt()));
	}

	public boolean validateState(MapleClient c) {
		return true;
	}

}
