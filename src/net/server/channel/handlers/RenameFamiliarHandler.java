package net.server.channel.handlers;

import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.MapleClient;
import client.MonsterFamiliar;
import net.AbstractMaplePacketHandler;
import net.RecvPacketOpcode;
import tools.data.LittleEndianAccessor;
import tools.packet.CField;
import tools.packet.CWvsContext;

public class RenameFamiliarHandler extends AbstractMaplePacketHandler {

	public RenameFamiliarHandler(RecvPacketOpcode recv) {
		super(recv);
	}

	@Override
	public void handlePacket(LittleEndianAccessor lea, MapleClient c, MapleCharacter chr) {
		MonsterFamiliar mf = (MonsterFamiliar) c.getPlayer().getFamiliars().get(Integer.valueOf(lea.readInt()));
        String newName = lea.readMapleAsciiString();
        if ((mf != null) && (mf.getName().equals(mf.getOriginalName())) && (MapleCharacterUtil.isEligibleCharName(newName, false))) {
            mf.setName(newName);
            c.getSession().write(CField.renameFamiliar(mf));
        } else {
            chr.dropMessage(1, "Name was not eligible.");
        }
        c.getSession().write(CWvsContext.enableActions());
	}

}
