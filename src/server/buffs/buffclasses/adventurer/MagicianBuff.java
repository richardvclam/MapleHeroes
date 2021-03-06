/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.buffs.buffclasses.adventurer;

import client.MapleBuffStat;
import client.MonsterStatus;
import constants.GameConstants;
import server.MapleStatEffect;
import server.MapleStatInfo;
import server.buffs.AbstractBuffClass;

/**
 *
 * @author Itzik
 */
public class MagicianBuff extends AbstractBuffClass {

    public MagicianBuff() {
        buffs = new int[]{
            2001002, //Magic Guard
            2101001, //Meditation
            2201001, //Meditation
            2300009, //Blessed Ensemble
            2301004, //Bless    
            2301008, //Magic Booster
            2101008, //Magic Booster
            2201010, //Magic Booster
            2301002, //Heal
            2301003, //Invicible
            2111005, //Spell Booster
            2111007, //Teleport Mastery
            2111008, //Elemental Decrease
            2211005, //Spell Booster
            2211007, //Teleport Mastery
            2211008, //Elemental Decrease
            2311011, //Holy Fountain
            2311012, //Divine Protection
            2211012, //Elemental Adaptation (Ice, Lightning)
            2111011, //Elemental Adaptation (Fire, Poison)
            2311002, //Mystic Door
            2311003, //Holy Symbol
            2311004, //Shining Ray
            2311007, //Teleport Mastery
            2311009, //Holy Magic Shield
            2121000, //Maple Warrior
            2121009, //Buff Mastery
            2221000, //Maple Warrior
            2121004, //Infinity
            2221004, //Infinity
            2321004, //Infinity
            2221009, //Buff Mastery
            2321000, //Maple Warrior
            2321005, //Advanced Blessing
            2321001, //Big Bang
            2321010, //Buff Mastery
            2121053, //Epic Adventure
            2121054, //Inferno Aura
            2221053, //Epic Adventure
            2221054, //Absolute Zero Aura
            2321053, //Epic Adventure
            2321054, //Avenging Angel
        };
    }
    
    @Override
    public boolean containsJob(int job) {
        return GameConstants.isAdventurer(job) && job / 100 == 2;
    }

