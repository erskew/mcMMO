/*
	This file is part of mcMMO.

    mcMMO is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    mcMMO is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with mcMMO.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.gmail.nossr50;

import com.gmail.nossr50.datatypes.PlayerProfile;
import com.gmail.nossr50.datatypes.SkillType;
import com.gmail.nossr50.commands.skills.*;
import com.gmail.nossr50.commands.spout.*;
import com.gmail.nossr50.commands.mc.*;
import com.gmail.nossr50.commands.party.*;
import com.gmail.nossr50.commands.general.*;
import com.gmail.nossr50.config.*;
import com.gmail.nossr50.runnables.mcTimer;
import com.gmail.nossr50.spout.SpoutStuff;
import com.gmail.nossr50.listeners.mcBlockListener;
import com.gmail.nossr50.listeners.mcEntityListener;
import com.gmail.nossr50.listeners.mcPlayerListener;
import com.gmail.nossr50.locale.mcLocale;
import com.gmail.nossr50.party.Party;
import com.gmail.nossr50.skills.*;
import com.nijikokun.bukkit.Permissions.Permissions;

import org.bukkit.Bukkit;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;
import org.getspout.spoutapi.player.FileManager;


public class mcMMO extends JavaPlugin 
{
	/*
	 * I never expected mcMMO to get so popular!
	 * Thanks for all the support for the mod
	 * Thanks to the people who have worked on the code
	 * Thanks to the donators who helped me out financially
	 * Thanks to the server admins who use my mod :)
	 * 
	 * This mod is open source, and its going to stay that way >:3
	 * 
	 * Donate via paypal to nossr50@gmail.com (A million thanks to anyone that does!)
	 */
	
	public static String maindirectory = "plugins" + File.separator + "mcMMO";
	File file = new File(maindirectory + File.separator + "config.yml");
	static File versionFile = new File(maindirectory + File.separator + "VERSION");
	public static final Logger log = Logger.getLogger("Minecraft"); 
	
	private final mcPlayerListener playerListener = new mcPlayerListener(this);
	private final mcBlockListener blockListener = new mcBlockListener(this);
	private final mcEntityListener entityListener = new mcEntityListener(this);
	
	public static mcPermissions permissionHandler = new mcPermissions();
	private Permissions permissions;

	private Runnable mcMMO_Timer = new mcTimer(this); //BLEED AND REGENERATION
	//private Timer mcMMO_SpellTimer = new Timer(true);

	//Alias - Command
	public HashMap<String, String> aliasMap = new HashMap<String, String>();
	
	public static Database database = null;
	public Misc misc = new Misc(this);

	//Config file stuff
	LoadProperties config = new LoadProperties();
	//Jar stuff
	public static File mcmmo;

	public void onEnable() 
	{
		mcmmo = this.getFile();
		new File(maindirectory).mkdir();
		
		if(!versionFile.exists()) 
		{
			updateVersion();
		} else 
		{
			String vnum = readVersion();
			//This will be changed to whatever version preceded when we actually need updater code.
			//Version 1.0.48 is the first to implement this, no checking before that version can be done.
			if(vnum.equalsIgnoreCase("1.0.48")) {
				updateFrom(1);
			}
			//Just add in more else if blocks for versions that need updater code.  Increment the updateFrom age int as we do so.
			//Catch all for versions not matching and no specific code being needed
			else if(!vnum.equalsIgnoreCase(this.getDescription().getVersion())) updateFrom(-1);
		}
		
		mcPermissions.initialize(getServer());
		
		config.configCheck();
		
		Party.getInstance().loadParties();
		new Party(this);
		
		if(!LoadProperties.useMySQL)
			Users.getInstance().loadUsers(); //Load Users file
		/*
		 * REGISTER EVENTS
		 */

		PluginManager pm = getServer().getPluginManager();
		
		if(pm.getPlugin("Spout") != null)
			LoadProperties.spoutEnabled = true;
		else
			LoadProperties.spoutEnabled = false;

		//Player Stuff
		pm.registerEvent(Event.Type.PLAYER_QUIT, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_LOGIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Lowest, this);
		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_RESPAWN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_FISH, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_COMMAND_PREPROCESS, playerListener, Priority.Lowest, this);

		//Block Stuff
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Priority.Highest, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.BLOCK_FROMTO, blockListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PLACE, blockListener, Priority.Normal, this);

		//Entity Stuff
		pm.registerEvent(Event.Type.ENTITY_DEATH, entityListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_DAMAGE, entityListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.CREATURE_SPAWN, entityListener, Priority.Normal, this);

		PluginDescriptionFile pdfFile = this.getDescription();
		mcPermissions.initialize(getServer());

		if(LoadProperties.useMySQL)
		{
			database = new Database(this);
			database.createStructure();
		} else
			Leaderboard.makeLeaderboards(); //Make the leaderboards

		for(Player player : getServer().getOnlinePlayers()){Users.addUser(player);} //In case of reload add all users back into PlayerProfile   
		System.out.println(pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
		
		Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this, mcMMO_Timer, 0, 20);
		
		registerCommands();
		
		//Spout Stuff
		if(LoadProperties.spoutEnabled)
		{
			SpoutStuff.setupSpoutConfigs();
			SpoutStuff.registerCustomEvent();
			SpoutStuff.extractFiles(); //Extract source materials
			
			FileManager FM = SpoutManager.getFileManager();
			FM.addToPreLoginCache(this, SpoutStuff.getFiles());
			
			/*
			Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(this,
					  new Runnable() {

						  @Override
						  public void run() {
							  mmoHelper.updateAll();
						  }
					  }, 20, 20);
			 */
		}
	}

	public PlayerProfile getPlayerProfile(Player player)
	{
		return Users.getProfile(player);
	}
	
	public void checkXp(Player player, SkillType skillType)
	{
		if(skillType == SkillType.ALL)
			Skills.XpCheckAll(player);
		else
			Skills.XpCheckSkill(skillType, player);
	}
	
	public boolean inSameParty(Player playera, Player playerb)
	{
		if(Users.getProfile(playera).inParty() && Users.getProfile(playerb).inParty()){
			if(Users.getProfile(playera).getParty().equals(Users.getProfile(playerb).getParty())){
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	public ArrayList<String> getParties(){
		String location = "plugins/mcMMO/mcmmo.users"; 
		ArrayList<String> parties = new ArrayList<String>();
		try {
			//Open the users file
			FileReader file = new FileReader(location);
			BufferedReader in = new BufferedReader(file);
			String line = ""; 
			while((line = in.readLine()) != null)
			{
				String[] character = line.split(":"); 
				String theparty = null;
				//Party
				if(character.length > 3)
					theparty = character[3];
				if(!parties.contains(theparty))
					parties.add(theparty);
			}
			in.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Exception while reading " 
					+ location + " (Are you sure you formatted it correctly?)", e); 
		}
		return parties;
	}
	public static String getPartyName(Player player){
		PlayerProfile PP = Users.getProfile(player);
		return PP.getParty();
	}
	public static boolean inParty(Player player){
		PlayerProfile PP = Users.getProfile(player);
		return PP.inParty();
	}
	public Permissions getPermissions() {
		return permissions;
	}
	public void onDisable() {
		Bukkit.getServer().getScheduler().cancelTasks(this);
		System.out.println("mcMMO was disabled."); 
	}
	
	private void registerCommands() {
		//Register aliases with the aliasmap (used in the playercommandpreprocessevent to ugly alias them to actual commands)
		//Skills commands
		aliasMap.put(mcLocale.getString("m.SkillAcrobatics").toLowerCase(), "acrobatics");
		aliasMap.put(mcLocale.getString("m.SkillArchery").toLowerCase(), "archery");
		aliasMap.put(mcLocale.getString("m.SkillAxes").toLowerCase(), "axes");
		aliasMap.put(mcLocale.getString("m.SkillExcavation").toLowerCase(), "excavation");
		aliasMap.put(mcLocale.getString("m.SkillFishing").toLowerCase(), "fishing");
		aliasMap.put(mcLocale.getString("m.SkillHerbalism").toLowerCase(), "herbalism");
		aliasMap.put(mcLocale.getString("m.SkillMining").toLowerCase(), "mining");
		aliasMap.put(mcLocale.getString("m.SkillRepair").toLowerCase(), "repair");
		aliasMap.put(mcLocale.getString("m.SkillSwords").toLowerCase(), "swords");
		aliasMap.put(mcLocale.getString("m.SkillTaming").toLowerCase(), "taming");
		aliasMap.put(mcLocale.getString("m.SkillUnarmed").toLowerCase(), "unarmed");
		aliasMap.put(mcLocale.getString("m.SkillWoodCutting").toLowerCase(), "woodcutting");
		
		//Mc* commands
		aliasMap.put(LoadProperties.mcability, "mcability");
		aliasMap.put(LoadProperties.mcc, "mcc");
		aliasMap.put(LoadProperties.mcgod, "mcgod");
		aliasMap.put(LoadProperties.mcmmo, "mcmmo");
		aliasMap.put(LoadProperties.mcrefresh, "mcrefresh");
		aliasMap.put(LoadProperties.mctop, "mctop");
		
		//Party commands
		aliasMap.put(LoadProperties.accept, "accept");
		//aliasMap.put(null, "a");
		aliasMap.put(LoadProperties.invite, "invite");
		aliasMap.put(LoadProperties.party, "party");
		//aliasMap.put(null, "p");
		aliasMap.put(LoadProperties.ptp, "ptp");
		
		//Other commands
		aliasMap.put(LoadProperties.addxp, "addxp");
		aliasMap.put(LoadProperties.clearmyspawn, "clearmyspawn");
		aliasMap.put(LoadProperties.mmoedit, "mmoedit");
		//aliasMap.put(key, "mmoupdate");
		aliasMap.put(LoadProperties.myspawn, "myspawn");
		aliasMap.put(LoadProperties.stats, "stats");
		aliasMap.put(LoadProperties.whois, "whois");
		aliasMap.put(LoadProperties.xprate, "xprate");
		
		//Spout commands
		//aliasMap.put(null, "mchud");
		aliasMap.put(LoadProperties.xplock, "xplock");
		
		
		//Register commands
		//Skills commands
		getCommand("acrobatics").setExecutor(new AcrobaticsCommand());
		getCommand("archery").setExecutor(new ArcheryCommand());
		getCommand("axes").setExecutor(new AxesCommand());
		getCommand("excavation").setExecutor(new ExcavationCommand());
		getCommand("fishing").setExecutor(new FishingCommand());
		getCommand("herbalism").setExecutor(new HerbalismCommand());
		getCommand("mining").setExecutor(new MiningCommand());
		getCommand("repair").setExecutor(new RepairCommand());
		getCommand("swords").setExecutor(new SwordsCommand());
		getCommand("taming").setExecutor(new TamingCommand());
		getCommand("unarmed").setExecutor(new UnarmedCommand());
		getCommand("woodcutting").setExecutor(new WoodcuttingCommand());
		
		//Mc* commands
		getCommand("mcability").setExecutor(new McabilityCommand());
		getCommand("mcc").setExecutor(new MccCommand());
		getCommand("mcgod").setExecutor(new McgodCommand());
		getCommand("mcmmo").setExecutor(new McmmoCommand());
		getCommand("mcrefresh").setExecutor(new McrefreshCommand(this));
		getCommand("mctop").setExecutor(new MctopCommand());
		
		//Party commands
		getCommand("accept").setExecutor(new AcceptCommand());
		getCommand("a").setExecutor(new ACommand());
		getCommand("invite").setExecutor(new InviteCommand(this));
		getCommand("party").setExecutor(new PartyCommand());
		getCommand("p").setExecutor(new PCommand());
		getCommand("ptp").setExecutor(new PtpCommand(this));
		
		//Other commands
		getCommand("addxp").setExecutor(new AddxpCommand(this));
		getCommand("clearmyspawn").setExecutor(new ClearmyspawnCommand());
		getCommand("mmoedit").setExecutor(new MmoeditCommand(this));
		getCommand("mmoupdate").setExecutor(new MmoupdateCommand());
		getCommand("myspawn").setExecutor(new MyspawnCommand());
		getCommand("stats").setExecutor(new StatsCommand());
		getCommand("whois").setExecutor(new WhoisCommand(this));
		getCommand("xprate").setExecutor(new XprateCommand());
		
		//Spout commands
		getCommand("mchud").setExecutor(new MchudCommand());
		getCommand("xplock").setExecutor(new XplockCommand());
		
	}

	/*
	 * It is important to always assume that you are updating from the lowest possible version.
	 * Thus, every block of updater code should be complete and self-contained; finishing all 
	 * SQL transactions and closing all file handlers, such that the next block of updater code
	 * if called will handle updating as expected.
	 */
	public void updateFrom(int age) {
		//No updater code needed, just update the version.
		if(age == -1) {
			updateVersion();
			return;
		}
		//Updater code from age 1 goes here
		if(age <= 1) {
			//Since age 1 is an example for now, we will just let it do nothing.
			
		}
		//If we are updating from age 1 but we need more to reach age 2, this will run too.
		if(age <= 2) {
			
		}
		updateVersion();
	}
	
	public void updateVersion() {
		try {
			versionFile.createNewFile();
			BufferedWriter vout = new BufferedWriter(new FileWriter(versionFile));
			vout.write(this.getDescription().getVersion());
			vout.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		} catch (SecurityException ex) {
			ex.printStackTrace();
		}
	}

	public String readVersion() {
		byte[] buffer = new byte[(int) versionFile.length()];
		BufferedInputStream f = null;
		try {
			f = new BufferedInputStream(new FileInputStream(versionFile));
			f.read(buffer);
		} catch (FileNotFoundException ex) {
			ex.printStackTrace();
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (f != null) try { f.close(); } catch (IOException ignored) { }
		}
		
		return new String(buffer);
	}
}