    @Override
    public void handleBuff(MapleStatEffect eff, int skill) {
        switch (skill) {
            case 2001002: //Magic Guard
                eff.statups.put(MapleBuffStat.MAGIC_GUARD, eff.info.get(MapleStatInfo.x));
                break;
            case 2300009: //Blessed Ensemble
            	// Blessed Ensemble & Blessed Harmony buffstats are defined in MapleCharacter.applyBlessedEnsemble()
                break;
            case 2301004: //Bless   
                eff.statups.put(MapleBuffStat.BLESS, (int)eff.getLevel());
                break;
            case 2101001: // Meditation
            case 2201001: // Meditation
            	eff.statups.put(MapleBuffStat.MATK, eff.info.get(MapleStatInfo.indieMad));
            	break;
            case 2101008: //Magic Booster
            case 2201010: //Magic Booster
            case 2301008: //Magic Booster
                eff.statups.put(MapleBuffStat.BOOSTER, eff.info.get(MapleStatInfo.x));
                break;
            case 2301002: //Heal
            	eff.monsterStatus.put(MonsterStatus.DAMAGE_PERCENT, eff.info.get(MapleStatInfo.x));
            	break;
            case 2301003: //Invicible
                eff.statups.put(MapleBuffStat.INVINCIBLE, eff.info.get(MapleStatInfo.x));
                break;
            case 2111011: //Elemental Adaptation (Fire, Poison)
            case 2211012: //Elemental Adaptation (Ice, Lightning)
            case 2311012: //Divine Protection
                eff.statups.put(MapleBuffStat.PRESSURE_VOID, 1);
                break;
            case 2111008: //Elemental Decrease
            case 2211008: //Elemental Decrease
                eff.statups.put(MapleBuffStat.ELEMENT_RESET, eff.info.get(MapleStatInfo.x));
                break;
            case 2311003: //Holy Symbol
                eff.statups.put(MapleBuffStat.HOLY_SYMBOL, eff.info.get(MapleStatInfo.x));
                break;
            case 2311004: //Shining Ray
            	eff.monsterStatus.put(MonsterStatus.STUN, 1);
            	break;
            case 2111007: //Teleport Mastery
            case 2211007: //Teleport Mastery
            case 2311007: //Teleport Mastery
                eff.info.put(MapleStatInfo.mpCon, eff.info.get(MapleStatInfo.y));
                eff.info.put(MapleStatInfo.time, 2100000000);
                eff.statups.put(MapleBuffStat.TELEPORT_MASTERY, eff.info.get(MapleStatInfo.x));
                eff.monsterStatus.put(MonsterStatus.STUN, 1);
                break;
            case 2311009: //Holy Magic Shield
                eff.statups.put(MapleBuffStat.HOLY_MAGIC_SHELL, eff.info.get(MapleStatInfo.x));
                //ret.info.put(MapleStatInfo.cooltime, ret.info.get(MapleStatInfo.y));
                //ret.hpR = ret.info.get(MapleStatInfo.z) / 100.0;
                break;
            case 2121004: //Infinity
            case 2221004: //Infinity
            case 2321004: //Infinity
            	//eff.hpR = eff.info.get(MapleStatInfo.y) / 100.0;
                //eff.mpR = eff.info.get(MapleStatInfo.y) / 100.0;
                eff.statups.put(MapleBuffStat.INFINITY, eff.info.get(MapleStatInfo.x));
                eff.statups.put(MapleBuffStat.STANCE, eff.info.get(MapleStatInfo.prop));
                break;
            case 2321005: //Advanced Blessing
                eff.statups.put(MapleBuffStat.ADVANCED_BLESSING, (int)eff.getLevel());
                eff.statups.put(MapleBuffStat.HP_BOOST, eff.info.get(MapleStatInfo.indieMhp));
                eff.statups.put(MapleBuffStat.MP_BOOST, eff.info.get(MapleStatInfo.indieMmp));
                break;
            case 2321001: //Big Bang
            	eff.info.put(MapleStatInfo.time, 45000);
            	eff.statups.put(MapleBuffStat.BIG_BANG, 1);
            	eff.setOvertime(true);
            	break;
            case 2121000: //Maple Warrior
            case 2221000: //Maple Warrior
            case 2321000: //Maple Warrior
                eff.statups.put(MapleBuffStat.MAPLE_WARRIOR, eff.info.get(MapleStatInfo.x));
                break;
            case 2121053: //Epic Adventure
            case 2221053: //Epic Adventure
            case 2321053: //Epic Adventure
                eff.statups.put(MapleBuffStat.DAMAGE_PERCENT, eff.info.get(MapleStatInfo.indieDamR));
                eff.statups.put(MapleBuffStat.DAMAGE_CAP_INCREASE, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
         //   case 2121054:
        //        eff.statups.put(MapleBuffStat.FIRE_AURA, eff.info.get(MapleStatInfo.x));
            case 2321054: //Righteously Indignant
                eff.statups.put(MapleBuffStat.ANGEL, 1);
                eff.statups.put(MapleBuffStat.IGNORE_DEF, eff.info.get(MapleStatInfo.ignoreMobpdpR));
                eff.statups.put(MapleBuffStat.ATTACK_SPEED, eff.info.get(MapleStatInfo.indieBooster));
                eff.statups.put(MapleBuffStat.INDIE_MAD, eff.info.get(MapleStatInfo.indieMad));
                eff.statups.put(MapleBuffStat.DAMAGE_CAP_INCREASE, eff.info.get(MapleStatInfo.indieMaxDamageOver));
                break;
            default:
                //System.out.println("Magician skill not coded: " + skill);
                break;
        }
    }
}
